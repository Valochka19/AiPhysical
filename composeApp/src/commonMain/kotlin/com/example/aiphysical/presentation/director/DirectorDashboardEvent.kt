package com.example.aiphysical.presentation.director

import com.example.aiphysical.data.model.BaseCourseCatalogItem
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.student.StudentTestType

sealed class DirectorEvent {
    object LoadDashboard : DirectorEvent()
    object RefreshData : DirectorEvent()
    data class SearchMembers(val query: String) : DirectorEvent()
    data class SelectMember(val member: UserProfile) : DirectorEvent()
    object BackToDashboard : DirectorEvent()
    data class OpenContactDialog(val member: UserProfile) : DirectorEvent()
    object DismissContactDialog : DirectorEvent()
    object CopyStudentCode : DirectorEvent()
    object CopyPsychCode : DirectorEvent()
    object ShareStudentCode : DirectorEvent()
    object SharePsychCode : DirectorEvent()
    data class ChangeLanguage(val language: AppLanguage) : DirectorEvent()
    object DismissSnackbar : DirectorEvent()
    object Logout : DirectorEvent()
    data class NavigateToTab(val tab: DirectorTab) : DirectorEvent()
    object LoadAiInsight : DirectorEvent()
    object OpenInviteSheet : DirectorEvent()
    object DismissInviteSheet : DirectorEvent()
    data class OpenRoleChangeSheet(val member: UserProfile) : DirectorEvent()
    object DismissRoleChangeSheet : DirectorEvent()
    data class ChangeUserRole(val uid: String, val newRole: String) : DirectorEvent()
    data class ToggleUserBlock(val uid: String) : DirectorEvent()
    data class SetAnalyticsFilter(val filter: String) : DirectorEvent()
    // ── Added courses ─────────────────────────────────────────────────────────
    data class OpenBaseCourse(val course: BaseCourseCatalogItem) : DirectorEvent()
    data class OpenBaseCourseCompletion(val courseId: String) : DirectorEvent()
    object CloseBaseCourseCompletionDialog : DirectorEvent()
    data class OpenTestStats(val testType: StudentTestType) : DirectorEvent()
    object CloseTestStatsDialog : DirectorEvent()
    data class OpenAddedCourse(val course: OrganizationCourse) : DirectorEvent()
    object CloseTextCourseViewer : DirectorEvent()
}
