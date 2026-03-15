package com.example.aiphysical.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiphysical.data.model.Organization
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.data.service.AuthResult
import com.example.aiphysical.data.service.FirebaseAuthService
import com.example.aiphysical.data.service.FirestoreResult
import com.example.aiphysical.data.service.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.Login -> handleLogin(event)
            is AuthEvent.RegisterDirector -> handleRegisterDirector(event)
            is AuthEvent.RegisterPsychologist -> handleRegisterPsychologist(event)
            is AuthEvent.RegisterStudent -> handleRegisterStudent(event)
            is AuthEvent.SelectRole -> _uiState.update { it.copy(currentScreen = AuthScreen.Registration(event.role)) }
            AuthEvent.NavigateToRoleSelection -> _uiState.update { it.copy(currentScreen = AuthScreen.RoleSelection, errorMessage = null) }
            AuthEvent.NavigateToLogin -> _uiState.update { it.copy(currentScreen = AuthScreen.Login, errorMessage = null) }
            AuthEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            AuthEvent.Logout -> {
                viewModelScope.launch { authService.signOut() }
                _uiState.update { AuthUiState(currentLanguage = it.currentLanguage) }
            }
            is AuthEvent.ChangeLanguage -> _uiState.update { it.copy(currentLanguage = event.language) }
        }
    }

    // ─── Login: fetch profile → route by role ─────────────────────────────────

    private fun handleLogin(event: AuthEvent.Login) {
        val lang = _uiState.value.currentLanguage
        if (event.email.isBlank() || event.password.isBlank()) { showError(emptyFieldsMsg(lang)); return }
        if (!event.email.contains("@")) { showError(badEmailMsg(lang)); return }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val authResult = authService.signIn(event.email, event.password)) {
                is AuthResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = authResult.message) }
                is AuthResult.Success -> {
                    val uid = authResult.uid
                    val nextScreen = when (val profileResult = firestoreService.getUserProfile(uid)) {
                        is FirestoreResult.UserProfileSuccess -> routeByRole(uid, profileResult.profile)
                        else -> AuthScreen.GenericHome(uid = uid, role = "unknown")
                    }
                    _uiState.update { it.copy(isLoading = false, currentScreen = nextScreen, isLoggedIn = true) }
                }
            }
        }
    }

    private fun routeByRole(uid: String, profile: UserProfile): AuthScreen =
        if (profile.role == "director") AuthScreen.DirectorDashboard(uid = uid, orgId = profile.orgId)
        else AuthScreen.GenericHome(uid = uid, role = profile.role)

    // ─── Director registration ────────────────────────────────────────────────

    private fun handleRegisterDirector(event: AuthEvent.RegisterDirector) {
        val lang = _uiState.value.currentLanguage
        if (event.orgName.isBlank() || event.fullName.isBlank() || event.email.isBlank() || event.password.isBlank()) {
            showError(emptyFieldsMsg(lang)); return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val authResult = authService.signUp(event.email, event.password)) {
                is AuthResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = authResult.message) }
                is AuthResult.Success -> {
                    val uid = authResult.uid
                    val orgId = generateCode(20)
                    val org = Organization(
                        id = orgId, name = event.orgName, directorId = uid,
                        inviteCodeStudent = generateCode(8), inviteCodePsych = generateCode(8)
                    )
                    when (val orgRes = firestoreService.createOrganization(org)) {
                        is FirestoreResult.Failure -> { authService.signOut(); _uiState.update { it.copy(isLoading = false, errorMessage = orgRes.message) } }
                        else -> {
                            val profile = UserProfile(uid = uid, fullName = event.fullName, email = event.email, role = "director", orgId = orgId)
                            when (val pRes = firestoreService.createUserProfile(profile)) {
                                is FirestoreResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = pRes.message) }
                                // 🔑 Navigate directly to DirectorDashboard — no back to registration
                                else -> _uiState.update { it.copy(isLoading = false, currentScreen = AuthScreen.DirectorDashboard(uid = uid, orgId = orgId), isLoggedIn = true) }
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── Psychologist registration ────────────────────────────────────────────
    //
    // Полный flow регистрации психолога:
    //
    //  [1] Валидация полей (не пустые)
    //  [2] Firestore: ищем organizations WHERE inviteCodePsych == inviteCode
    //         → NotFound  → показываем ошибку "неверный код"
    //         → OrgSuccess → берём org.id (это и есть organizationId)
    //  [3] Firebase Auth: createUserWithEmailAndPassword(email, password)
    //         → возвращает uid нового пользователя
    //  [4] Firestore: создаём документ в коллекции users
    //         {uid, fullName, email, role: "psychologist", orgId: org.id, ...}
    //         orgId обязателен — по нему Директор фильтрует участников
    //  [5] Навигация на GenericHome с role="psychologist"
    //
    private fun handleRegisterPsychologist(event: AuthEvent.RegisterPsychologist) {
        val lang = _uiState.value.currentLanguage

        // [1] Валидация
        if (event.fullName.isBlank() || event.inviteCode.isBlank() ||
            event.email.isBlank() || event.password.isBlank()
        ) { showError(emptyFieldsMsg(lang)); return }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {

            // [2] Ищем организацию по коду приглашения психолога
            when (val orgRes = firestoreService.findOrgByPsychCode(
                event.inviteCode.trim().uppercase()
            )) {
                is FirestoreResult.NotFound ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = invalidCodeMsg(lang, isPsych = true)) }

                is FirestoreResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = orgRes.message) }

                is FirestoreResult.OrgSuccess -> {
                    val organizationId = orgRes.org.id   // ← docId найденной организации

                    // [3] Регистрируем пользователя в Firebase Auth
                    when (val authResult = authService.signUp(event.email, event.password)) {
                        is AuthResult.Failure ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = authResult.message) }

                        is AuthResult.Success -> {
                            val uid = authResult.uid

                            // [4] Создаём профиль — organizationId записывается в документ
                            val profile = UserProfile(
                                uid      = uid,
                                fullName = event.fullName,
                                email    = event.email,
                                role     = "psychologist",
                                orgId    = organizationId   // ← ключевое поле
                            )
                            when (val pRes = firestoreService.createUserProfile(profile)) {
                                is FirestoreResult.Failure ->
                                    _uiState.update { it.copy(isLoading = false, errorMessage = pRes.message) }

                                // [5] Успех → навигация на HomeScreen психолога
                                else -> _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        currentScreen = AuthScreen.GenericHome(uid = uid, role = "psychologist"),
                                        isLoggedIn = true
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // ─── Student registration ─────────────────────────────────────────────────

    private fun handleRegisterStudent(event: AuthEvent.RegisterStudent) {
        val lang = _uiState.value.currentLanguage
        if (event.fullName.isBlank() || event.orgCode.isBlank() || event.email.isBlank() || event.password.isBlank()) {
            showError(emptyFieldsMsg(lang)); return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val orgRes = firestoreService.findOrgByStudentCode(event.orgCode.trim().uppercase())) {
                is FirestoreResult.NotFound -> _uiState.update { it.copy(isLoading = false, errorMessage = invalidCodeMsg(lang, false)) }
                is FirestoreResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = orgRes.message) }
                is FirestoreResult.OrgSuccess -> {
                    when (val authResult = authService.signUp(event.email, event.password)) {
                        is AuthResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = authResult.message) }
                        is AuthResult.Success -> {
                            val profile = UserProfile(uid = authResult.uid, fullName = event.fullName, email = event.email, role = "user", orgId = orgRes.org.id, ageGroup = event.ageGroup.name)
                            when (val pRes = firestoreService.createUserProfile(profile)) {
                                is FirestoreResult.Failure -> _uiState.update { it.copy(isLoading = false, errorMessage = pRes.message) }
                                else -> _uiState.update { it.copy(isLoading = false, currentScreen = AuthScreen.GenericHome(uid = authResult.uid, role = "user"), isLoggedIn = true) }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun showError(msg: String) = _uiState.update { it.copy(errorMessage = msg) }

    private fun emptyFieldsMsg(lang: AppLanguage) = when (lang) {
        AppLanguage.KZ -> "Барлық өрістерді толтырыңыз"
        AppLanguage.RU -> "Заполните все поля"
        AppLanguage.EN -> "Fill in all fields"
    }

    private fun badEmailMsg(lang: AppLanguage) = when (lang) {
        AppLanguage.KZ -> "Email форматы жарамсыз"
        AppLanguage.RU -> "Неверный формат email"
        AppLanguage.EN -> "Invalid email format"
    }

    private fun invalidCodeMsg(lang: AppLanguage, isPsych: Boolean) = when (lang) {
        AppLanguage.KZ -> if (isPsych) "Жарамсыз шақыру коды" else "Жарамсыз ұйым коды"
        AppLanguage.RU -> if (isPsych) "Недействительный код приглашения" else "Недействительный код организации"
        AppLanguage.EN -> if (isPsych) "Invalid invite code" else "Invalid organization code"
    }

    private fun generateCode(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }
}
