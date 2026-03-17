package com.example.aiphysical.presentation.student

import com.example.aiphysical.data.model.StudentTestUiState
import com.example.aiphysical.data.model.ChatMessage
import com.example.aiphysical.data.model.CourseProgress
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.data.model.TestResult
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.auth.AppLanguage

// ── Navigation tabs ───────────────────────────────────────────────────────────
enum class StudentTab { Home, Help, Courses, Chat, Profile }

// ── 5 test types (match DB IDs) ───────────────────────────────────────────────
enum class StudentTestType(
    val testId: String,
    val emoji: String,
    val label: String,
    /** ARGB hex pairs for gradient: start to end */
    val colorStartHex: Long,
    val colorEndHex: Long
) {
    BURNOUT(   "burnout",    "🔥", "Выгорание",   0xFFFF4757, 0xFFFF8C00),
    STRESS(    "stress",     "⚡", "Стресс",       0xFFFFD32A, 0xFFFF6B35),
    EMOTION(   "emotion",    "😊", "Состояние",   0xFF00CED1, 0xFF0052D4),
    MOTIVATION("motivation", "🚀", "Мотивация",   0xFF4FD18A, 0xFF00CED1),
    ANXIETY(   "anxiety",    "☁️", "Тревожность", 0xFF9D5FF5, 0xFFE040FB)
}

// ── Main UI state ─────────────────────────────────────────────────────────────
data class StudentUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val testHistory: List<TestResult> = emptyList(),
    val courseProgress: List<CourseProgress> = emptyList(),
    val selectedTab: StudentTab = StudentTab.Home,
    /** 0–100, computed from all five metric scores */
    val overallScore: Float = 0f,
    /** Which tests have been completed (by testId) */
    val completedTestIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val currentLanguage: AppLanguage = AppLanguage.RU,
    // ── Added (org-level) courses ─────────────────────────────────────────────
    val addedCourses: List<OrganizationCourse> = emptyList(),
    val isLoadingAddedCourses: Boolean = false,
    val selectedAddedCourse: OrganizationCourse? = null,
    val showAddedCoursesViewer: Boolean = false,
    val showTextCourseViewer: Boolean = false,
    // ── AI Chat ───────────────────────────────────────────────────────────────
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatInput: String = "",
    val isChatLoading: Boolean = false,
    val chatError: String? = null,
    // ── Active Student Test ───────────────────────────────────────────────────
    /** Non-null while any student test overlay is open */
    val activeTestState: StudentTestUiState? = null,
)
