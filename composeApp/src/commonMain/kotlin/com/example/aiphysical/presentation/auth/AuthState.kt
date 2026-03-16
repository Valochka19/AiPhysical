package com.example.aiphysical.presentation.auth

enum class UserRole { DIRECTOR, PSYCHOLOGIST, STUDENT }

enum class AgeGroup {
    JUNIOR, MIDDLE, SENIOR;

    fun display(language: AppLanguage): String = when (language) {
        AppLanguage.KZ -> when (this) {
            JUNIOR -> "Кіші мектеп"
            MIDDLE -> "Орта мектеп"
            SENIOR -> "Жоғары мектеп"
        }
        AppLanguage.RU -> when (this) {
            JUNIOR -> "Младшая школа"
            MIDDLE -> "Средняя школа"
            SENIOR -> "Старшая школа"
        }
        AppLanguage.EN -> when (this) {
            JUNIOR -> "Junior School"
            MIDDLE -> "Middle School"
            SENIOR -> "Senior High School"
        }
    }
}

enum class AppLanguage(val displayName: String, val code: String) {
    KZ("ҚАЗ", "kz"),
    RU("РУС", "ru"),
    EN("ENG", "en")
}

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
    /** Placeholder home for unknown roles */
    data class GenericHome(val uid: String, val role: String) : AuthScreen()
}

data class AuthUiState(
    val currentScreen: AuthScreen = AuthScreen.Login,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentLanguage: AppLanguage = AppLanguage.RU,
    val isLoggedIn: Boolean = false
)
