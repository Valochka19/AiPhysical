package com.example.aiphysical

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiphysical.presentation.auth.*
import com.example.aiphysical.ui.components.AnimatedBackground
import com.example.aiphysical.ui.components.GlassPrimaryButton
import com.example.aiphysical.ui.components.LanguageSwitcher
import com.example.aiphysical.ui.screens.*
import com.example.aiphysical.ui.screens.director.DirectorDashboardScreen
import com.example.aiphysical.ui.screens.psychologist.PsychologistDashboardScreen
import com.example.aiphysical.ui.screens.student.StudentDashboardScreen
import com.example.aiphysical.ui.screens.teacher.TeacherDashboardScreen
import com.example.aiphysical.ui.theme.*
import com.example.aiphysical.util.createFirebaseAuthService
import com.example.aiphysical.util.createFirestoreService

@Composable
fun App() {
    AIPhysicalTheme {
        val authViewModel: AuthViewModel = viewModel {
            AuthViewModel(
                authService = createFirebaseAuthService(),
                firestoreService = createFirestoreService()
            )
        }
        val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
        val strings = getStrings(uiState.currentLanguage)

        if (uiState.isRestoringSession) {
            AnimatedBackground(animate = false) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = VioletGlow)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = uiState.currentLanguage.pick(
                            ru = "Восстанавливаем сессию...",
                            en = "Restoring session...",
                            kz = "Сессия қалпына келтірілуде..."
                        ),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            return@AIPhysicalTheme
        }

        AnimatedContent(
            targetState = uiState.currentScreen,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
            },
            label = "auth_nav"
        ) { screen ->
            when (screen) {
                is AuthScreen.Login -> LoginScreen(uiState = uiState, onEvent = authViewModel::onEvent)
                is AuthScreen.RoleSelection -> RoleSelectionScreen(uiState = uiState, onEvent = authViewModel::onEvent)
                is AuthScreen.Registration -> RegistrationScreen(role = screen.role, uiState = uiState, onEvent = authViewModel::onEvent)

                // ── Director Dashboard ─────────────────────────────────────────────────
                // Back button is disabled here — cannot navigate back to registration
                is AuthScreen.DirectorDashboard -> DirectorDashboardScreen(
                    orgId = screen.orgId,
                    uid = screen.uid,
                    initialLanguage = uiState.currentLanguage,
                    onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                    onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                )

                // ── Psychologist Dashboard ─────────────────────────────────────────────
                is AuthScreen.PsychologistDashboard -> PsychologistDashboardScreen(
                    uid = screen.uid,
                    orgId = screen.orgId,
                    fullName = screen.fullName,
                    currentLanguage = uiState.currentLanguage,
                    onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                    onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                )

                // ── Student Dashboard ─────────────────────────────────────────────────
                is AuthScreen.StudentDashboard -> StudentDashboardScreen(
                    uid = screen.uid,
                    orgId = screen.orgId,
                    currentLanguage = uiState.currentLanguage,
                    onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                    onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                )

                // ── Teacher Dashboard ─────────────────────────────────────────────────
                is AuthScreen.TeacherDashboard -> TeacherDashboardScreen(
                    uid = screen.uid,
                    orgId = screen.orgId,
                    currentLanguage = uiState.currentLanguage,
                    onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                    onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                )

                // ── Generic Home (fallback for unknown roles) ─────────────────────────
                is AuthScreen.GenericHome -> GenericHomeScreen(
                    uid = screen.uid,
                    role = screen.role,
                    language = uiState.currentLanguage,
                    onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                    onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                )
            }
        }
    }
}

// ─── Generic Home (Psychologist / Student) ───────────────────────────────────

@Composable
private fun GenericHomeScreen(
    uid: String,
    role: String,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
) {
    val strings = getStrings(language)
    val (emoji, roleColor) = when (role) {
        "psychologist" -> Pair("🧠", AccentCyan)
        "user" -> Pair("👤", VioletLight)
        "teacher" -> Pair("🧑‍🏫", AlertOrange)
        else -> Pair("✅", SuccessColor)
    }

    AnimatedBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                LanguageSwitcher(currentLanguage = language, onLanguageChange = onLanguageChange)
            }
            Spacer(Modifier.weight(1f))
            Text(emoji, fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                strings.genericHomeTitle,
                color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Role: $role",
                color = roleColor, fontSize = 14.sp, fontWeight = FontWeight.Medium
            )
            Text("UID: ${uid.take(12)}…", color = TextHint, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            GlassPrimaryButton(text = "🚪 ${strings.logoutBtn}", onClick = onLogout)
            Spacer(Modifier.height(24.dp))
        }
    }
}
