package com.example.aiphysical.presentation.director

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.aiphysical.data.model.*
import com.example.aiphysical.data.service.FirestoreResult
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.presentation.auth.AppLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.reflect.KClass

class DirectorDashboardViewModel(
    val orgId: String,
    val uid: String,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val _state = MutableStateFlow(DirectorDashboardState())
    val state: StateFlow<DirectorDashboardState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<DirectorEffect>()
    val effects: SharedFlow<DirectorEffect> = _effects.asSharedFlow()

    /** Job реального времени: отменяется и перезапускается при RefreshData. */
    private var membersObserverJob: Job? = null
    private var coursesObserverJob: Job? = null

    init {
        loadDashboard()
        loadAiInsight()
        observeAddedCourses()
        refreshContentInsights()
    }

    fun onEvent(event: DirectorEvent) {
        when (event) {
            DirectorEvent.LoadDashboard, DirectorEvent.RefreshData -> {
                loadDashboard()
                loadAiInsight()
            }
            is DirectorEvent.SearchMembers -> handleSearch(event.query)
            is DirectorEvent.SelectMember -> handleSelectMember(event.member)
            DirectorEvent.BackToDashboard -> _state.update {
                it.copy(
                    currentScreen = DirectorPanelScreen.Dashboard,
                    selectedMember = null,
                    selectedMemberTestHistory = emptyList(),
                    selectedMemberCourseProgress = emptyList()
                )
            }
            is DirectorEvent.OpenContactDialog -> _state.update {
                it.copy(showContactDialog = true, contactTargetMember = event.member)
            }
            DirectorEvent.DismissContactDialog -> _state.update {
                it.copy(showContactDialog = false, contactTargetMember = null)
            }
            DirectorEvent.CopyStudentCode -> {
                val code = _state.value.organization?.inviteCodeStudent ?: ""
                val msg = snackMsg(_state.value.currentLanguage, "student_code")
                emitEffect(DirectorEffect.CopyToClipboard(code, msg))
                emitEffect(DirectorEffect.TriggerHaptic)
            }
            DirectorEvent.CopyPsychCode -> {
                val code = _state.value.organization?.inviteCodePsych ?: ""
                val msg = snackMsg(_state.value.currentLanguage, "psych_code")
                emitEffect(DirectorEffect.CopyToClipboard(code, msg))
                emitEffect(DirectorEffect.TriggerHaptic)
            }
            DirectorEvent.ShareStudentCode -> {
                val code = _state.value.organization?.inviteCodeStudent ?: ""
                emitEffect(DirectorEffect.OpenUrl("https://aiphysical.app/join?role=student&code=$code"))
                emitEffect(DirectorEffect.TriggerHaptic)
            }
            DirectorEvent.SharePsychCode -> {
                val code = _state.value.organization?.inviteCodePsych ?: ""
                emitEffect(DirectorEffect.OpenUrl("https://aiphysical.app/join?role=psych&code=$code"))
                emitEffect(DirectorEffect.TriggerHaptic)
            }
            is DirectorEvent.ChangeLanguage -> _state.update { it.copy(currentLanguage = event.language) }
            DirectorEvent.DismissSnackbar -> _state.update { it.copy(snackbarMessage = null) }
            DirectorEvent.Logout -> { /* handled by parent AuthViewModel via App.kt */ }

            // ── Tab navigation ────────────────────────────────────────────────
            is DirectorEvent.NavigateToTab -> _state.update { it.copy(selectedTab = event.tab) }

            // ── AI Insight ────────────────────────────────────────────────────
            DirectorEvent.LoadAiInsight -> loadAiInsight()

            // ── Invite sheet ──────────────────────────────────────────────────
            DirectorEvent.OpenInviteSheet -> {
                _state.update { it.copy(showInviteSheet = true) }
                emitEffect(DirectorEffect.TriggerHaptic)
            }
            DirectorEvent.DismissInviteSheet -> _state.update { it.copy(showInviteSheet = false) }

            // ── Role change ───────────────────────────────────────────────────
            is DirectorEvent.OpenRoleChangeSheet -> {
                _state.update { it.copy(showRoleChangeSheet = true, roleChangeTarget = event.member) }
                emitEffect(DirectorEffect.TriggerHaptic)
            }
            DirectorEvent.DismissRoleChangeSheet -> _state.update {
                it.copy(showRoleChangeSheet = false, roleChangeTarget = null)
            }
            is DirectorEvent.ChangeUserRole -> handleChangeRole(event.uid, event.newRole)

            // ── Block / unblock ───────────────────────────────────────────────
            is DirectorEvent.ToggleUserBlock -> handleToggleBlock(event.uid)

            // ── Analytics filter ──────────────────────────────────────────────
            is DirectorEvent.SetAnalyticsFilter -> _state.update { it.copy(analyticsFilter = event.filter) }

            // ── Added courses ─────────────────────────────────────────────────
            is DirectorEvent.OpenBaseCourse -> handleOpenBaseCourse(event.course)
            is DirectorEvent.OpenBaseCourseCompletion -> handleOpenBaseCourseCompletion(event.courseId)
            DirectorEvent.CloseBaseCourseCompletionDialog -> _state.update {
                it.copy(showBaseCourseCompletionDialog = false, selectedBaseCourseCompletion = null)
            }
            is DirectorEvent.OpenTestStats -> handleOpenTestStats(event.testType)
            DirectorEvent.CloseTestStatsDialog -> _state.update {
                it.copy(showTestStatsDialog = false, selectedTestStats = null)
            }
            is DirectorEvent.OpenAddedCourse -> handleOpenAddedCourse(event.course)
            DirectorEvent.CloseTextCourseViewer -> _state.update {
                it.copy(showTextCourseViewer = false, selectedAddedCourse = null)
            }
        }
    }

    // ─── Data loading ─────────────────────────────────────────────────────────

    private fun loadDashboard() {
        _state.update { it.copy(isLoading = true) }

        // ── Шаг А: одноразовый запрос организации ────────────────────────────
        viewModelScope.launch {
            val org = (firestoreService.getOrganization(orgId) as? FirestoreResult.OrgSuccess)?.org
            _state.update { it.copy(organization = org) }
        }

        // ── Шаг Б: real-time подписка на участников организации ──────────────
        //
        // Firestore query:  db.collection("users").whereEqualTo("orgId", orgId)
        //
        // observeOrganizationMembers() возвращает callbackFlow, который
        // обёртывает addSnapshotListener. Каждый раз, когда любой документ
        // в коллекции users с orgId == this.orgId изменяется, Flow эмитирует
        // новый MembersSuccess со свежим списком — без ручного refresh.
        membersObserverJob?.cancel()
        membersObserverJob = viewModelScope.launch {
            firestoreService.observeOrganizationMembers(orgId)
                .catch { _state.update { it.copy(isLoading = false) } }
                .collect { result ->
                    when (result) {
                        is FirestoreResult.MembersSuccess -> {
                            val members = result.members
                            val query   = _state.value.searchQuery
                            val critical     = members.filter { it.latestAiStatus == "critical" }
                            val psychologists = members.filter { it.role == "psychologist" }
                            _state.update { s ->
                                s.copy(
                                    isLoading = false,
                                    members = members,
                                    filteredMembers = if (query.isBlank()) members
                                        else members.filter { m -> m.fullName.contains(query, ignoreCase = true) },
                                    kpiData = computeKpi(members),
                                    trendPoints = generateTrendData(members),
                                    criticalMembers = critical,
                                    psychologists = psychologists
                                )
                            }
                            refreshContentInsights()
                        }
                        is FirestoreResult.Failure ->
                            _state.update { it.copy(isLoading = false) }
                        else ->
                            _state.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    private fun loadAiInsight() {
        _state.update { it.copy(isAiLoading = true, aiInsightText = "") }
        viewModelScope.launch {
            delay(1800L) // Simulate AI processing
            val insight = generateAiInsight(_state.value.organization, _state.value.members)
            _state.update { it.copy(isAiLoading = false, aiInsightText = insight) }
        }
    }

    private fun handleSearch(query: String) {
        val filtered = if (query.isBlank()) _state.value.members
        else _state.value.members.filter {
            it.fullName.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
        }
        _state.update { it.copy(searchQuery = query, filteredMembers = filtered) }
    }

    private fun handleSelectMember(member: UserProfile) {
        _state.update {
            it.copy(
                currentScreen = DirectorPanelScreen.MemberDetail,
                selectedMember = member,
                isLoadingDetail = true
            )
        }
        viewModelScope.launch {
            val histDeferred = async { firestoreService.getUserTestHistory(member.uid) }
            val progDeferred = async { firestoreService.getUserCourseProgress(member.uid) }
            val history = (histDeferred.await() as? FirestoreResult.TestHistorySuccess)?.results ?: emptyList()
            val progress = (progDeferred.await() as? FirestoreResult.CourseProgressSuccess)?.progressList ?: emptyList()
            _state.update { it.copy(isLoadingDetail = false, selectedMemberTestHistory = history, selectedMemberCourseProgress = progress) }
        }
    }

    private fun handleChangeRole(uid: String, newRole: String) {
        viewModelScope.launch {
            firestoreService.updateUserRole(uid, newRole)
            val updatedMembers = _state.value.members.map { if (it.uid == uid) it.copy(role = newRole) else it }
            val query = _state.value.searchQuery
            _state.update { s ->
                s.copy(
                    members = updatedMembers,
                    filteredMembers = if (query.isBlank()) updatedMembers
                    else updatedMembers.filter { it.fullName.contains(query, ignoreCase = true) },
                    showRoleChangeSheet = false,
                    roleChangeTarget = null
                )
            }
            emitEffect(DirectorEffect.ShowSnackbar(snackMsg(_state.value.currentLanguage, "role_changed")))
            emitEffect(DirectorEffect.TriggerHaptic)
        }
    }

    private fun handleToggleBlock(uid: String) {
        val member = _state.value.members.find { it.uid == uid } ?: return
        val newBlocked = !member.isBlocked
        viewModelScope.launch {
            firestoreService.updateUserBlockStatus(uid, newBlocked)
            val updatedMembers = _state.value.members.map { if (it.uid == uid) it.copy(isBlocked = newBlocked) else it }
            val query = _state.value.searchQuery
            _state.update { s ->
                s.copy(
                    members = updatedMembers,
                    filteredMembers = if (query.isBlank()) updatedMembers
                    else updatedMembers.filter { it.fullName.contains(query, ignoreCase = true) }
                )
            }
            val msgKey = if (newBlocked) "user_blocked" else "user_unblocked"
            emitEffect(DirectorEffect.ShowSnackbar(snackMsg(_state.value.currentLanguage, msgKey)))
            emitEffect(DirectorEffect.TriggerHaptic)
        }
    }

    // ─── Analytics computation ────────────────────────────────────────────────

    private fun computeKpi(members: List<UserProfile>): KpiData {
        if (members.isEmpty()) return KpiData()
        val total = members.size.toFloat()
        val critical = members.count { it.latestAiStatus == "critical" }.toFloat()
        val stress = members.count { it.latestAiStatus == "stress" }.toFloat()
        val burnout = ((critical * 1.0f + stress * 0.5f) / total * 100f).coerceIn(0f, 100f)
        val avgStress = (members.sumOf { it.stressScore.toDouble() } / total).toFloat().coerceIn(0f, 100f)
        val avgCourse = (members.sumOf { it.courseProgressPercent.toDouble() } / total).toFloat().coerceIn(0f, 100f)
        val avgBurnout = (members.sumOf { it.burnoutScore.toDouble() } / total).toFloat().coerceIn(0f, 100f)
        val avgEmotion = (members.sumOf { it.emotionScore.toDouble() } / total).toFloat().coerceIn(0f, 100f)
        val avgMotivation = (members.sumOf { it.motivationScore.toDouble() } / total).toFloat().coerceIn(0f, 100f)
        val avgAnxiety = (members.sumOf { it.anxietyScore.toDouble() } / total).toFloat().coerceIn(0f, 100f)
        return KpiData(
            burnoutIndex = burnout, avgStressLevel = avgStress, courseEngagement = avgCourse,
            avgBurnout = avgBurnout, avgEmotion = avgEmotion, avgMotivation = avgMotivation, avgAnxiety = avgAnxiety
        )
    }

    private fun generateTrendData(members: List<UserProfile>): List<TrendPoint> {
        val baseStress = if (members.isEmpty()) 35f
        else (members.sumOf { it.stressScore.toDouble() } / members.size).toFloat().coerceIn(10f, 90f)
        val baseBurnout = if (members.isEmpty()) 25f
        else members.map { m -> when (m.latestAiStatus) { "critical" -> 75f; "stress" -> 45f; else -> 20f } }
            .average().toFloat().coerceIn(5f, 95f)
        return (0..29).map { day ->
            val noise = Random.nextFloat() * 22f - 11f
            TrendPoint(
                dayOffset = day - 29,
                stressValue = (baseStress + noise).coerceIn(0f, 100f),
                burnoutValue = (baseBurnout + noise * 0.7f).coerceIn(0f, 100f)
            )
        }
    }

    private fun generateAiInsight(org: Organization?, members: List<UserProfile>): String {
        if (members.isEmpty()) return "Нет данных для анализа организации."
        val criticalCount = members.count { it.latestAiStatus == "critical" }
        val stressCount = members.count { it.latestAiStatus == "stress" }
        val normalCount = members.count { it.latestAiStatus == "normal" }
        val avgStress = (members.sumOf { it.stressScore.toDouble() } / members.size).toFloat()
        val avgBurnout = (members.sumOf { it.burnoutScore.toDouble() } / members.size).toFloat()
        val avgMotivation = (members.sumOf { it.motivationScore.toDouble() } / members.size).toFloat()
        return buildString {
            append("Анализ «${org?.name ?: "организации"}»: ")
            when {
                criticalCount > 0 -> append("⚠️ Обнаружено $criticalCount случаев критического состояния — рекомендуется вмешательство психолога. ")
                stressCount > members.size / 2 -> append("⚡ Более половины участников испытывают повышенный стресс. ")
                else -> append("✅ Общая ситуация в норме. ")
            }
            if (avgBurnout > 65f) append("Высокий риск выгорания (${avgBurnout.toInt()}%). ")
            else if (avgBurnout > 40f) append("Умеренный риск выгорания (${avgBurnout.toInt()}%). ")
            if (avgMotivation < 40f) append("Низкая мотивация — рекомендуются командные активности. ")
            append("Средний стресс: ${avgStress.toInt()}%. Норма: $normalCount/${members.size}.")
        }
    }

    // ─── Org-level courses ─────────────────────────────────────────────────────

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

    private fun refreshContentInsights() {
        if (orgId.isBlank()) return
        viewModelScope.launch {
            when (val result = firestoreService.getOrganizationBaseCourseCompletionStats(orgId)) {
                is FirestoreResult.BaseCourseCompletionStatsSuccess -> _state.update {
                    it.copy(baseCourseCompletionStats = result.stats)
                }
                else -> Unit
            }

            when (val result = firestoreService.getOrganizationTestStats(orgId)) {
                is FirestoreResult.OrganizationTestStatsSuccess -> _state.update {
                    it.copy(testStats = result.stats)
                }
                else -> Unit
            }
        }
    }

    private fun handleOpenBaseCourse(course: BaseCourseCatalogItem) {
        if (course.courseUrl.isBlank()) {
            emitEffect(DirectorEffect.ShowSnackbar("Ссылка на курс недоступна"))
            return
        }

        emitEffect(DirectorEffect.OpenUrl(course.courseUrl))
        viewModelScope.launch {
            when (firestoreService.upsertBaseCourseProgress(uid, course)) {
                is FirestoreResult.Failure -> emitEffect(DirectorEffect.ShowSnackbar("Не удалось обновить прогресс курса"))
                else -> Unit
            }
        }
    }

    private fun handleOpenBaseCourseCompletion(courseId: String) {
        val courseName = AppCourseCatalog.baseCourseById(courseId)?.title.orEmpty()
        _state.update {
            it.copy(
                isLoadingCourseCompletionDetails = true,
                selectedBaseCourseCompletion = BaseCourseCompletionDetails(
                    courseId = courseId,
                    courseName = courseName
                )
            )
        }
        viewModelScope.launch {
            when (val result = firestoreService.getOrganizationBaseCourseCompletionDetails(orgId, courseId)) {
                is FirestoreResult.BaseCourseCompletionDetailsSuccess -> _state.update {
                    it.copy(
                        isLoadingCourseCompletionDetails = false,
                        selectedBaseCourseCompletion = result.details,
                        showBaseCourseCompletionDialog = true
                    )
                }
                is FirestoreResult.Failure -> {
                    _state.update { it.copy(isLoadingCourseCompletionDetails = false) }
                    emitEffect(DirectorEffect.ShowSnackbar("Не удалось загрузить прохождение курса"))
                }
                else -> _state.update { it.copy(isLoadingCourseCompletionDetails = false) }
            }
        }
    }

    private fun handleOpenTestStats(testType: com.example.aiphysical.presentation.student.StudentTestType) {
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
                    emitEffect(DirectorEffect.ShowSnackbar("Не удалось загрузить статистику теста"))
                }
                else -> _state.update { it.copy(isLoadingTestStats = false) }
            }
        }
    }

    private fun handleOpenAddedCourse(course: OrganizationCourse) {
        when (course.type) {
            CourseContentType.VIDEO -> {
                if (course.videoUrl.isNotBlank()) {
                    emitEffect(DirectorEffect.OpenUrl(course.videoUrl))
                } else {
                    emitEffect(DirectorEffect.ShowSnackbar("Ссылка на видео недоступна"))
                }
            }
            CourseContentType.TEXT -> {
                _state.update { it.copy(selectedAddedCourse = course, showTextCourseViewer = true) }
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun emitEffect(effect: DirectorEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun snackMsg(lang: AppLanguage, key: String): String = when (key) {
        "student_code" -> when (lang) { AppLanguage.KZ -> "Студент коды көшірілді"; AppLanguage.RU -> "Код студентов скопирован"; else -> "Student code copied" }
        "psych_code"   -> when (lang) { AppLanguage.KZ -> "Психолог коды көшірілді"; AppLanguage.RU -> "Код психолога скопирован"; else -> "Psychologist code copied" }
        "role_changed" -> when (lang) { AppLanguage.KZ -> "Рөл өзгертілді"; AppLanguage.RU -> "Роль изменена"; else -> "Role changed" }
        "user_blocked" -> when (lang) { AppLanguage.KZ -> "Пайдаланушы бұғатталды"; AppLanguage.RU -> "Пользователь заблокирован"; else -> "User blocked" }
        "user_unblocked" -> when (lang) { AppLanguage.KZ -> "Бұғат алынды"; AppLanguage.RU -> "Пользователь разблокирован"; else -> "User unblocked" }
        else -> "Done"
    }

    // ─── Factory ──────────────────────────────────────────────────────────────

    companion object {
        fun factory(orgId: String, uid: String, firestoreService: FirestoreService): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                    DirectorDashboardViewModel(orgId, uid, firestoreService) as T
            }
        }
    }
}
