package com.example.aiphysical.presentation.director

import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.auth.AppLanguage

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
    // ── New tab navigation ────────────────────────────────────────────────────
    data class NavigateToTab(val tab: DirectorTab) : DirectorEvent()
    // ── AI Insight ─────────────────────────────────────────────────────────────
    object LoadAiInsight : DirectorEvent()
    // ── Invite sheet ──────────────────────────────────────────────────────────
    object OpenInviteSheet : DirectorEvent()
    object DismissInviteSheet : DirectorEvent()
    // ── Role change ───────────────────────────────────────────────────────────
    data class OpenRoleChangeSheet(val member: UserProfile) : DirectorEvent()
    object DismissRoleChangeSheet : DirectorEvent()
    data class ChangeUserRole(val uid: String, val newRole: String) : DirectorEvent()
    // ── Block / unblock ───────────────────────────────────────────────────────
    data class ToggleUserBlock(val uid: String) : DirectorEvent()
    // ── Analytics filter ──────────────────────────────────────────────────────
    data class SetAnalyticsFilter(val filter: String) : DirectorEvent()
}
