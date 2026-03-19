package com.example.aiphysical.presentation.psychologist

import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.OrganizationCustomTest
import com.example.aiphysical.data.model.OrganizationCustomTestQuestion
import com.example.aiphysical.data.model.OrganizationTestStats
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.data.model.TestResult
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.auth.AppLanguage

enum class PsychologistTab { Overview, Database, Interventions, Library }
enum class PsychologistScreen { Dashboard, StudentDetail, TestBuilder }

/** Represents a single entry in the home-screen test results feed. */
data class RecentTestFeedItem(
    val studentId: String = "",
    val studentName: String = "",
    val studentStatus: String = "unknown",  // "normal" | "stress" | "critical"
    val stressScore: Float = 0f,
    val burnoutScore: Float = 0f,
    val anxietyScore: Float = 0f,
    val emotionScore: Float = 50f,
    val motivationScore: Float = 50f
)

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

    // Analytics tab age-group filter: "ALL" | "JUNIOR" | "MIDDLE" | "SENIOR"
    val analyticsFilter: String = "ALL",

    // Test results feed (derived from students, shown on home screen)
    val recentTestFeed: List<RecentTestFeedItem> = emptyList(),
    val showTestResultSheet: Boolean = false,
    val selectedTestFeedItem: RecentTestFeedItem? = null,
    val testStats: List<OrganizationTestStats> = emptyList(),
    val isLoadingTestStats: Boolean = false,
    val selectedTestStats: OrganizationTestStats? = null,
    val showTestStatsDialog: Boolean = false,

    // Feedback
    val snackbarMessage: String? = null,
    val errorMessage: String? = null,

    // Language
    val currentLanguage: AppLanguage = AppLanguage.RU,

    // ── Added (org-level) courses ─────────────────────────────────────────────
    val addedCourses: List<OrganizationCourse> = emptyList(),
    val isLoadingAddedCourses: Boolean = false,

    // ── Add course form ───────────────────────────────────────────────────────
    val showAddCourseSheet: Boolean = false,
    val newCourseTitle: String = "",
    val newCourseDescription: String = "",
    val newCourseType: CourseContentType = CourseContentType.TEXT,
    val newCourseTextContent: String = "",
    val newCourseVideoUrl: String = "",
    val isPublishingCourse: Boolean = false,

    // ── Added courses viewer ──────────────────────────────────────────────────
    val showAddedCoursesViewer: Boolean = false,
    val selectedAddedCourse: OrganizationCourse? = null,
    val showTextCourseViewer: Boolean = false,

    // ── Organization custom tests ─────────────────────────────────────────────
    val customTests: List<OrganizationCustomTest> = emptyList(),
    val isLoadingCustomTests: Boolean = false,

    // ── Custom test builder ───────────────────────────────────────────────────
    val currentTestDraftTitle: String = "",
    val draftQuestions: List<OrganizationCustomTestQuestion> = emptyList(),
    val currentDraftQuestionText: String = "",
    val currentDraftOption1: String = "",
    val currentDraftOption2: String = "",
    val currentDraftOption3: String = "",
    val currentDraftQuestionIndex: Int = 1,
    val isPublishingCustomTest: Boolean = false,
    val showDiscardCustomTestDialog: Boolean = false,
)

