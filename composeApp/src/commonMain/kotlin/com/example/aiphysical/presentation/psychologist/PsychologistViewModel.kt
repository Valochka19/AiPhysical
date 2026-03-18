package com.example.aiphysical.presentation.psychologist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.data.service.FirestoreResult
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.util.currentTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class PsychologistViewModel(
    private val orgId: String,
    private val uid: String,
    private val psychologistName: String,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val _state = MutableStateFlow(
        PsychologistHomeState(
            orgId = orgId,
            psychologistId = uid,
            psychologistName = psychologistName
        )
    )
    val state: StateFlow<PsychologistHomeState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PsychologistEffect>()
    val effects: SharedFlow<PsychologistEffect> = _effects.asSharedFlow()

    private var studentsObserverJob: Job? = null
    private var coursesObserverJob: Job? = null

    init {
        loadData()
        observeAddedCourses()
    }

    fun onEvent(event: PsychologistEvent) {
        when (event) {
            PsychologistEvent.LoadData -> loadData()

            is PsychologistEvent.SearchStudents -> handleSearch(event.query)

            is PsychologistEvent.SelectStudent -> handleSelectStudent(event.student)

            PsychologistEvent.BackToDashboard -> _state.update {
                it.copy(
                    currentScreen = PsychologistScreen.Dashboard,
                    selectedStudent = null,
                    selectedStudentTestHistory = emptyList()
                )
            }

            is PsychologistEvent.NavigateToTab -> _state.update { it.copy(selectedTab = event.tab) }

            // ── Recommendation sheet ──────────────────────────────────────────
            is PsychologistEvent.OpenRecommendationSheet -> _state.update {
                it.copy(
                    showRecommendationSheet = true,
                    recommendationTarget = event.student,
                    recommendationComment = event.student.psychComment,
                    recommendationCourseId = event.student.assignedCourseId,
                    recommendationCourseName = event.student.assignedCourseName,
                    recommendationPriority = event.student.psychPriority.ifBlank { "MEDIUM" }
                )
            }

            PsychologistEvent.DismissRecommendationSheet -> _state.update {
                it.copy(
                    showRecommendationSheet = false,
                    recommendationTarget = null,
                    recommendationComment = "",
                    recommendationCourseId = "",
                    recommendationCourseName = "",
                    recommendationPriority = "MEDIUM"
                )
            }

            is PsychologistEvent.UpdateRecommendationComment ->
                _state.update { it.copy(recommendationComment = event.text) }

            is PsychologistEvent.SelectRecommendationCourse -> _state.update {
                it.copy(
                    recommendationCourseId = event.courseId,
                    recommendationCourseName = event.courseName
                )
            }

            is PsychologistEvent.SetRecommendationPriority ->
                _state.update { it.copy(recommendationPriority = event.priority) }

            PsychologistEvent.SendRecommendation -> handleSendRecommendation()

            // Test result feed sheet
            is PsychologistEvent.ViewTestResult -> _state.update {
                it.copy(showTestResultSheet = true, selectedTestFeedItem = event.item)
            }
            PsychologistEvent.DismissTestResultSheet -> _state.update {
                it.copy(showTestResultSheet = false, selectedTestFeedItem = null)
            }

            PsychologistEvent.DismissSnackbar -> _state.update { it.copy(snackbarMessage = null) }

            is PsychologistEvent.SetAnalyticsFilter ->
                _state.update { it.copy(analyticsFilter = event.filter) }

            is PsychologistEvent.ChangeLanguage ->
                _state.update { it.copy(currentLanguage = event.language) }

            PsychologistEvent.Logout -> { /* handled externally in App.kt */ }

            // ── Add course form ───────────────────────────────────────────────
            PsychologistEvent.OpenAddCourseSheet -> _state.update { it.copy(showAddCourseSheet = true) }
            PsychologistEvent.CloseAddCourseSheet -> _state.update {
                it.copy(
                    showAddCourseSheet = false,
                    newCourseTitle = "", newCourseDescription = "",
                    newCourseType = com.example.aiphysical.data.model.CourseContentType.TEXT,
                    newCourseTextContent = "", newCourseVideoUrl = ""
                )
            }
            is PsychologistEvent.UpdateNewCourseTitle -> _state.update { it.copy(newCourseTitle = event.value) }
            is PsychologistEvent.UpdateNewCourseDescription -> _state.update { it.copy(newCourseDescription = event.value) }
            is PsychologistEvent.UpdateNewCourseType -> _state.update { it.copy(newCourseType = event.type) }
            is PsychologistEvent.UpdateNewCourseTextContent -> _state.update { it.copy(newCourseTextContent = event.value) }
            is PsychologistEvent.UpdateNewCourseVideoUrl -> _state.update { it.copy(newCourseVideoUrl = event.value) }
            PsychologistEvent.PublishCourse -> handlePublishCourse()

            // ── Added courses viewer ──────────────────────────────────────────
            PsychologistEvent.OpenAddedCourses -> _state.update { it.copy(showAddedCoursesViewer = true) }
            PsychologistEvent.CloseAddedCourses -> _state.update { it.copy(showAddedCoursesViewer = false) }
            is PsychologistEvent.OpenAddedCourse -> handleOpenAddedCourse(event.course)
            PsychologistEvent.CloseSelectedAddedCourse -> _state.update { it.copy(selectedAddedCourse = null) }
            is PsychologistEvent.DeleteAddedCourse -> handleDeleteCourse(event.courseId)
            PsychologistEvent.CloseTextCourseViewer -> _state.update {
                it.copy(showTextCourseViewer = false, selectedAddedCourse = null)
            }
        }
    }

    // ─── Data loading ─────────────────────────────────────────────────────────

    private fun loadData() {
        _state.update { it.copy(isLoading = true) }
        studentsObserverJob?.cancel()
        studentsObserverJob = viewModelScope.launch {
            firestoreService.observeOrganizationMembers(orgId)                .catch { _state.update { s -> s.copy(isLoading = false) } }
                .collect { result ->
                    when (result) {
                        is FirestoreResult.MembersSuccess -> {
                            // Filter only students (role == "user")
                            val students = result.members.filter { it.role == "user" }
                            val query = _state.value.searchQuery
                            val critical = students.filter { it.latestAiStatus == "critical" }
                            val stress   = students.filter { it.latestAiStatus == "stress" }
                            // Pending = has interacted with a test (status != unknown) but no comment yet
                            val pending  = students.filter {
                                it.latestAiStatus != "unknown" && it.psychComment.isBlank()
                            }
                            val analytics = computeAnalytics(students)
                            val recentFeed = students
                                .filter { it.latestAiStatus != "unknown" }
                                .sortedByDescending {
                                    when (it.latestAiStatus) { "critical" -> 2; "stress" -> 1; else -> 0 }
                                }
                                .take(20)
                                .map { s ->
                                    RecentTestFeedItem(
                                        studentId      = s.uid,
                                        studentName    = s.fullName,
                                        studentStatus  = s.latestAiStatus,
                                        stressScore    = s.stressScore,
                                        burnoutScore   = s.burnoutScore,
                                        anxietyScore   = s.anxietyScore,
                                        emotionScore   = s.emotionScore,
                                        motivationScore= s.motivationScore
                                    )
                                }
                            _state.update { s ->
                                s.copy(
                                    isLoading = false,
                                    students = students,
                                    filteredStudents = if (query.isBlank()) students
                                    else students.filter {
                                        it.fullName.contains(query, ignoreCase = true) ||
                                        it.email.contains(query, ignoreCase = true)
                                    },
                                    criticalStudents = critical,
                                    stressStudents   = stress,
                                    pendingRecommendations = pending,
                                    avgBurnout   = analytics[0],
                                    avgStress    = analytics[1],
                                    avgAnxiety   = analytics[2],
                                    avgEmotion   = analytics[3],
                                    avgMotivation = analytics[4],
                                    psychClimate = computeClimate(students),
                                    recentTestFeed = recentFeed
                                )
                            }
                        }
                        is FirestoreResult.Failure -> _state.update { it.copy(isLoading = false) }
                        else -> _state.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    private fun handleSearch(query: String) {
        val filtered = if (query.isBlank()) _state.value.students
        else _state.value.students.filter {
            it.fullName.contains(query, ignoreCase = true) ||
            it.email.contains(query, ignoreCase = true)
        }
        _state.update { it.copy(searchQuery = query, filteredStudents = filtered) }
    }

    private fun handleSelectStudent(student: UserProfile) {
        _state.update {
            it.copy(
                currentScreen = PsychologistScreen.StudentDetail,
                selectedStudent = student,
                isLoadingDetail = true
            )
        }
        viewModelScope.launch {
            val history = (firestoreService.getUserTestHistory(student.uid)
                    as? FirestoreResult.TestHistorySuccess)?.results ?: emptyList()
            _state.update { it.copy(isLoadingDetail = false, selectedStudentTestHistory = history) }
        }
    }

    private fun handleSendRecommendation() {
        val s = _state.value
        val target = s.recommendationTarget ?: return
        if (s.recommendationComment.isBlank()) {
            emitEffect(PsychologistEffect.ShowSnackbar("Введите текст рекомендации"))
            return
        }
        _state.update { it.copy(isSendingRecommendation = true) }
        viewModelScope.launch {
            val result = firestoreService.updateStudentRecommendation(
                studentId  = target.uid,
                comment    = s.recommendationComment.trim(),
                courseId   = s.recommendationCourseId,
                courseName = s.recommendationCourseName,
                priority   = s.recommendationPriority,
                psychId    = uid
            )
            when (result) {
                is FirestoreResult.GenericSuccess -> {
                    _state.update {
                        it.copy(
                            isSendingRecommendation = false,
                            showRecommendationSheet = false,
                            recommendationTarget = null,
                            recommendationComment = "",
                            recommendationCourseId = "",
                            recommendationCourseName = "",
                            recommendationPriority = "MEDIUM"
                        )
                    }
                    emitEffect(PsychologistEffect.ShowSnackbar("✅ Рекомендация отправлена студенту"))
                    emitEffect(PsychologistEffect.TriggerHaptic)
                }
                is FirestoreResult.Failure -> {
                    _state.update { it.copy(isSendingRecommendation = false) }
                    emitEffect(PsychologistEffect.ShowSnackbar("Ошибка: ${result.message}"))
                }
                else -> _state.update { it.copy(isSendingRecommendation = false) }
            }
        }
    }

    // ─── Org-level courses ────────────────────────────────────────────────────

    private fun observeAddedCourses() {
        if (orgId.isBlank()) return
        coursesObserverJob?.cancel()
        coursesObserverJob = viewModelScope.launch {
            firestoreService.observeOrganizationCourses(orgId)
                .catch { /* silently ignore */ }
                .collect { result ->
                    when (result) {
                        is FirestoreResult.OrganizationCoursesSuccess ->
                            _state.update { it.copy(addedCourses = result.courses.filter { c -> c.isPublished }) }
                        else -> { /* ignore */ }
                    }
                }
        }
    }

    private fun handlePublishCourse() {
        val s = _state.value
        // Validation
        if (s.newCourseTitle.isBlank()) {
            emitEffect(PsychologistEffect.ShowSnackbar("Введите название курса"))
            return
        }
        if (s.newCourseDescription.isBlank()) {
            emitEffect(PsychologistEffect.ShowSnackbar("Введите описание курса"))
            return
        }
        when (s.newCourseType) {
            CourseContentType.VIDEO -> {
                if (s.newCourseVideoUrl.isBlank()) {
                    emitEffect(PsychologistEffect.ShowSnackbar("Введите ссылку на видео"))
                    return
                }
                if (!s.newCourseVideoUrl.startsWith("http")) {
                    emitEffect(PsychologistEffect.ShowSnackbar("Введите корректную ссылку"))
                    return
                }
            }
            CourseContentType.TEXT -> {
                if (s.newCourseTextContent.isBlank()) {
                    emitEffect(PsychologistEffect.ShowSnackbar("Введите текст курса"))
                    return
                }
            }
        }

        _state.update { it.copy(isPublishingCourse = true) }
        viewModelScope.launch {
            val now = currentTimeMillis()
            val course = OrganizationCourse(
                orgId         = orgId,
                title         = s.newCourseTitle.trim(),
                description   = s.newCourseDescription.trim(),
                type          = s.newCourseType,
                contentText   = s.newCourseTextContent.trim(),
                videoUrl      = s.newCourseVideoUrl.trim(),
                createdBy     = uid,
                createdByName = s.psychologistName,
                createdAt     = now,
                updatedAt     = now,
                isPublished   = true
            )
            val result = firestoreService.createOrganizationCourse(orgId, course)
            when (result) {
                is FirestoreResult.GenericSuccess -> {
                    _state.update {
                        it.copy(
                            isPublishingCourse = false,
                            showAddCourseSheet = false,
                            newCourseTitle = "", newCourseDescription = "",
                            newCourseType = CourseContentType.TEXT,
                            newCourseTextContent = "", newCourseVideoUrl = ""
                        )
                    }
                    emitEffect(PsychologistEffect.ShowSnackbar("✅ Курс опубликован"))
                    emitEffect(PsychologistEffect.TriggerHaptic)
                }
                is FirestoreResult.Failure -> {
                    _state.update { it.copy(isPublishingCourse = false) }
                    emitEffect(PsychologistEffect.ShowSnackbar("Ошибка: ${result.message}"))
                }
                else -> _state.update { it.copy(isPublishingCourse = false) }
            }
        }
    }

    private fun handleDeleteCourse(courseId: String) {
        viewModelScope.launch {
            val result = firestoreService.deleteOrganizationCourse(orgId, courseId)
            when (result) {
                is FirestoreResult.GenericSuccess -> {
                    emitEffect(PsychologistEffect.ShowSnackbar("🗑 Курс удалён"))
                    emitEffect(PsychologistEffect.TriggerHaptic)
                }
                is FirestoreResult.Failure -> emitEffect(PsychologistEffect.ShowSnackbar("Ошибка удаления: ${result.message}"))
                else -> { /* ignore */ }
            }
        }
    }

    private fun handleOpenAddedCourse(course: OrganizationCourse) {
        when (course.type) {
            CourseContentType.VIDEO -> {
                if (course.videoUrl.isNotBlank()) {
                    emitEffect(PsychologistEffect.OpenUrl(course.videoUrl))
                } else {
                    emitEffect(PsychologistEffect.ShowSnackbar("Ссылка на видео недоступна"))
                }
            }
            CourseContentType.TEXT -> {
                _state.update { it.copy(selectedAddedCourse = course, showTextCourseViewer = true) }
            }
        }
    }

    // ─── Analytics helpers ────────────────────────────────────────────────────

    /** Returns [avgBurnout, avgStress, avgAnxiety, avgEmotion, avgMotivation] */
    private fun computeAnalytics(students: List<UserProfile>): FloatArray {
        if (students.isEmpty()) return floatArrayOf(0f, 0f, 0f, 50f, 50f)
        val n = students.size.toDouble()
        return floatArrayOf(
            (students.sumOf { it.burnoutScore.toDouble() }    / n).toFloat().coerceIn(0f, 100f),
            (students.sumOf { it.stressScore.toDouble() }     / n).toFloat().coerceIn(0f, 100f),
            (students.sumOf { it.anxietyScore.toDouble() }    / n).toFloat().coerceIn(0f, 100f),
            (students.sumOf { it.emotionScore.toDouble() }    / n).toFloat().coerceIn(0f, 100f),
            (students.sumOf { it.motivationScore.toDouble() } / n).toFloat().coerceIn(0f, 100f)
        )
    }

    private fun computeClimate(students: List<UserProfile>): String {
        if (students.isEmpty()) return "unknown"
        val n = students.size.toFloat()
        val critRatio   = students.count { it.latestAiStatus == "critical" } / n
        val stressRatio = students.count { it.latestAiStatus == "stress" }   / n
        return when {
            critRatio > 0.20f                    -> "critical"
            critRatio > 0.05f || stressRatio > 0.40f -> "warning"
            else                                 -> "good"
        }
    }

    private fun emitEffect(effect: PsychologistEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    // ─── Factory ──────────────────────────────────────────────────────────────

    companion object {
        fun factory(
            orgId: String,
            uid: String,
            psychologistName: String,
            firestoreService: FirestoreService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                PsychologistViewModel(orgId, uid, psychologistName, firestoreService) as T
        }
    }
}

