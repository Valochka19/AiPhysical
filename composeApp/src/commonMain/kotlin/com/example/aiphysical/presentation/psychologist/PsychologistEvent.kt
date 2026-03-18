package com.example.aiphysical.presentation.psychologist

import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.BaseCourseCatalogItem
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.student.StudentTestType

sealed class PsychologistEvent {
    object LoadData : PsychologistEvent()
    data class SearchStudents(val query: String) : PsychologistEvent()
    data class SelectStudent(val student: UserProfile) : PsychologistEvent()
    object BackToDashboard : PsychologistEvent()
    data class NavigateToTab(val tab: PsychologistTab) : PsychologistEvent()

    // Recommendation flow
    data class OpenRecommendationSheet(val student: UserProfile) : PsychologistEvent()
    object DismissRecommendationSheet : PsychologistEvent()
    data class UpdateRecommendationComment(val text: String) : PsychologistEvent()
    data class SelectRecommendationCourse(val courseId: String, val courseName: String) : PsychologistEvent()
    data class SetRecommendationPriority(val priority: String) : PsychologistEvent()
    object SendRecommendation : PsychologistEvent()

    // UI feedback
    object DismissSnackbar : PsychologistEvent()
    object Logout : PsychologistEvent()

    // Test result feed sheet
    data class ViewTestResult(val item: RecentTestFeedItem) : PsychologistEvent()
    object DismissTestResultSheet : PsychologistEvent()

    // Analytics tab filter
    data class SetAnalyticsFilter(val filter: String) : PsychologistEvent()

    // Language
    data class ChangeLanguage(val language: AppLanguage) : PsychologistEvent()

    // ── Add course form ───────────────────────────────────────────────────────
    object OpenAddCourseSheet : PsychologistEvent()
    object CloseAddCourseSheet : PsychologistEvent()
    data class UpdateNewCourseTitle(val value: String) : PsychologistEvent()
    data class UpdateNewCourseDescription(val value: String) : PsychologistEvent()
    data class UpdateNewCourseType(val type: CourseContentType) : PsychologistEvent()
    data class UpdateNewCourseTextContent(val value: String) : PsychologistEvent()
    data class UpdateNewCourseVideoUrl(val value: String) : PsychologistEvent()
    object PublishCourse : PsychologistEvent()

    // ── Added courses viewer ──────────────────────────────────────────────────
    object OpenAddedCourses : PsychologistEvent()
    object CloseAddedCourses : PsychologistEvent()
    data class OpenBaseCourse(val course: BaseCourseCatalogItem) : PsychologistEvent()
    data class OpenAddedCourse(val course: OrganizationCourse) : PsychologistEvent()
    object CloseSelectedAddedCourse : PsychologistEvent()
    data class DeleteAddedCourse(val courseId: String) : PsychologistEvent()
    object CloseTextCourseViewer : PsychologistEvent()
    data class OpenTestStats(val testType: StudentTestType) : PsychologistEvent()
    object CloseTestStatsDialog : PsychologistEvent()
}
