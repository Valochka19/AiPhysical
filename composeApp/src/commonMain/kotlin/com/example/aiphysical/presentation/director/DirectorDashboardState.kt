package com.example.aiphysical.presentation.director

import com.example.aiphysical.data.model.*
import com.example.aiphysical.presentation.auth.AppLanguage

enum class DirectorPanelScreen { Dashboard, MemberDetail }

enum class DirectorTab { Dashboard, Analytics, Management, Content }

data class DirectorDashboardState(
    val isLoading: Boolean = true,
    val organization: Organization? = null,
    val members: List<UserProfile> = emptyList(),
    val filteredMembers: List<UserProfile> = emptyList(),
    val searchQuery: String = "",
    val kpiData: KpiData = KpiData(),
    val trendPoints: List<TrendPoint> = emptyList(),
    val criticalMembers: List<UserProfile> = emptyList(),
    val psychologists: List<UserProfile> = emptyList(),
    // Navigation
    val currentScreen: DirectorPanelScreen = DirectorPanelScreen.Dashboard,
    val selectedTab: DirectorTab = DirectorTab.Dashboard,
    // Member detail
    val selectedMember: UserProfile? = null,
    val selectedMemberTestHistory: List<TestResult> = emptyList(),
    val selectedMemberCourseProgress: List<CourseProgress> = emptyList(),
    val isLoadingDetail: Boolean = false,
    // Dialogs / Sheets
    val showContactDialog: Boolean = false,
    val contactTargetMember: UserProfile? = null,
    val showInviteSheet: Boolean = false,
    val showRoleChangeSheet: Boolean = false,
    val roleChangeTarget: UserProfile? = null,
    // AI Insight
    val aiInsightText: String = "",
    val isAiLoading: Boolean = false,
    // Analytics filter: "ALL" | "JUNIOR" | "MIDDLE" | "SENIOR" | "STAFF"
    val analyticsFilter: String = "ALL",
    // Feedback
    val snackbarMessage: String? = null,
    val currentLanguage: AppLanguage = AppLanguage.RU,
    // ── Added (org-level) courses ─────────────────────────────────────────────
    val addedCourses: List<OrganizationCourse> = emptyList(),
    val isLoadingAddedCourses: Boolean = false,
    val selectedAddedCourse: OrganizationCourse? = null,
    val showTextCourseViewer: Boolean = false,
)
