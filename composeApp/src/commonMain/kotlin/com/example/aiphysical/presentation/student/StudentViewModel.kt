package com.example.aiphysical.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aiphysical.data.model.ChatMessage
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.service.FirestoreResult
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.data.service.GeminiService
import com.example.aiphysical.util.createGeminiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/** Rough token estimate: 1 token ≈ 4 characters */
private const val MAX_TOKENS_PER_MESSAGE = 50_000
private const val CHARS_PER_TOKEN        = 4

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
            StudentEvent.LoadData          -> loadData()
            StudentEvent.Refresh           -> loadData(isRefresh = true)
            is StudentEvent.NavigateToTab  -> _state.update { it.copy(selectedTab = event.tab) }
            is StudentEvent.StartTest      -> handleStartTest(event.testType)
            StudentEvent.GenerateReport    -> handleGenerateReport()
            StudentEvent.DismissError      -> _state.update { it.copy(errorMessage = null) }
            StudentEvent.Logout            -> { /* handled in App.kt */ }
            is StudentEvent.ChangeLanguage -> _state.update { it.copy(currentLanguage = event.language) }
            // ── Added courses ─────────────────────────────────────────────────
            StudentEvent.OpenAddedCourses         -> _state.update { it.copy(showAddedCoursesViewer = true) }
            StudentEvent.CloseAddedCourses        -> _state.update { it.copy(showAddedCoursesViewer = false) }
            is StudentEvent.OpenAddedCourse       -> handleOpenAddedCourse(event.course)
            StudentEvent.CloseSelectedAddedCourse -> _state.update { it.copy(selectedAddedCourse = null) }
            is StudentEvent.OpenTextCourse        -> _state.update {
                it.copy(selectedAddedCourse = event.course, showTextCourseViewer = true)
            }
            StudentEvent.CloseTextCourse -> _state.update {
                it.copy(showTextCourseViewer = false, selectedAddedCourse = null)
            }
            // ── AI Chat ───────────────────────────────────────────────────────
            is StudentEvent.SendChatMessage -> handleSendChatMessage(event.message)
            is StudentEvent.UpdateChatInput -> _state.update { it.copy(chatInput = event.text) }
            StudentEvent.ClearChatError     -> _state.update { it.copy(chatError = null) }
            StudentEvent.ClearChatHistory   -> _state.update {
                it.copy(chatMessages = emptyList(), chatError = null)
            }
        }
    }

    // ─── AI Chat ──────────────────────────────────────────────────────────────

    private fun handleSendChatMessage(message: String) {
        if (message.isBlank()) return

        // Token guard: 1 token ≈ 4 chars → 50 000 tokens ≈ 200 000 chars
        val estimatedTokens = message.length / CHARS_PER_TOKEN
        if (estimatedTokens > MAX_TOKENS_PER_MESSAGE) {
            _state.update {
                it.copy(
                    chatError = "⚠️ Сообщение слишком длинное (~$estimatedTokens токенов). " +
                            "Максимум — 50 000 токенов. Сократите текст."
                )
            }
            return
        }

        val userMsg    = ChatMessage(role = "user", text = message)
        val newHistory = _state.value.chatMessages + userMsg
        _state.update {
            it.copy(
                chatMessages  = newHistory,
                chatInput     = "",
                isChatLoading = true,
                chatError     = null
            )
        }

        viewModelScope.launch {
            geminiService.sendMessage(newHistory).fold(
                onSuccess = { responseText ->
                    val modelMsg = ChatMessage(role = "model", text = responseText)
                    _state.update {
                        it.copy(
                            chatMessages  = it.chatMessages + modelMsg,
                            isChatLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    val errMsg = ChatMessage(
                        role    = "model",
                        text    = "Ошибка: ${error.message ?: "Неизвестная ошибка"}",
                        isError = true
                    )
                    _state.update {
                        it.copy(
                            chatMessages  = it.chatMessages + errMsg,
                            isChatLoading = false,
                            chatError     = error.message
                        )
                    }
                }
            )
        }
    }

    // ─── Data loading ─────────────────────────────────────────────────────────

    private fun loadData(isRefresh: Boolean = false) {
        if (isRefresh) _state.update { it.copy(isRefreshing = true) }
        else           _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val profileResult = firestoreService.getUserProfile(uid)
            val historyResult = firestoreService.getUserTestHistory(uid)
            val courseResult  = firestoreService.getUserCourseProgress(uid)

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
            val overall      = computeOverallScore(profile)

            _state.update {
                it.copy(
                    isLoading        = false,
                    isRefreshing     = false,
                    profile          = profile,
                    testHistory      = history,
                    courseProgress   = courses,
                    completedTestIds = completedIds,
                    overallScore     = overall,
                    errorMessage     = null
                )
            }
        }
    }

    private fun observeAddedCourses() {
        if (orgId.isBlank()) return
        coursesObserverJob?.cancel()
        coursesObserverJob = viewModelScope.launch {
            firestoreService.observeOrganizationCourses(orgId)
                .catch { /* silently ignore */ }
                .collect { result ->
                    when (result) {
                        is FirestoreResult.OrganizationCoursesSuccess ->
                            _state.update {
                                it.copy(addedCourses = result.courses.filter { c -> c.isPublished })
                            }
                        else -> { /* ignore */ }
                    }
                }
        }
    }

    // ─── Event handlers ───────────────────────────────────────────────────────

    private fun handleOpenAddedCourse(course: com.example.aiphysical.data.model.OrganizationCourse) {
        when (course.type) {
            CourseContentType.VIDEO -> {
                if (course.videoUrl.isNotBlank()) {
                    emit(StudentEffect.OpenUrl(course.videoUrl))
                } else {
                    emit(StudentEffect.ShowSnackbar("Ссылка на видео недоступна"))
                }
            }
            CourseContentType.TEXT -> {
                _state.update { it.copy(selectedAddedCourse = course, showTextCourseViewer = true) }
            }
        }
    }

    private fun handleStartTest(testType: StudentTestType) {
        emit(StudentEffect.ShowSnackbar("🚀 Тест «${testType.label}» — функция в разработке"))
        emit(StudentEffect.NavigateToTest(testType))
    }

    private fun handleGenerateReport() {
        emit(StudentEffect.ShowSnackbar("📊 Генерация отчёта — функция в разработке"))
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    private fun computeOverallScore(p: com.example.aiphysical.data.model.UserProfile): Float {
        if (p.stressScore == 0f && p.burnoutScore == 0f &&
            p.anxietyScore == 0f && p.emotionScore == 50f && p.motivationScore == 50f
        ) return 0f
        return ((100f - p.stressScore)  +
                (100f - p.burnoutScore) +
                (100f - p.anxietyScore) +
                p.emotionScore          +
                p.motivationScore) / 5f
    }

    // ─── Side-effect helper ───────────────────────────────────────────────────

    private fun emit(effect: StudentEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    // ─── Factory ──────────────────────────────────────────────────────────────

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
