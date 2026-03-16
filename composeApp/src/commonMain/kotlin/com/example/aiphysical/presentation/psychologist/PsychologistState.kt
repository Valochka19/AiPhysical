package com.example.aiphysical.presentation.psychologist

import com.example.aiphysical.data.model.TestResult
import com.example.aiphysical.data.model.UserProfile

enum class PsychologistTab { Overview, Database, Interventions, Library }
enum class PsychologistScreen { Dashboard, StudentDetail }

data class PsychologistHomeState(
    val isLoading: Boolean = true,
    val psychologistName: String = "",
    val orgId: String = "",
    val psychologistId: String = "",

    // Student lists
    val students: List<UserProfile> = emptyList(),
    val filteredStudents: List<UserProfile> = emptyList(),
    val criticalStudents: List<UserProfile> = emptyList(),
    val stressStudents: List<UserProfile> = emptyList(),
    /** Students who passed ≥1 test but have no psychologist comment yet */
    val pendingRecommendations: List<UserProfile> = emptyList(),
    val searchQuery: String = "",

    // Navigation
    val currentScreen: PsychologistScreen = PsychologistScreen.Dashboard,
    val selectedTab: PsychologistTab = PsychologistTab.Overview,

    // Student detail
    val selectedStudent: UserProfile? = null,
    val selectedStudentTestHistory: List<TestResult> = emptyList(),
    val isLoadingDetail: Boolean = false,

    // Recommendation form state
    val showRecommendationSheet: Boolean = false,
    val recommendationTarget: UserProfile? = null,
    val recommendationComment: String = "",
    val recommendationCourseId: String = "",
    val recommendationCourseName: String = "",
    val recommendationPriority: String = "MEDIUM",   // "LOW" | "MEDIUM" | "HIGH"
    val isSendingRecommendation: Boolean = false,

    // Analytics (org-wide averages)
    val avgBurnout: Float = 0f,
    val avgStress: Float = 0f,
    val avgAnxiety: Float = 0f,
    val avgEmotion: Float = 50f,
    val avgMotivation: Float = 50f,
    /** "good" | "warning" | "critical" | "unknown" */
    val psychClimate: String = "unknown",

    // Feedback
    val snackbarMessage: String? = null,
    val errorMessage: String? = null
)

