package com.example.aiphysical.presentation.psychologist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aiphysical.data.model.BaseCourseCatalogItem
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.OrganizationCustomTest
import com.example.aiphysical.data.model.OrganizationCustomTestOption
import com.example.aiphysical.data.model.OrganizationCustomTestQuestion
import com.example.aiphysical.data.model.OrganizationTestStats
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.data.model.studentTestDefinitionFor
import com.example.aiphysical.data.service.FirestoreResult
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.presentation.student.StudentTestType
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
    private var customTestsObserverJob: Job? = null

    init {
        loadData()
        observeAddedCourses()
        observeCustomTests()
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
                    newCourseType = CourseContentType.TEXT,
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
            is PsychologistEvent.OpenBaseCourse -> handleOpenBaseCourse(event.course)
            is PsychologistEvent.OpenAddedCourse -> handleOpenAddedCourse(event.course)
            PsychologistEvent.CloseSelectedAddedCourse -> _state.update { it.copy(selectedAddedCourse = null) }
            is PsychologistEvent.DeleteAddedCourse -> handleDeleteCourse(event.courseId)
            PsychologistEvent.CloseTextCourseViewer -> _state.update {
                it.copy(showTextCourseViewer = false, selectedAddedCourse = null)
            }
            is PsychologistEvent.OpenTestStats -> handleOpenTestStats(event.testType)
            PsychologistEvent.CloseTestStatsDialog -> _state.update {
                it.copy(showTestStatsDialog = false, selectedTestStats = null)
            }

            PsychologistEvent.OpenAddTestScreen -> _state.update {
                it.copy(
                    currentScreen = PsychologistScreen.TestBuilder,
                    selectedTab = PsychologistTab.Library,
                    showDiscardCustomTestDialog = false
                )
            }
            PsychologistEvent.CloseAddTestScreen -> handleCloseAddTestScreen()
            PsychologistEvent.ConfirmCloseAddTestScreen -> resetCustomTestBuilder()
            PsychologistEvent.DismissCloseAddTestScreen -> _state.update {
                it.copy(showDiscardCustomTestDialog = false)
            }
            is PsychologistEvent.UpdateDraftTestTitle -> _state.update { it.copy(currentTestDraftTitle = event.value) }
            is PsychologistEvent.UpdateDraftQuestionText -> _state.update { it.copy(currentDraftQuestionText = event.value) }
            is PsychologistEvent.UpdateDraftOption1 -> _state.update { it.copy(currentDraftOption1 = event.value) }
            is PsychologistEvent.UpdateDraftOption2 -> _state.update { it.copy(currentDraftOption2 = event.value) }
            is PsychologistEvent.UpdateDraftOption3 -> _state.update { it.copy(currentDraftOption3 = event.value) }
            PsychologistEvent.AddNextDraftQuestion -> handleAddNextDraftQuestion()
            PsychologistEvent.PublishDraftTest -> handlePublishDraftTest()
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
            emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                ru = "Введите текст рекомендации",
                en = "Enter the recommendation text",
                kz = "Ұсыным мәтінін енгізіңіз"
            )))
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
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "✅ Рекомендация отправлена студенту",
                        en = "✅ Recommendation sent to the student",
                        kz = "✅ Ұсыным студентке жіберілді"
                    )))
                    emitEffect(PsychologistEffect.TriggerHaptic)
                }
                is FirestoreResult.Failure -> {
                    _state.update { it.copy(isSendingRecommendation = false) }
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Ошибка: ${result.message}",
                        en = "Error: ${result.message}",
                        kz = "Қате: ${result.message}"
                    )))
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

    private fun observeCustomTests() {
        if (orgId.isBlank()) return
        customTestsObserverJob?.cancel()
        _state.update { it.copy(isLoadingCustomTests = true) }
        customTestsObserverJob = viewModelScope.launch {
            firestoreService.observeOrganizationCustomTests(orgId)
                .catch {
                    _state.update { state -> state.copy(isLoadingCustomTests = false) }
                }
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

    private fun handlePublishCourse() {
        val s = _state.value
        // Validation
        if (s.newCourseTitle.isBlank()) {
            emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                ru = "Введите название курса",
                en = "Enter the course title",
                kz = "Курс атауын енгізіңіз"
            )))
            return
        }
        if (s.newCourseDescription.isBlank()) {
            emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                ru = "Введите описание курса",
                en = "Enter the course description",
                kz = "Курс сипаттамасын енгізіңіз"
            )))
            return
        }
        when (s.newCourseType) {
            CourseContentType.VIDEO -> {
                if (s.newCourseVideoUrl.isBlank()) {
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Введите ссылку на видео",
                        en = "Enter the video link",
                        kz = "Бейне сілтемесін енгізіңіз"
                    )))
                    return
                }
                if (!s.newCourseVideoUrl.startsWith("http")) {
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Введите корректную ссылку",
                        en = "Enter a valid link",
                        kz = "Дұрыс сілтемені енгізіңіз"
                    )))
                    return
                }
            }
            CourseContentType.TEXT -> {
                if (s.newCourseTextContent.isBlank()) {
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Введите текст курса",
                        en = "Enter the course text",
                        kz = "Курс мәтінін енгізіңіз"
                    )))
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
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "✅ Курс опубликован",
                        en = "✅ Course published",
                        kz = "✅ Курс жарияланды"
                    )))
                    emitEffect(PsychologistEffect.TriggerHaptic)
                }
                is FirestoreResult.Failure -> {
                    _state.update { it.copy(isPublishingCourse = false) }
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Ошибка: ${result.message}",
                        en = "Error: ${result.message}",
                        kz = "Қате: ${result.message}"
                    )))
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
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "🗑 Курс удалён",
                        en = "🗑 Course deleted",
                        kz = "🗑 Курс өшірілді"
                    )))
                    emitEffect(PsychologistEffect.TriggerHaptic)
                }
                is FirestoreResult.Failure -> emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                    ru = "Ошибка удаления: ${result.message}",
                    en = "Deletion error: ${result.message}",
                    kz = "Өшіру қатесі: ${result.message}"
                )))
                else -> { /* ignore */ }
            }
        }
    }

    private fun handleCloseAddTestScreen() {
        if (hasCustomTestDraftData(_state.value)) {
            _state.update { it.copy(showDiscardCustomTestDialog = true) }
        } else {
            resetCustomTestBuilder()
        }
    }

    private fun handleAddNextDraftQuestion() {
        val state = _state.value
        val validated = state.buildValidatedDraftQuestionOrNull()
        if (validated == null) {
            emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                ru = "Заполните название, вопрос и все 3 варианта ответа",
                en = "Fill in the title, the question, and all 3 answer options",
                kz = "Атауды, сұрақты және барлық 3 жауап нұсқасын толтырыңыз"
            )))
            return
        }
        _state.update {
            it.copy(
                draftQuestions = it.draftQuestions + validated,
                currentDraftQuestionText = "",
                currentDraftOption1 = "",
                currentDraftOption2 = "",
                currentDraftOption3 = "",
                currentDraftQuestionIndex = validated.order + 1
            )
        }
        emitEffect(PsychologistEffect.TriggerHaptic)
    }

    private fun handlePublishDraftTest() {
        val state = _state.value
        val finalQuestions = when {
            state.buildValidatedDraftQuestionOrNull() != null -> state.draftQuestions + state.buildValidatedDraftQuestionOrNull()!!
            state.hasPartialCurrentDraftQuestion() -> {
                emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                    ru = "Заполните текущий вопрос полностью или очистите его перед публикацией",
                    en = "Complete the current question fully or clear it before publishing",
                    kz = "Жариялау алдында ағымдағы сұрақты толық толтырыңыз немесе тазалаңыз"
                )))
                return
            }
            state.draftQuestions.isEmpty() -> {
                emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                    ru = "Добавьте хотя бы один вопрос",
                    en = "Add at least one question",
                    kz = "Кемінде бір сұрақ қосыңыз"
                )))
                return
            }
            state.currentTestDraftTitle.trim().isBlank() -> {
                emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                    ru = "Введите название теста",
                    en = "Enter the test title",
                    kz = "Тест атауын енгізіңіз"
                )))
                return
            }
            else -> state.draftQuestions
        }

        _state.update { it.copy(isPublishingCustomTest = true) }
        viewModelScope.launch {
            val now = currentTimeMillis()
            val test = OrganizationCustomTest(
                orgId = orgId,
                title = state.currentTestDraftTitle.trim(),
                questions = finalQuestions,
                createdBy = uid,
                createdByName = state.psychologistName,
                createdAt = now,
                updatedAt = now,
                isPublished = true
            )
            when (val result = firestoreService.createOrganizationCustomTest(orgId, test)) {
                is FirestoreResult.GenericSuccess -> {
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "✅ Тест опубликован",
                        en = "✅ Test published",
                        kz = "✅ Тест жарияланды"
                    )))
                    emitEffect(PsychologistEffect.TriggerHaptic)
                    resetCustomTestBuilder(showSnackbar = false)
                }

                is FirestoreResult.Failure -> {
                    _state.update { it.copy(isPublishingCustomTest = false) }
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Ошибка: ${result.message}",
                        en = "Error: ${result.message}",
                        kz = "Қате: ${result.message}"
                    )))
                }

                else -> _state.update { it.copy(isPublishingCustomTest = false) }
            }
        }
    }

    private fun handleOpenBaseCourse(course: BaseCourseCatalogItem) {
        if (course.courseUrl.isBlank()) {
            emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                ru = "Ссылка на курс недоступна",
                en = "The course link is unavailable",
                kz = "Курс сілтемесі қолжетімсіз"
            )))
            return
        }

        emitEffect(PsychologistEffect.OpenUrl(course.courseUrl))
        viewModelScope.launch {
            when (firestoreService.upsertBaseCourseProgress(uid, course)) {
                is FirestoreResult.Failure -> emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                    ru = "Не удалось обновить прогресс курса",
                    en = "Failed to update course progress",
                    kz = "Курс прогресін жаңарту мүмкін болмады"
                )))
                else -> Unit
            }
        }
    }

    private fun handleOpenAddedCourse(course: OrganizationCourse) {
        when (course.type) {
            CourseContentType.VIDEO -> {
                if (course.videoUrl.isNotBlank()) {
                    emitEffect(PsychologistEffect.OpenUrl(course.videoUrl))
                } else {
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Ссылка на видео недоступна",
                        en = "The video link is unavailable",
                        kz = "Бейне сілтемесі қолжетімсіз"
                    )))
                }
            }
            CourseContentType.TEXT -> {
                _state.update { it.copy(selectedAddedCourse = course, showTextCourseViewer = true) }
            }
        }
    }

    private fun handleOpenTestStats(testType: StudentTestType) {
        val cached = _state.value.testStats.firstOrNull { it.testType == testType }
        if (cached != null) {
            _state.update { it.copy(selectedTestStats = cached, showTestStatsDialog = true) }
            return
        }

        _state.update {
            it.copy(
                isLoadingTestStats = true,
                selectedTestStats = OrganizationTestStats(
                    testType = testType,
                    testId = testType.testId,
                    testName = studentTestDefinitionFor(testType).testName
                )
            )
        }
        viewModelScope.launch {
            when (val result = firestoreService.getOrganizationTestStats(orgId)) {
                is FirestoreResult.OrganizationTestStatsSuccess -> {
                    val selected = result.stats.firstOrNull { it.testType == testType }
                    _state.update {
                        it.copy(
                            testStats = result.stats,
                            isLoadingTestStats = false,
                            selectedTestStats = selected,
                            showTestStatsDialog = true
                        )
                    }
                }

                is FirestoreResult.Failure -> {
                    _state.update { it.copy(isLoadingTestStats = false) }
                    emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                        ru = "Не удалось загрузить статистику теста",
                        en = "Failed to load test statistics",
                        kz = "Тест статистикасын жүктеу мүмкін болмады"
                    )))
                }

                else -> _state.update { it.copy(isLoadingTestStats = false) }
            }
        }
    }

    private fun resetCustomTestBuilder(showSnackbar: Boolean = false) {
        _state.update {
            it.copy(
                currentScreen = PsychologistScreen.Dashboard,
                selectedTab = PsychologistTab.Library,
                currentTestDraftTitle = "",
                draftQuestions = emptyList(),
                currentDraftQuestionText = "",
                currentDraftOption1 = "",
                currentDraftOption2 = "",
                currentDraftOption3 = "",
                currentDraftQuestionIndex = 1,
                isPublishingCustomTest = false,
                showDiscardCustomTestDialog = false
            )
        }
        if (showSnackbar) {
            emitEffect(PsychologistEffect.ShowSnackbar(currentLanguage().pick(
                ru = "Создание теста отменено",
                en = "Test creation cancelled",
                kz = "Тест құру тоқтатылды"
            )))
        }
    }

    private fun currentLanguage() = _state.value.currentLanguage

    private fun hasCustomTestDraftData(state: PsychologistHomeState): Boolean =
        state.currentTestDraftTitle.isNotBlank() ||
            state.draftQuestions.isNotEmpty() ||
            state.currentDraftQuestionText.isNotBlank() ||
            state.currentDraftOption1.isNotBlank() ||
            state.currentDraftOption2.isNotBlank() ||
            state.currentDraftOption3.isNotBlank()

    private fun PsychologistHomeState.hasPartialCurrentDraftQuestion(): Boolean {
        val question = currentDraftQuestionText.trim()
        val option1 = currentDraftOption1.trim()
        val option2 = currentDraftOption2.trim()
        val option3 = currentDraftOption3.trim()
        val anyFilled = question.isNotBlank() || option1.isNotBlank() || option2.isNotBlank() || option3.isNotBlank()
        val allFilled = question.isNotBlank() && option1.isNotBlank() && option2.isNotBlank() && option3.isNotBlank()
        return anyFilled && !allFilled
    }

    private fun PsychologistHomeState.buildValidatedDraftQuestionOrNull(): OrganizationCustomTestQuestion? {
        val title = currentTestDraftTitle.trim()
        val questionText = currentDraftQuestionText.trim()
        val options = listOf(
            currentDraftOption1.trim(),
            currentDraftOption2.trim(),
            currentDraftOption3.trim()
        )
        if (title.isBlank() || questionText.isBlank() || options.any { it.isBlank() }) return null
        val order = draftQuestions.size + 1
        return OrganizationCustomTestQuestion(
            id = "q_$order",
            order = order,
            text = questionText,
            options = options.mapIndexed { index, option ->
                OrganizationCustomTestOption(
                    id = "q_${order}_o_${index + 1}",
                    order = index + 1,
                    text = option
                )
            }
        )
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

