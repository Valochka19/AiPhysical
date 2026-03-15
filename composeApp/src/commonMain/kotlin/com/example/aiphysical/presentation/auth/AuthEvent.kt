package com.example.aiphysical.presentation.auth

sealed class AuthEvent {
    data class Login(val email: String, val password: String) : AuthEvent()

    data class RegisterDirector(
        val orgName: String,
        val fullName: String,
        val email: String,
        val password: String
    ) : AuthEvent()

    data class RegisterPsychologist(
        val fullName: String,
        val inviteCode: String,
        val email: String,
        val password: String
    ) : AuthEvent()

    data class RegisterStudent(
        val fullName: String,
        val orgCode: String,
        val ageGroup: AgeGroup,
        val email: String,
        val password: String
    ) : AuthEvent()

    data class SelectRole(val role: UserRole) : AuthEvent()
    object NavigateToRoleSelection : AuthEvent()
    object NavigateToLogin : AuthEvent()
    object DismissError : AuthEvent()
    object Logout : AuthEvent()
    data class ChangeLanguage(val language: AppLanguage) : AuthEvent()
}

