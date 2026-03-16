package com.example.aiphysical.presentation.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aiphysical.data.service.FirestoreResult
import com.example.aiphysical.data.service.FirestoreService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class StudentViewModel(
    private val uid: String,
    /** orgId reserved for psychologist-lookup and future org-level features */
    @Suppress("unused") private val orgId: String,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val _state = MutableStateFlow(StudentUiState())
    val state: StateFlow<StudentUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<StudentEffect>()
    @Suppress("unused")   // consumed by StudentDashboardScreen via collectLatest
    val effects: SharedFlow<StudentEffect> = _effects.asSharedFlow()

    init { loadData() }

    fun onEvent(event: StudentEvent) {
        when (event) {
            StudentEvent.LoadData         -> loadData()
            StudentEvent.Refresh          -> loadData(isRefresh = true)
            is StudentEvent.NavigateToTab -> _state.update { it.copy(selectedTab = event.tab) }
            is StudentEvent.StartTest     -> handleStartTest(event.testType)
            StudentEvent.GenerateReport   -> handleGenerateReport()
            StudentEvent.DismissError     -> _state.update { it.copy(errorMessage = null) }
            StudentEvent.Logout           -> { /* handled in App.kt */ }
            is StudentEvent.ChangeLanguage -> _state.update { it.copy(currentLanguage = event.language) }
        }
    }

    // ─── Data loading ─────────────────────────────────────────────────────────

    private fun loadData(isRefresh: Boolean = false) {
        if (isRefresh) _state.update { it.copy(isRefreshing = true) }
        else           _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            // Fetch profile, test history, and course progress concurrently
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
            val overall = computeOverallScore(profile)

            _state.update {
                it.copy(
                    isLoading      = false,
                    isRefreshing   = false,
                    profile        = profile,
                    testHistory    = history,
                    courseProgress = courses,
                    completedTestIds = completedIds,
                    overallScore   = overall,
                    errorMessage   = null
                )
            }
        }
    }

    // ─── Event handlers ───────────────────────────────────────────────────────

    private fun handleStartTest(testType: StudentTestType) {
        emit(StudentEffect.ShowSnackbar("🚀 Тест «${testType.label}» — функция в разработке"))
        emit(StudentEffect.NavigateToTest(testType))
    }

    private fun handleGenerateReport() {
        emit(StudentEffect.ShowSnackbar("📊 Генерация отчёта — функция в разработке"))
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    /**
     * Overall mental health score (0–100, higher = better).
     * emotion & motivation: higher is good
     * stress, burnout, anxiety: lower is good → invert
     */
    private fun computeOverallScore(p: com.example.aiphysical.data.model.UserProfile): Float {
        if (p.stressScore == 0f && p.burnoutScore == 0f &&
            p.anxietyScore == 0f && p.emotionScore == 50f && p.motivationScore == 50f
        ) return 0f   // no data yet

        return ((100f - p.stressScore)   +
                (100f - p.burnoutScore)  +
                (100f - p.anxietyScore)  +
                p.emotionScore           +
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

