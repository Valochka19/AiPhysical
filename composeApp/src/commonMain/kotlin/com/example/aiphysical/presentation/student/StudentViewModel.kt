package com.example.aiphysical.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aiphysical.data.model.AppCourseCatalog
import com.example.aiphysical.data.model.BaseCourseCatalogItem
import com.example.aiphysical.data.model.ChatMessage
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.OrganizationCustomTest
import com.example.aiphysical.data.model.OrganizationCustomTestAnswer
import com.example.aiphysical.data.model.OrganizationCustomTestSessionState
import com.example.aiphysical.data.model.OrganizationCustomTestSubmission
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
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.util.currentTimeMillis
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
    private val geminiService: GeminiService = createGeminiService(),
    initialLanguage: AppLanguage = AppLanguage.RU,
) : ViewModel() {

    private val _state = MutableStateFlow(StudentUiState(currentLanguage = initialLanguage))
    val state: StateFlow<StudentUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<StudentEffect>()
    @Suppress("unused")
    val effects: SharedFlow<StudentEffect> = _effects.asSharedFlow()

    private var coursesObserverJob: Job? = null
    private var customTestsObserverJob: Job? = null

    init {
        loadData()
        observeAddedCourses()
        observeCustomTests()
    }

    fun onEvent(event: StudentEvent) {
        when (event) {
            StudentEvent.LoadData -> loadData()
            StudentEvent.Refresh -> loadData(isRefresh = true)
            is StudentEvent.NavigateToTab -> _state.update { it.copy(selectedTab = event.tab, showAiChat = false) }
            is StudentEvent.ChangeContentSubTab -> _state.update { it.copy(selectedContentSubTab = event.tab) }
            is StudentEvent.StartTest -> handleStartTest(event.testType)
            StudentEvent.GenerateReport -> handleGenerateReport()
            StudentEvent.DismissError -> _state.update { it.copy(errorMessage = null) }
            StudentEvent.Logout -> Unit
            is StudentEvent.ChangeLanguage -> _state.update { it.copy(currentLanguage = event.language) }

            StudentEvent.OpenAddedCourses -> _state.update { it.copy(showAddedCoursesViewer = true) }
            StudentEvent.CloseAddedCourses -> _state.update { it.copy(showAddedCoursesViewer = false) }
            is StudentEvent.OpenBaseCourse -> handleOpenBaseCourse(event.course)
            is StudentEvent.OpenAddedCourse -> handleOpenAddedCourse(event.course)
            StudentEvent.CloseSelectedAddedCourse -> _state.update { it.copy(selectedAddedCourse = null) }
            is StudentEvent.OpenTextCourse -> _state.update {
                it.copy(selectedAddedCourse = event.course, showTextCourseViewer = true)
            }
            StudentEvent.CloseTextCourse -> _state.update {
                it.copy(showTextCourseViewer = false, selectedAddedCourse = null)
            }
            is StudentEvent.OpenOrganizationCustomTest -> handleOpenOrganizationCustomTest(event.test)
            is StudentEvent.AnswerOrganizationCustomTestQuestion -> handleAnswerOrganizationCustomTestQuestion(event.optionId)
            StudentEvent.NextOrganizationCustomTestQuestion -> handleNextOrganizationCustomTestQuestion()
            StudentEvent.SubmitOrganizationCustomTest -> handleSubmitOrganizationCustomTest()
            StudentEvent.CloseOrganizationCustomTest -> _state.update { it.copy(activeCustomTestState = null) }

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

        val language = _state.value.currentLanguage

        val estimatedTokens = message.length / CHARS_PER_TOKEN
        if (estimatedTokens > MAX_TOKENS_PER_MESSAGE) {
            _state.update {
                it.copy(
                    chatError = language.pick(
                        ru = "⚠️ Сообщение слишком длинное (~$estimatedTokens токенов). Максимум — 50 000 токенов. Сократите текст.",
                        en = "⚠️ Your message is too long (~$estimatedTokens tokens). The limit is 50,000 tokens. Please shorten it.",
                        kz = "⚠️ Хабарлама тым ұзын (~$estimatedTokens токен). Шегі — 50 000 токен. Қысқартыңыз."
                    )
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
                systemInstruction = systemPrompt,
                language = assistantState.currentLanguage
            ).fold(
                onSuccess = { responseText ->
                    val modelMsg = ChatMessage(role = "model", text = normalizeAssistantText(responseText))
                    _state.update { it.copy(chatMessages = it.chatMessages + modelMsg, isChatLoading = false) }
                },
                onFailure = { error ->
                    val errMsg = ChatMessage(
                        role = "model",
                        text = assistantState.currentLanguage.pick(
                            ru = "Ошибка: ${error.message ?: "Неизвестная ошибка"}",
                            en = "Error: ${error.message ?: "Unknown error"}",
                            kz = "Қате: ${error.message ?: "Белгісіз қате"}"
                        ),
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
            "раздел", "вкладк", "экран", "меню", "профиль", "главн", "помощ", "курс",
            "where", "how to find", "how can i find", "how to open", "how do i open", "how to go", "tab", "section", "screen", "menu", "home", "help", "profile",
            "қайда", "қалай таб", "қалай аш", "қалай кір", "қалай өт", "бөлім", "қойынды", "экран", "мәзір", "басты", "көмек", "профиль", "курс"
        )
        val coursesScore = score(
            "курс", "курсы", "обучен", "урок", "каталог", "назначен", "рекоменд",
            "video", "text", "ссылка",
            "course", "courses", "study", "lesson", "catalog", "assigned", "recommend", "link",
            "курс", "курстар", "оқу", "сабақ", "каталог", "тағайындал", "ұсын", "мәтін", "сілтеме"
        )
        val testsScore = score(
            "тест", "выгора", "стресс", "тревог", "мотивац", "состояни", "балл", "результат",
            "test", "burnout", "stress", "anxiety", "motivation", "condition", "score", "result",
            "тест", "күйіп кет", "стресс", "мазасыз", "мотивац", "жағдай", "балл", "нәтиже"
        )
        val helpScore = score(
            "помощ", "психолог", "поддержк", "срочно", "кризис", "плохо", "150",
            "тревож", "страшно", "паник", "одинок", "груст", "депрес", "устал",
            "нет сил", "выгорел", "плохо морально", "мне тяжело", "я не справляюсь", "хочу поговорить",
            "help", "psychologist", "support", "urgent", "crisis", "bad", "anxious", "scared", "panic", "lonely", "sad", "depress", "tired", "no energy", "burned out", "need help", "need to talk",
            "көмек", "психолог", "қолдау", "шұғыл", "дағдарыс", "жаман", "мазасыз", "қорқ", "үрей", "жалғыз", "мұң", "депресс", "шарша", "күш жоқ", "күйіп кетт", "маған қиын", "көмектес", "сөйлескім келеді"
        )
        val rolesScore = score(
            "директор", "психолог", "студент", "роль", "для директора", "для психолога",
            "director", "psychologist", "student", "teacher", "role", "for director", "for psychologist",
            "директор", "психолог", "студент", "мұғалім", "рөл", "директор үшін", "психолог үшін"
        )
        val progressScore = score(
            "прогресс", "сколько прош", "статус", "assigned", "назначен", "мой курс",
            "progress", "completed", "how much", "my course", "points",
            "прогресс", "қанша өт", "статус", "менің курсым", "ұпай"
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
        val pointsHistory = when (val result = firestoreService.getUserPointsLedger(uid)) {
            is FirestoreResult.PointsLedgerSuccess -> result.entries
            else -> baseState.pointsHistory
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
                pointsHistory = pointsHistory,
                addedCourses = addedCourses,
                completedTestIds = completedIds,
                overallScore = overall
            )
        }
        return _state.value
    }

    private fun buildChatSystemPrompt(state: StudentUiState, intent: ChatIntent): String {
        val language = state.currentLanguage
        val baseKnowledge = AppAiKnowledge.buildBasePrompt(language)
        val intentKnowledge = when (intent) {
            ChatIntent.Navigation -> AppAiKnowledge.buildNavigationSlice(language)
            ChatIntent.Courses -> AppAiKnowledge.buildCoursesSlice(language)
            ChatIntent.Tests -> AppAiKnowledge.buildTestsSlice(language)
            ChatIntent.Help -> AppAiKnowledge.buildHelpSlice(language)
            ChatIntent.Roles -> AppAiKnowledge.buildRolesSlice(language)
            ChatIntent.Progress -> AppAiKnowledge.buildProgressSlice(language)
            ChatIntent.General -> AppAiKnowledge.buildGeneralSlice(language)
        }
        val currentAccountSummary = buildCurrentAccountSummary(state, language)
        val dynamicContext = buildIntentContext(state, intent, language)

        return """
            $baseKnowledge

            ${language.pick("Режим ответа", "Response mode", "Жауап режимі")}: ${intent.displayName(language)}

            ${language.pick("Полезный срез", "Relevant context", "Пайдалы контекст")}:
            $intentKnowledge

            ${language.pick("Текущий аккаунт", "Current account", "Ағымдағы аккаунт")}:
            $currentAccountSummary

            ${language.pick("Контекст для ответа", "Answer context", "Жауап контексті")}:
            $dynamicContext
        """.trimIndent()
    }

    private fun buildIntentContext(state: StudentUiState, intent: ChatIntent, language: AppLanguage): String = when (intent) {
        ChatIntent.Navigation -> listOf(
            language.pick(
                ru = "- студент видит: Главная, Помощь, Курсы, Профиль и AI-чат",
                en = "- student sees: Home, Help, Courses, Profile, and AI chat",
                kz = "- студент мына бөлімдерді көреді: Басты бет, Көмек, Курстар, Профиль және AI чат"
            ),
            language.pick(
                ru = "- если пользователь ищет функцию, дай короткий маршрут по вкладкам",
                en = "- if the user is looking for a feature, give a short path through tabs",
                kz = "- егер пайдаланушы функция іздесе, бөлімдер арқылы қысқа маршрут бер"
            )
        ).joinToString("\n")

        ChatIntent.Courses -> listOf(
            language.pick("Доступные организационные курсы:", "Available organization courses:", "Қолжетімді ұйым курстары:"),
            buildOrganizationCoursesSummary(state, language),
            language.pick("База курсов:", "Course catalog:", "Курс каталогы:"),
            buildBaseCatalogSummary(language),
            language.pick("Идеи будущих курсов:", "Future course ideas:", "Болашақ курс идеялары:"),
            buildFutureCourseIdeas(state, language)
        ).joinToString("\n")

        ChatIntent.Tests -> listOf(
            language.pick("Последние тесты:", "Recent tests:", "Соңғы тесттер:"),
            buildTestHistorySummary(state.testHistory, language),
            "- completed=${state.completedTestIds.ifEmpty { setOf(language.pick("нет", "none", "жоқ")) }.joinToString()}"
        ).joinToString("\n")

        ChatIntent.Help -> listOf(
            language.pick(
                ru = "- раздел Помощь содержит связь с психологом, быструю помощь и номер 150",
                en = "- the Help section contains psychologist contact, quick support, and hotline 150",
                kz = "- Көмек бөлімінде психологпен байланыс, жедел қолдау және 150 нөмірі бар"
            ),
            language.pick(
                ru = "- текущий статус=${state.profile.latestAiStatus}",
                en = "- current status=${state.profile.latestAiStatus}",
                kz = "- ағымдағы статус=${state.profile.latestAiStatus}"
            )
        ).joinToString("\n")

        ChatIntent.Roles -> AppAiKnowledge.buildFullRoleReference(language)

        ChatIntent.Progress -> listOf(
            language.pick("Прогресс курсов:", "Course progress:", "Курс прогресі:"),
            buildCourseProgressSummary(state, language),
            "- assignedCourse=${state.profile.assignedCourseName.ifBlank { language.pick("нет", "none", "жоқ") }}"
        ).joinToString("\n")

        ChatIntent.General -> listOf(
            language.pick("Последние тесты:", "Recent tests:", "Соңғы тесттер:"),
            buildTestHistorySummary(state.testHistory, language),
            language.pick("Прогресс курсов:", "Course progress:", "Курс прогресі:"),
            buildCourseProgressSummary(state, language),
            language.pick("Курсы организации:", "Organization courses:", "Ұйым курстары:"),
            buildOrganizationCoursesSummary(state, language)
        ).joinToString("\n")
    }

    private fun ChatIntent.displayName(language: AppLanguage): String = when (this) {
        ChatIntent.Navigation -> language.pick("навигация", "navigation", "навигация")
        ChatIntent.Courses -> language.pick("курсы", "courses", "курстар")
        ChatIntent.Tests -> language.pick("тесты", "tests", "тесттер")
        ChatIntent.Help -> language.pick("помощь", "help", "көмек")
        ChatIntent.Roles -> language.pick("роли", "roles", "рөлдер")
        ChatIntent.Progress -> language.pick("прогресс", "progress", "прогресс")
        ChatIntent.General -> language.pick("общий", "general", "жалпы")
    }

    private fun buildCurrentAccountSummary(state: StudentUiState, language: AppLanguage): String = buildString {
        appendLine("- email=${state.profile.email.ifBlank { language.pick("не указан", "not specified", "көрсетілмеген") }}, name=${state.profile.fullName.ifBlank { language.pick("не указано", "not specified", "көрсетілмеген") }}, role=student")
        appendLine("- uid=${state.profile.uid.ifBlank { uid }}, orgId=${state.profile.orgId.ifBlank { orgId.ifBlank { language.pick("не указан", "not specified", "көрсетілмеген") } }}")
        appendLine("- age=${state.profile.ageGroup.ifBlank { language.pick("не указана", "not specified", "көрсетілмеген") }}, overall=${state.overallScore.toInt()}/100, status=${state.profile.latestAiStatus}")
        appendLine("- assignedCourse=${state.profile.assignedCourseName.ifBlank { language.pick("нет", "none", "жоқ") }}, psychComment=${state.profile.psychComment.take(120).ifBlank { language.pick("нет", "none", "жоқ") }}")
        append("- completedTests=${state.completedTestIds.ifEmpty { setOf(language.pick("нет", "none", "жоқ")) }.joinToString()}")
    }

    private fun buildTestHistorySummary(results: List<com.example.aiphysical.data.model.TestResult>, language: AppLanguage): String {
        if (results.isEmpty()) return language.pick("- пока нет сохранённых результатов", "- no saved results yet", "- әзірге сақталған нәтиже жоқ")
        return results
            .sortedByDescending { it.dateMillis }
            .take(4)
            .joinToString("\n") { result ->
                val shortFeedback = result.feedbackText.replace("\n", " ").take(80).ifBlank { language.pick("без комментария", "no comment", "пікір жоқ") }
                "- ${result.testId}: ${result.score.toInt()}%, ${result.aiAssessment}, ${shortFeedback}"
            }
    }

    private fun buildCourseProgressSummary(state: StudentUiState, language: AppLanguage): String {
        val progressList = state.courseProgress
        if (progressList.isEmpty()) return language.pick("- прогресс пока не сохранён", "- no progress saved yet", "- прогресс әлі сақталмаған")
        return progressList.joinToString("\n") { item ->
            val percent = normalizeCourseProgress(item.progress)
            "- ${item.courseName.ifBlank { item.courseId }}: $percent%"
        }
    }

    private fun buildOrganizationCoursesSummary(state: StudentUiState, language: AppLanguage): String {
        if (state.addedCourses.isEmpty()) return language.pick("- опубликованных организационных курсов пока нет", "- no published organization courses yet", "- жарияланған ұйым курстары әлі жоқ")
        return state.addedCourses.take(8).joinToString("\n") { course ->
            val linkPart = if (course.type == CourseContentType.VIDEO && course.videoUrl.isNotBlank()) {
                language.pick(", ссылка=${course.videoUrl}", ", link=${course.videoUrl}", ", сілтеме=${course.videoUrl}")
            } else {
                ""
            }
            "- ${course.title} [${course.type.name}]: ${course.description.ifBlank { language.pick("без описания", "no description", "сипаттама жоқ") }}$linkPart"
        }
    }

    private fun buildBaseCatalogSummary(language: AppLanguage): String = AppCourseCatalog.baseCourses.joinToString("\n") { course ->
        "- ${course.id}: ${course.title} (${course.durationLabel})"
    }

    private fun buildFutureCourseIdeas(state: StudentUiState, language: AppLanguage): String {
        val ideas = linkedSetOf<String>()

        if (state.profile.latestAiStatus == "critical") {
            ideas += language.pick("антикризисная самопомощь и быстрые техники стабилизации", "anti-crisis self-help and quick stabilization tools", "дағдарысқа қарсы өзіндік көмек және тез тұрақтану тәсілдері")
        }
        if ((state.profile.burnoutScore) >= 60f) {
            ideas += language.pick("профилактика выгорания и восстановление ресурса", "burnout prevention and energy recovery", "күйіп кетудің алдын алу және ресурсты қалпына келтіру")
        }
        if ((state.profile.stressScore) >= 60f || (state.profile.anxietyScore) >= 60f) {
            ideas += language.pick("снижение стресса, тревоги и техники заземления", "stress and anxiety reduction with grounding tools", "стресс пен мазасыздықты азайту және grounding тәсілдері")
        }
        if ((state.profile.motivationScore) in 0f..45f) {
            ideas += language.pick("возврат мотивации и маленькие устойчивые привычки", "bringing motivation back with small sustainable habits", "мотивацияны қайтару және шағын тұрақты әдеттер")
        }
        if ((state.profile.emotionScore) in 0f..45f) {
            ideas += language.pick("эмоциональная устойчивость и саморегуляция", "emotional resilience and self-regulation", "эмоциялық тұрақтылық пен өзін-өзі реттеу")
        }
        if (state.addedCourses.none { it.title.contains("сон", ignoreCase = true) }) {
            ideas += language.pick("сон и восстановление", "sleep and recovery", "ұйқы және қалпына келу")
        }
        if (state.addedCourses.none { it.title.contains("коммуника", ignoreCase = true) }) {
            ideas += language.pick("коммуникация и личные границы", "communication and personal boundaries", "коммуникация және жеке шекаралар")
        }

        if (ideas.isEmpty()) {
            ideas += language.pick("углублённые курсы по стресс-менеджменту, эмоциональной устойчивости и учебному балансу", "deeper courses on stress management, emotional resilience, and study balance", "стресс-менеджмент, эмоциялық тұрақтылық және оқу балансын тереңірек ашатын курстар")
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
            val pointsResult = firestoreService.getUserPointsLedger(uid)

            val profile = when (profileResult) {
                is FirestoreResult.UserProfileSuccess -> profileResult.profile
                else -> {
                    emit(StudentEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Ошибка загрузки профиля",
                        en = "Failed to load the profile",
                        kz = "Профильді жүктеу қатесі"
                    )))
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

            val pointsHistory = when (pointsResult) {
                is FirestoreResult.PointsLedgerSuccess -> pointsResult.entries
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
                    pointsHistory = pointsHistory,
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

    private fun observeCustomTests() {
        if (orgId.isBlank()) return
        customTestsObserverJob?.cancel()
        _state.update { it.copy(isLoadingCustomTests = true) }
        customTestsObserverJob = viewModelScope.launch {
            firestoreService.observeOrganizationCustomTests(orgId)
                .catch { _state.update { state -> state.copy(isLoadingCustomTests = false) } }
                .collect { result ->
                    when (result) {
                        is FirestoreResult.OrganizationCustomTestsSuccess -> _state.update {
                            it.copy(
                                customTests = result.tests.filter { test -> test.isPublished },
                                isLoadingCustomTests = false
                            )
                        }

                        is FirestoreResult.Failure -> _state.update { it.copy(isLoadingCustomTests = false) }
                        else -> Unit
                    }
                }
        }
    }

    private fun handleOpenAddedCourse(course: com.example.aiphysical.data.model.OrganizationCourse) {
        when (course.type) {
            CourseContentType.VIDEO -> {
                if (course.videoUrl.isNotBlank()) emit(StudentEffect.OpenUrl(course.videoUrl))
                else emit(StudentEffect.ShowSnackbar(currentLanguage().pick(
                    ru = "Ссылка на видео недоступна",
                    en = "The video link is unavailable",
                    kz = "Бейне сілтемесі қолжетімсіз"
                )))
            }
            CourseContentType.TEXT -> _state.update { it.copy(selectedAddedCourse = course, showTextCourseViewer = true) }
        }
    }

    private fun handleOpenBaseCourse(course: BaseCourseCatalogItem) {
        if (course.courseUrl.isBlank()) {
            emit(StudentEffect.ShowSnackbar(currentLanguage().pick(
                ru = "Ссылка на курс недоступна",
                en = "The course link is unavailable",
                kz = "Курс сілтемесі қолжетімсіз"
            )))
            return
        }

        emit(StudentEffect.OpenUrl(course.courseUrl))
        viewModelScope.launch {
            when (val result = firestoreService.upsertBaseCourseProgress(uid, course)) {
                is FirestoreResult.Failure -> emit(StudentEffect.ShowSnackbar(currentLanguage().pick(
                    ru = "Не удалось обновить прогресс курса",
                    en = "Failed to update course progress",
                    kz = "Курс прогресін жаңарту мүмкін болмады"
                )))
                else -> loadData()
            }
        }
    }

    private fun handleOpenOrganizationCustomTest(test: OrganizationCustomTest) {
        if (test.questions.isEmpty()) {
            emit(StudentEffect.ShowSnackbar(currentLanguage().pick(
                ru = "В этом тесте пока нет вопросов",
                en = "This test has no questions yet",
                kz = "Бұл тестте әзірге сұрақтар жоқ"
            )))
            return
        }
        _state.update {
            it.copy(
                selectedContentSubTab = StudentContentSubTab.Tests,
                activeCustomTestState = OrganizationCustomTestSessionState(test = test)
            )
        }
    }

    private fun handleAnswerOrganizationCustomTestQuestion(optionId: String) {
        _state.update { state ->
            state.copy(activeCustomTestState = state.activeCustomTestState?.copy(selectedOptionId = optionId, errorMessage = null))
        }
    }

    private fun handleNextOrganizationCustomTestQuestion() {
        val session = _state.value.activeCustomTestState ?: return
        val question = session.currentQuestion ?: return
        val selectedOption = question.options.firstOrNull { it.id == session.selectedOptionId }
        if (selectedOption == null) {
            _state.update { state ->
                state.copy(activeCustomTestState = state.activeCustomTestState?.copy(errorMessage = currentLanguage().pick(
                    ru = "Выберите один вариант ответа",
                    en = "Select one answer option",
                    kz = "Бір жауап нұсқасын таңдаңыз"
                )))
            }
            return
        }
        if (session.isLastQuestion) {
            handleSubmitOrganizationCustomTest()
            return
        }

        val answer = OrganizationCustomTestAnswer(
            questionId = question.id,
            questionText = question.text,
            selectedOptionId = selectedOption.id,
            selectedOptionText = selectedOption.text,
            order = question.order
        )
        val updatedAnswers = session.answers.upsertCustomTestAnswer(answer)
        val nextQuestion = session.test.questions.getOrNull(session.currentQuestionIndex + 1)
        val preselectedOptionId = updatedAnswers.firstOrNull { it.order == nextQuestion?.order }?.selectedOptionId
        _state.update { state ->
            state.copy(
                activeCustomTestState = session.copy(
                    currentQuestionIndex = session.currentQuestionIndex + 1,
                    selectedOptionId = preselectedOptionId,
                    answers = updatedAnswers,
                    errorMessage = null
                )
            )
        }
    }

    private fun handleSubmitOrganizationCustomTest() {
        val session = _state.value.activeCustomTestState ?: return
        val question = session.currentQuestion ?: return
        val selectedOption = question.options.firstOrNull { it.id == session.selectedOptionId }
        if (selectedOption == null) {
            _state.update { state ->
                state.copy(activeCustomTestState = state.activeCustomTestState?.copy(errorMessage = currentLanguage().pick(
                    ru = "Выберите один вариант ответа",
                    en = "Select one answer option",
                    kz = "Бір жауап нұсқасын таңдаңыз"
                )))
            }
            return
        }

        val finalAnswers = session.answers.upsertCustomTestAnswer(
            OrganizationCustomTestAnswer(
                questionId = question.id,
                questionText = question.text,
                selectedOptionId = selectedOption.id,
                selectedOptionText = selectedOption.text,
                order = question.order
            )
        )
        _state.update { state ->
            state.copy(activeCustomTestState = session.copy(answers = finalAnswers, isSubmitting = true, errorMessage = null))
        }

        viewModelScope.launch {
            val now = currentTimeMillis()
            val currentState = _state.value
            val submission = OrganizationCustomTestSubmission(
                orgId = orgId,
                testId = session.test.id,
                testTitle = session.test.title,
                studentId = uid,
                studentName = currentState.profile.fullName.ifBlank {
                    currentState.profile.email.ifBlank {
                        currentLanguage().pick(
                            ru = "Студент",
                            en = "Student",
                            kz = "Студент"
                        )
                    }
                },
                submittedAt = now,
                answers = finalAnswers.sortedBy { it.order }
            )
            when (val result = firestoreService.submitOrganizationCustomTest(orgId, session.test.id, submission)) {
                is FirestoreResult.GenericSuccess -> {
                    loadData()
                    _state.update { it.copy(activeCustomTestState = null) }
                    emit(StudentEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Информация отправлена психологу",
                        en = "The information was sent to the psychologist",
                        kz = "Ақпарат психологқа жіберілді"
                    )))
                }

                is FirestoreResult.Failure -> _state.update { state ->
                    state.copy(
                        activeCustomTestState = state.activeCustomTestState?.copy(
                            isSubmitting = false,
                            errorMessage = result.message
                        )
                    )
                }

                else -> _state.update { state ->
                    state.copy(activeCustomTestState = state.activeCustomTestState?.copy(isSubmitting = false))
                }
            }
        }
    }

    private fun handleStartTest(testType: StudentTestType) {
        openTest(testType)
        emit(StudentEffect.NavigateToTest(testType))
    }

    private fun handleGenerateReport() {
        emit(StudentEffect.ShowSnackbar(currentLanguage().pick(
            ru = "📊 Генерация отчёта — функция в разработке",
            en = "📊 Report generation is under development",
            kz = "📊 Есепті құрастыру функциясы әзірленуде"
        )))
    }

    private fun currentLanguage(): AppLanguage = _state.value.currentLanguage

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
        val language = _state.value.currentLanguage
        val prompt = definition.buildPrompt(answers, score, assessment, language)

        viewModelScope.launch {
            geminiService.sendMessage(
                history = listOf(ChatMessage(role = "user", text = prompt)),
                language = language
            ).fold(
                onSuccess = { feedbackText ->
                    persistAndPresentResult(
                        definition = definition,
                        answers = answers,
                        score = score,
                        assessment = assessment,
                        feedbackText = normalizeAssistantText(feedbackText),
                        initialErrorMessage = null,
                        language = language
                    )
                },
                onFailure = {
                    val fallback = definition.buildFallback(score, assessment, language)
                    persistAndPresentResult(
                        definition = definition,
                        answers = answers,
                        score = score,
                        assessment = assessment,
                        feedbackText = fallback,
                        initialErrorMessage = language.pick(
                            ru = "⚠️ AI недоступен, показан локальный результат",
                            en = "⚠️ AI is unavailable, showing a local result",
                            kz = "⚠️ AI қолжетімсіз, жергілікті нәтиже көрсетілді"
                        ),
                        language = language
                    )
                }
            )
        }
    }

    private fun normalizeAssistantText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
            .replace(Regex("""(?m)^\s*[*•#]+\s*"""), "")
            .replace(Regex("""(?m)^\s*[-–—]+\s*"""), "— ")
            .replace(Regex("""[ \t]{2,}"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private suspend fun persistAndPresentResult(
        definition: com.example.aiphysical.data.model.StudentTestDefinition,
        answers: List<StudentTestAnswer>,
        score: Int,
        assessment: String,
        feedbackText: String,
        initialErrorMessage: String?,
        language: AppLanguage
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
            emit(StudentEffect.ShowSnackbar(language.pick(
                ru = "Не удалось сохранить результат теста",
                en = "Failed to save the test result",
                kz = "Тест нәтижесін сақтау мүмкін болмады"
            )))
        }

        val combinedError = listOfNotNull(
            initialErrorMessage,
            saveErrorMessage?.let {
                language.pick(
                    ru = "⚠️ Не удалось сохранить результат: $it",
                    en = "⚠️ Failed to save the result: $it",
                    kz = "⚠️ Нәтижені сақтау мүмкін болмады: $it"
                )
            }
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

    private fun List<OrganizationCustomTestAnswer>.upsertCustomTestAnswer(answer: OrganizationCustomTestAnswer): List<OrganizationCustomTestAnswer> =
        filterNot { it.questionId == answer.questionId } + answer

    companion object {
        fun factory(
            uid: String,
            orgId: String,
            firestoreService: FirestoreService,
            initialLanguage: AppLanguage = AppLanguage.RU,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                StudentViewModel(uid, orgId, firestoreService, initialLanguage = initialLanguage) as T
        }
    }
}
