package com.example.aiphysical.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aiphysical.data.model.AppCourseCatalog
import com.example.aiphysical.data.model.ChatMessage
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.StudentTestAnswer
import com.example.aiphysical.data.model.StudentTestStep
import com.example.aiphysical.data.model.StudentTestSubmission
import com.example.aiphysical.data.model.StudentTestUiState
import com.example.aiphysical.data.model.computeOverallHealthPercent
import com.example.aiphysical.data.model.studentTestDefinitionFor
import com.example.aiphysical.data.service.AppAiKnowledge
import com.example.aiphysical.data.service.FirestoreResult
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.data.service.GeminiService
import com.example.aiphysical.util.createGeminiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/** Rough token estimate: 1 token ≈ 4 characters */
private const val MAX_TOKENS_PER_MESSAGE = 50_000
private const val CHARS_PER_TOKEN = 4

private enum class ChatIntent {
    Navigation,
    Courses,
    Tests,
    Help,
    Roles,
    Progress,
    General
}

class StudentViewModel(
    private val uid: String,
    private val orgId: String,
    private val firestoreService: FirestoreService,
    private val geminiService: GeminiService = createGeminiService()
) : ViewModel() {

    private val _state = MutableStateFlow(StudentUiState())
    val state: StateFlow<StudentUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<StudentEffect>()
    @Suppress("unused")
    val effects: SharedFlow<StudentEffect> = _effects.asSharedFlow()

    private var coursesObserverJob: Job? = null

    init {
        loadData()
        observeAddedCourses()
    }

    fun onEvent(event: StudentEvent) {
        when (event) {
            StudentEvent.LoadData -> loadData()
            StudentEvent.Refresh -> loadData(isRefresh = true)
            is StudentEvent.NavigateToTab -> _state.update { it.copy(selectedTab = event.tab, showAiChat = false) }
            is StudentEvent.StartTest -> handleStartTest(event.testType)
            StudentEvent.GenerateReport -> handleGenerateReport()
            StudentEvent.DismissError -> _state.update { it.copy(errorMessage = null) }
            StudentEvent.Logout -> Unit
            is StudentEvent.ChangeLanguage -> _state.update { it.copy(currentLanguage = event.language) }

            StudentEvent.OpenAddedCourses -> _state.update { it.copy(showAddedCoursesViewer = true) }
            StudentEvent.CloseAddedCourses -> _state.update { it.copy(showAddedCoursesViewer = false) }
            is StudentEvent.OpenAddedCourse -> handleOpenAddedCourse(event.course)
            StudentEvent.CloseSelectedAddedCourse -> _state.update { it.copy(selectedAddedCourse = null) }
            is StudentEvent.OpenTextCourse -> _state.update {
                it.copy(selectedAddedCourse = event.course, showTextCourseViewer = true)
            }
            StudentEvent.CloseTextCourse -> _state.update {
                it.copy(showTextCourseViewer = false, selectedAddedCourse = null)
            }

            is StudentEvent.SendChatMessage -> handleSendChatMessage(event.message)
            is StudentEvent.UpdateChatInput -> _state.update { it.copy(chatInput = event.text) }
            StudentEvent.ClearChatError -> _state.update { it.copy(chatError = null) }
            StudentEvent.ClearChatHistory -> _state.update { it.copy(chatMessages = emptyList(), chatError = null) }
            StudentEvent.OpenAiChat -> _state.update { it.copy(showAiChat = true) }
            StudentEvent.CloseAiChat -> _state.update { it.copy(showAiChat = false) }

            StudentEvent.CloseActiveTest,
            StudentEvent.CloseBurnoutTest -> _state.update { it.copy(activeTestState = null) }

            is StudentEvent.AnswerCurrentTestQuestion -> handleCurrentTestAnswer(event.answerType)
            is StudentEvent.AnswerBurnoutQuestion -> handleCurrentTestAnswer(event.answerType)

            StudentEvent.RetryCurrentTestGemini,
            StudentEvent.RetryBurnoutGemini -> retryCurrentTestGemini()

            StudentEvent.ResetCurrentTest,
            StudentEvent.ResetBurnoutTest -> resetCurrentTest()

            StudentEvent.OpenBurnoutTest -> openTest(StudentTestType.BURNOUT)
        }
    }

    private fun handleSendChatMessage(message: String) {
        if (message.isBlank()) return

        val estimatedTokens = message.length / CHARS_PER_TOKEN
        if (estimatedTokens > MAX_TOKENS_PER_MESSAGE) {
            _state.update {
                it.copy(
                    chatError = "⚠️ Сообщение слишком длинное (~$estimatedTokens токенов). Максимум — 50 000 токенов. Сократите текст."
                )
            }
            return
        }

        val userMsg = ChatMessage(role = "user", text = message)
        val newHistory = _state.value.chatMessages + userMsg
        _state.update {
            it.copy(
                chatMessages = newHistory,
                chatInput = "",
                isChatLoading = true,
                chatError = null
            )
        }

        viewModelScope.launch {
            val assistantState = refreshAssistantContext()
            val chatIntent = detectChatIntent(message)
            val systemPrompt = buildChatSystemPrompt(assistantState, chatIntent)

            geminiService.sendMessage(
                history = _state.value.chatMessages,
                systemInstruction = systemPrompt
            ).fold(
                onSuccess = { responseText ->
                    val modelMsg = ChatMessage(role = "model", text = responseText)
                    _state.update { it.copy(chatMessages = it.chatMessages + modelMsg, isChatLoading = false) }
                },
                onFailure = { error ->
                    val errMsg = ChatMessage(
                        role = "model",
                        text = "Ошибка: ${error.message ?: "Неизвестная ошибка"}",
                        isError = true
                    )
                    _state.update {
                        it.copy(
                            chatMessages = it.chatMessages + errMsg,
                            isChatLoading = false,
                            chatError = error.message
                        )
                    }
                }
            )
        }
    }

    private fun detectChatIntent(message: String): ChatIntent {
        val text = message.lowercase()

        fun score(vararg keywords: String): Int = keywords.sumOf { keyword ->
            if (text.contains(keyword)) 1 else 0
        }

        val navigationScore = score(
            "где", "куда", "как найти", "как открыть", "как зайти", "как перейти",
            "раздел", "вкладк", "экран", "меню", "профиль", "главн", "помощ", "курс"
        )
        val coursesScore = score(
            "курс", "курсы", "обучен", "урок", "каталог", "назначен", "рекоменд",
            "video", "text", "ссылка"
        )
        val testsScore = score(
            "тест", "выгора", "стресс", "тревог", "мотивац", "состояни", "балл", "результат"
        )
        val helpScore = score(
            "помощ", "психолог", "поддержк", "срочно", "кризис", "плохо", "150",
            "тревож", "страшно", "паник", "одинок", "груст", "депрес", "устал",
            "нет сил", "выгорел", "плохо морально", "мне тяжело", "я не справляюсь", "хочу поговорить"
        )
        val rolesScore = score(
            "директор", "психолог", "студент", "роль", "для директора", "для психолога"
        )
        val progressScore = score(
            "прогресс", "сколько прош", "статус", "assigned", "назначен", "мой курс"
        )

        val ranked = listOf(
            ChatIntent.Navigation to navigationScore,
            ChatIntent.Courses to coursesScore,
            ChatIntent.Tests to testsScore,
            ChatIntent.Help to helpScore,
            ChatIntent.Roles to rolesScore,
            ChatIntent.Progress to progressScore
        ).maxByOrNull { it.second }

        return if (ranked == null || ranked.second <= 0) ChatIntent.General else ranked.first
    }

    private suspend fun refreshAssistantContext(): StudentUiState {
        val baseState = _state.value

        val profile = when (val result = firestoreService.getUserProfile(uid)) {
            is FirestoreResult.UserProfileSuccess -> result.profile
            else -> baseState.profile
        }
        val history = when (val result = firestoreService.getUserTestHistory(uid)) {
            is FirestoreResult.TestHistorySuccess -> result.results
            else -> baseState.testHistory
        }
        val progress = when (val result = firestoreService.getUserCourseProgress(uid)) {
            is FirestoreResult.CourseProgressSuccess -> result.progressList
            else -> baseState.courseProgress
        }
        val addedCourses = if (orgId.isBlank()) {
            baseState.addedCourses
        } else {
            when (val result = firestoreService.getOrganizationCourses(orgId)) {
                is FirestoreResult.OrganizationCoursesSuccess -> result.courses.filter { it.isPublished }
                else -> baseState.addedCourses
            }
        }

        val completedIds = history.map { it.testId }.toSet()
        val overall = computeOverallHealthPercent(profile, completedIds)

        _state.update { current ->
            current.copy(
                profile = profile,
                testHistory = history,
                courseProgress = progress,
                addedCourses = addedCourses,
                completedTestIds = completedIds,
                overallScore = overall
            )
        }
        return _state.value
    }

    private fun buildChatSystemPrompt(state: StudentUiState, intent: ChatIntent): String {
        val baseKnowledge = AppAiKnowledge.buildBasePrompt()
        val intentKnowledge = when (intent) {
            ChatIntent.Navigation -> AppAiKnowledge.buildNavigationSlice()
            ChatIntent.Courses -> AppAiKnowledge.buildCoursesSlice()
            ChatIntent.Tests -> AppAiKnowledge.buildTestsSlice()
            ChatIntent.Help -> AppAiKnowledge.buildHelpSlice()
            ChatIntent.Roles -> AppAiKnowledge.buildRolesSlice()
            ChatIntent.Progress -> AppAiKnowledge.buildProgressSlice()
            ChatIntent.General -> AppAiKnowledge.buildGeneralSlice()
        }
        val currentAccountSummary = buildCurrentAccountSummary(state)
        val dynamicContext = buildIntentContext(state, intent)

        return """
            $baseKnowledge

            Режим ответа: ${intent.displayName()}

            Полезный срез:
            $intentKnowledge

            Текущий аккаунт:
            $currentAccountSummary

            Контекст для ответа:
            $dynamicContext
        """.trimIndent()
    }

    private fun buildIntentContext(state: StudentUiState, intent: ChatIntent): String = when (intent) {
        ChatIntent.Navigation -> listOf(
            "- студент видит: Главная, Помощь, Курсы, Профиль и AI-чат",
            "- если пользователь ищет функцию, дай короткий маршрут по вкладкам"
        ).joinToString("\n")

        ChatIntent.Courses -> listOf(
            "Доступные организационные курсы:",
            buildOrganizationCoursesSummary(state),
            "База курсов:",
            buildBaseCatalogSummary(),
            "Идеи будущих курсов:",
            buildFutureCourseIdeas(state)
        ).joinToString("\n")

        ChatIntent.Tests -> listOf(
            "Последние тесты:",
            buildTestHistorySummary(state.testHistory),
            "- completed=${state.completedTestIds.ifEmpty { setOf("нет") }.joinToString()}"
        ).joinToString("\n")

        ChatIntent.Help -> listOf(
            "- раздел Помощь содержит связь с психологом, быструю помощь и номер 150",
            "- текущий статус=${state.profile.latestAiStatus}"
        ).joinToString("\n")

        ChatIntent.Roles -> AppAiKnowledge.buildFullRoleReference()

        ChatIntent.Progress -> listOf(
            "Прогресс курсов:",
            buildCourseProgressSummary(state),
            "- назначенный курс=${state.profile.assignedCourseName.ifBlank { "нет" }}"
        ).joinToString("\n")

        ChatIntent.General -> listOf(
            "Последние тесты:",
            buildTestHistorySummary(state.testHistory),
            "Прогресс курсов:",
            buildCourseProgressSummary(state),
            "Курсы организации:",
            buildOrganizationCoursesSummary(state)
        ).joinToString("\n")
    }

    private fun ChatIntent.displayName(): String = when (this) {
        ChatIntent.Navigation -> "навигация"
        ChatIntent.Courses -> "курсы"
        ChatIntent.Tests -> "тесты"
        ChatIntent.Help -> "помощь"
        ChatIntent.Roles -> "роли"
        ChatIntent.Progress -> "прогресс"
        ChatIntent.General -> "общий"
    }

    private fun buildCurrentAccountSummary(state: StudentUiState): String = buildString {
        appendLine("- email=${state.profile.email.ifBlank { "не указан" }}, name=${state.profile.fullName.ifBlank { "не указано" }}, role=student")
        appendLine("- uid=${state.profile.uid.ifBlank { uid }}, orgId=${state.profile.orgId.ifBlank { orgId.ifBlank { "не указан" } }}")
        appendLine("- age=${state.profile.ageGroup.ifBlank { "не указана" }}, overall=${state.overallScore.toInt()}/100, status=${state.profile.latestAiStatus}")
        appendLine("- assignedCourse=${state.profile.assignedCourseName.ifBlank { "нет" }}, psychComment=${state.profile.psychComment.take(120).ifBlank { "нет" }}")
        append("- completedTests=${state.completedTestIds.ifEmpty { setOf("нет") }.joinToString()}")
    }

    private fun buildTestHistorySummary(results: List<com.example.aiphysical.data.model.TestResult>): String {
        if (results.isEmpty()) return "- пока нет сохранённых результатов"
        return results
            .sortedByDescending { it.dateMillis }
            .take(4)
            .joinToString("\n") { result ->
                val shortFeedback = result.feedbackText.replace("\n", " ").take(80).ifBlank { "без комментария" }
                "- ${result.testId}: ${result.score.toInt()}%, ${result.aiAssessment}, ${shortFeedback}"
            }
    }

    private fun buildCourseProgressSummary(state: StudentUiState): String {
        val progressList = state.courseProgress
        if (progressList.isEmpty()) return "- прогресс пока не сохранён"
        return progressList.joinToString("\n") { item ->
            val percent = normalizeCourseProgress(item.progress)
            "- ${item.courseName.ifBlank { item.courseId }}: $percent%"
        }
    }

    private fun buildOrganizationCoursesSummary(state: StudentUiState): String {
        if (state.addedCourses.isEmpty()) return "- опубликованных организационных курсов пока нет"
        return state.addedCourses.take(8).joinToString("\n") { course ->
            val linkPart = if (course.type == CourseContentType.VIDEO && course.videoUrl.isNotBlank()) {
                ", ссылка=${course.videoUrl}"
            } else {
                ""
            }
            "- ${course.title} [${course.type.name}]: ${course.description.ifBlank { "без описания" }}$linkPart"
        }
    }

    private fun buildBaseCatalogSummary(): String = AppCourseCatalog.baseCourses.joinToString("\n") { course ->
        "- ${course.id}: ${course.title} (${course.durationLabel})"
    }

    private fun buildFutureCourseIdeas(state: StudentUiState): String {
        val ideas = linkedSetOf<String>()

        if (state.profile.latestAiStatus == "critical") {
            ideas += "антикризисная самопомощь и быстрые техники стабилизации"
        }
        if ((state.profile.burnoutScore) >= 60f) {
            ideas += "профилактика выгорания и восстановление ресурса"
        }
        if ((state.profile.stressScore) >= 60f || (state.profile.anxietyScore) >= 60f) {
            ideas += "снижение стресса, тревоги и техники заземления"
        }
        if ((state.profile.motivationScore) in 0f..45f) {
            ideas += "возврат мотивации и маленькие устойчивые привычки"
        }
        if ((state.profile.emotionScore) in 0f..45f) {
            ideas += "эмоциональная устойчивость и саморегуляция"
        }
        if (state.addedCourses.none { it.title.contains("сон", ignoreCase = true) }) {
            ideas += "сон и восстановление"
        }
        if (state.addedCourses.none { it.title.contains("коммуника", ignoreCase = true) }) {
            ideas += "коммуникация и личные границы"
        }

        if (ideas.isEmpty()) {
            ideas += "углублённые курсы по стресс-менеджменту, эмоциональной устойчивости и учебному балансу"
        }

        return ideas.take(5).joinToString("\n") { "- $it" }
    }

    private fun normalizeCourseProgress(progress: Float): Int = when {
        progress <= 0f -> 0
        progress <= 1f -> (progress * 100).toInt()
        else -> progress.toInt()
    }

    private fun loadData(isRefresh: Boolean = false) {
        if (isRefresh) _state.update { it.copy(isRefreshing = true) }
        else _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val profileResult = firestoreService.getUserProfile(uid)
            val historyResult = firestoreService.getUserTestHistory(uid)
            val courseResult = firestoreService.getUserCourseProgress(uid)

            val profile = when (profileResult) {
                is FirestoreResult.UserProfileSuccess -> profileResult.profile
                else -> {
                    emit(StudentEffect.ShowSnackbar("Ошибка загрузки профиля"))
                    _state.update { it.copy(isLoading = false, isRefreshing = false) }
                    return@launch
                }
            }

            val history = when (historyResult) {
                is FirestoreResult.TestHistorySuccess -> historyResult.results
                else -> emptyList()
            }

            val courses = when (courseResult) {
                is FirestoreResult.CourseProgressSuccess -> courseResult.progressList
                else -> emptyList()
            }

            val completedIds = history.map { it.testId }.toSet()
            val overall = computeOverallHealthPercent(profile, completedIds)

            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    profile = profile,
                    testHistory = history,
                    courseProgress = courses,
                    completedTestIds = completedIds,
                    overallScore = overall,
                    errorMessage = null
                )
            }
        }
    }

    private fun observeAddedCourses() {
        if (orgId.isBlank()) return
        coursesObserverJob?.cancel()
        coursesObserverJob = viewModelScope.launch {
            firestoreService.observeOrganizationCourses(orgId)
                .catch { }
                .collect { result ->
                    when (result) {
                        is FirestoreResult.OrganizationCoursesSuccess ->
                            _state.update { it.copy(addedCourses = result.courses.filter { c -> c.isPublished }) }
                        else -> Unit
                    }
                }
        }
    }

    private fun handleOpenAddedCourse(course: com.example.aiphysical.data.model.OrganizationCourse) {
        when (course.type) {
            CourseContentType.VIDEO -> {
                if (course.videoUrl.isNotBlank()) emit(StudentEffect.OpenUrl(course.videoUrl))
                else emit(StudentEffect.ShowSnackbar("Ссылка на видео недоступна"))
            }
            CourseContentType.TEXT -> _state.update { it.copy(selectedAddedCourse = course, showTextCourseViewer = true) }
        }
    }

    private fun handleStartTest(testType: StudentTestType) {
        openTest(testType)
        emit(StudentEffect.NavigateToTest(testType))
    }

    private fun handleGenerateReport() {
        emit(StudentEffect.ShowSnackbar("📊 Генерация отчёта — функция в разработке"))
    }

    private fun openTest(testType: StudentTestType) {
        val definition = studentTestDefinitionFor(testType)
        _state.update { it.copy(activeTestState = StudentTestUiState(definition = definition)) }
    }

    private fun resetCurrentTest() {
        val currentDefinition = _state.value.activeTestState?.definition ?: return
        _state.update { it.copy(activeTestState = StudentTestUiState(definition = currentDefinition)) }
    }

    private fun handleCurrentTestAnswer(answerType: com.example.aiphysical.data.model.AnswerType) {
        val testState = _state.value.activeTestState ?: return
        if (testState.isAnswering) return

        val question = testState.definition.questions.getOrNull(testState.currentQuestionIndex) ?: return
        val newAnswer = StudentTestAnswer(
            questionId = question.id,
            questionText = question.text,
            catEmotion = question.catEmotion,
            answerType = answerType,
            polarity = question.polarity
        )
        val newAnswers = testState.answers + newAnswer
        val nextIndex = testState.currentQuestionIndex + 1

        if (nextIndex >= testState.definition.questions.size) {
            _state.update {
                it.copy(
                    activeTestState = testState.copy(
                        answers = newAnswers,
                        currentQuestionIndex = nextIndex,
                        isAnswering = true,
                        step = StudentTestStep.LoadingResult,
                        errorMessage = null
                    )
                )
            }
            launchTestAnalysis(testState.definition, newAnswers)
        } else {
            _state.update {
                it.copy(
                    activeTestState = testState.copy(
                        answers = newAnswers,
                        currentQuestionIndex = nextIndex,
                        isAnswering = true,
                        errorMessage = null
                    )
                )
            }
            viewModelScope.launch {
                delay(350)
                _state.update { state ->
                    state.copy(activeTestState = state.activeTestState?.copy(isAnswering = false))
                }
            }
        }
    }

    private fun retryCurrentTestGemini() {
        val testState = _state.value.activeTestState ?: return
        if (testState.answers.isEmpty()) return
        _state.update {
            it.copy(
                activeTestState = testState.copy(
                    step = StudentTestStep.LoadingResult,
                    isAnswering = true,
                    errorMessage = null
                )
            )
        }
        launchTestAnalysis(testState.definition, testState.answers)
    }

    private fun launchTestAnalysis(
        definition: com.example.aiphysical.data.model.StudentTestDefinition,
        answers: List<StudentTestAnswer>
    ) {
        val score = definition.scoreAnswers(answers)
        val assessment = definition.computeAssessment(score)
        val prompt = definition.buildPrompt(answers, score, assessment)

        viewModelScope.launch {
            geminiService.sendMessage(listOf(ChatMessage(role = "user", text = prompt))).fold(
                onSuccess = { feedbackText ->
                    persistAndPresentResult(
                        definition = definition,
                        answers = answers,
                        score = score,
                        assessment = assessment,
                        feedbackText = feedbackText,
                        initialErrorMessage = null
                    )
                },
                onFailure = {
                    val fallback = definition.buildFallback(score, assessment)
                    persistAndPresentResult(
                        definition = definition,
                        answers = answers,
                        score = score,
                        assessment = assessment,
                        feedbackText = fallback,
                        initialErrorMessage = "⚠️ AI недоступен, показан локальный результат"
                    )
                }
            )
        }
    }

    private suspend fun persistAndPresentResult(
        definition: com.example.aiphysical.data.model.StudentTestDefinition,
        answers: List<StudentTestAnswer>,
        score: Int,
        assessment: String,
        feedbackText: String,
        initialErrorMessage: String?
    ) {
        val saveResult = firestoreService.saveStudentTestResult(
            uid = uid,
            submission = StudentTestSubmission(
                definition = definition,
                score = score,
                aiAssessment = assessment,
                feedbackText = feedbackText,
                answers = answers
            )
        )

        val saveErrorMessage = (saveResult as? FirestoreResult.Failure)?.message
        if (saveErrorMessage == null) {
            loadData()
        } else {
            emit(StudentEffect.ShowSnackbar("Не удалось сохранить результат теста"))
        }

        val combinedError = listOfNotNull(
            initialErrorMessage,
            saveErrorMessage?.let { "⚠️ Не удалось сохранить результат: $it" }
        ).takeIf { it.isNotEmpty() }?.joinToString("\n")

        _state.update { state ->
            state.copy(
                activeTestState = state.activeTestState?.copy(
                    step = StudentTestStep.Result(feedbackText, score, assessment),
                    isAnswering = false,
                    errorMessage = combinedError
                )
            )
        }
    }

    private fun emit(effect: StudentEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    companion object {
        fun factory(
            uid: String,
            orgId: String,
            firestoreService: FirestoreService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                StudentViewModel(uid, orgId, firestoreService) as T
        }
    }
}
