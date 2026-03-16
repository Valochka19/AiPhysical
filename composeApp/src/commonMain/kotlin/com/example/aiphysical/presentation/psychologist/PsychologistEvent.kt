package com.example.aiphysical.presentation.psychologist

import com.example.aiphysical.data.model.UserProfile

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
}

