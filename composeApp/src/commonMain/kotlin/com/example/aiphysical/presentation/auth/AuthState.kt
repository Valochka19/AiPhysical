package com.example.aiphysical.presentation.auth

import com.example.aiphysical.data.model.UserProfile

enum class UserRole { DIRECTOR, PSYCHOLOGIST, STUDENT }

enum class AgeGroup {
    JUNIOR, MIDDLE, SENIOR, TEACHER;

    fun display(language: AppLanguage): String = when (language) {
        AppLanguage.KZ -> when (this) {
            JUNIOR -> "Кіші мектеп"
            MIDDLE -> "Орта мектеп"
            SENIOR -> "Жоғары мектеп"
            TEACHER -> "Мұғалім"
        }
        AppLanguage.RU -> when (this) {
            JUNIOR -> "Младшая школа"
            MIDDLE -> "Средняя школа"
            SENIOR -> "Старшая школа"
            TEACHER -> "Преподаватель"
        }
        AppLanguage.EN -> when (this) {
            JUNIOR -> "Junior School"
            MIDDLE -> "Middle School"
            SENIOR -> "Senior High School"
            TEACHER -> "Teacher"
        }
    }
}

fun AgeGroup.persistedRole(): String = if (this == AgeGroup.TEACHER) "teacher" else "user"

fun AgeGroup.persistedAgeGroup(): String = if (this == AgeGroup.TEACHER) "" else name

enum class AppLanguage(val displayName: String, val code: String) {
    KZ("ҚАЗ", "kz"),
    RU("РУС", "ru"),
    EN("ENG", "en")
}

fun AppLanguage.pick(ru: String, en: String, kz: String): String = when (this) {
    AppLanguage.RU -> ru
    AppLanguage.EN -> en
    AppLanguage.KZ -> kz
}

fun String.displayAgeGroup(language: AppLanguage): String =
    AgeGroup.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }?.display(language) ?: this

fun AppLanguage.replyLanguageName(): String = pick(
    ru = "русском",
    en = "English",
    kz = "қазақ тілінде"
)

sealed class AuthScreen {
    object Login : AuthScreen()
    object RoleSelection : AuthScreen()
    data class Registration(val role: UserRole) : AuthScreen()
    /** Director landing – replaces generic Home for director role */
    data class DirectorDashboard(val uid: String, val orgId: String) : AuthScreen()
    /** Psychologist dedicated dashboard */
    data class PsychologistDashboard(val uid: String, val orgId: String, val fullName: String) : AuthScreen()
    /** Student dedicated dashboard */
    data class StudentDashboard(val uid: String, val orgId: String) : AuthScreen()
    /** Teacher dedicated dashboard */
    data class TeacherDashboard(val uid: String, val orgId: String) : AuthScreen()
    /** Placeholder home for unknown roles */
    data class GenericHome(val uid: String, val role: String) : AuthScreen()
}

fun routeByRole(uid: String, profile: UserProfile): AuthScreen = when (profile.role.lowercase()) {
    "director" -> AuthScreen.DirectorDashboard(uid = uid, orgId = profile.orgId)
    "psychologist" -> AuthScreen.PsychologistDashboard(uid = uid, orgId = profile.orgId, fullName = profile.fullName)
    "user" -> AuthScreen.StudentDashboard(uid = uid, orgId = profile.orgId)
    "teacher" -> AuthScreen.TeacherDashboard(uid = uid, orgId = profile.orgId)
    else -> AuthScreen.GenericHome(uid = uid, role = profile.role)
}

data class AuthUiState(
    val currentScreen: AuthScreen = AuthScreen.Login,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentLanguage: AppLanguage = AppLanguage.RU,
    val isLoggedIn: Boolean = false,
    val isRestoringSession: Boolean = true
)
