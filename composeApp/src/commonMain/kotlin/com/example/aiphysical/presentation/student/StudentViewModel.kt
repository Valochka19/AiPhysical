package com.example.aiphysical.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aiphysical.data.model.ChatMessage
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.QuestionPolarity
import com.example.aiphysical.data.model.StudentTestAnswer
import com.example.aiphysical.data.model.StudentTestStep
import com.example.aiphysical.data.model.StudentTestSubmission
import com.example.aiphysical.data.model.StudentTestUiState
import com.example.aiphysical.data.model.computeOverallHealthPercent
import com.example.aiphysical.data.model.studentTestDefinitionFor
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
            is StudentEvent.NavigateToTab -> _state.update { it.copy(selectedTab = event.tab) }
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
            geminiService.sendMessage(newHistory).fold(
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
