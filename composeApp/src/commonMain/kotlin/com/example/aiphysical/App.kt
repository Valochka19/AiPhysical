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

        // ── Root safe-area wrapper ────────────────────────────────────────────
        // Two-layer design:
        //   Outer Box  — fills the ENTIRE physical screen (backgrounds bleed to edges,
        //                including under the notch / camera cutout).
        //   Inner Box  — consumes WindowInsets.displayCutout so every screen's
        //                interactive content (text, buttons, cards) is guaranteed
        //                to stay clear of camera notches and punch-holes on
        //                Chinese phones (MIUI, EMUI, ColorOS, OriginOS…).
        //
        // We intentionally use ONLY displayCutout and NOT the full safeDrawing
        // insets here, for two reasons:
        //   1. System bars (status / navigation) are already hidden in immersive
        //      mode; each Scaffold handles them via its own contentWindowInsets.
        //   2. IME (keyboard) insets are already handled per-screen with
        //      Modifier.imePadding() where needed (e.g. LoginScreen).
        // Keeping responsibilities separate prevents any accidental double-padding.
        Box(modifier = Modifier.fillMaxSize()) {                       // ← full bleed
            Box(modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout)       // ← notch guard
            ) {
                // A single AnimatedContent wraps BOTH the loading splash and the
                // main navigation so that the loading→content switch is animated
                // rather than abrupt (prevents the 1-frame "doubled text" artefact
                // on some Android GPU drivers).
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                    },
                    contentKey = { state ->
                        // Key changes only when the meaningful screen changes,
                        // not on every minor state update — avoids spurious transitions.
                        if (state.isRestoringSession) "loading"
                        else state.currentScreen::class.simpleName
                    },
                    label = "app_root_nav"
                ) { state ->
                    if (state.isRestoringSession) {
                        AnimatedBackground(animate = false) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = VioletGlow)
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = state.currentLanguage.pick(
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
                        return@AnimatedContent
                    }

                    when (val screen = state.currentScreen) {
                        is AuthScreen.Login -> LoginScreen(uiState = state, onEvent = authViewModel::onEvent)
                        is AuthScreen.RoleSelection -> RoleSelectionScreen(uiState = state, onEvent = authViewModel::onEvent)
                        is AuthScreen.Registration -> RegistrationScreen(role = screen.role, uiState = state, onEvent = authViewModel::onEvent)

                        // ── Director Dashboard ─────────────────────────────────────
                        is AuthScreen.DirectorDashboard -> DirectorDashboardScreen(
                            orgId = screen.orgId,
                            uid = screen.uid,
                            initialLanguage = state.currentLanguage,
                            onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                            onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                        )

                        // ── Psychologist Dashboard ─────────────────────────────────
                        is AuthScreen.PsychologistDashboard -> PsychologistDashboardScreen(
                            uid = screen.uid,
                            orgId = screen.orgId,
                            fullName = screen.fullName,
                            currentLanguage = state.currentLanguage,
                            onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                            onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                        )

                        // ── Student Dashboard ──────────────────────────────────────
                        is AuthScreen.StudentDashboard -> StudentDashboardScreen(
                            uid = screen.uid,
                            orgId = screen.orgId,
                            currentLanguage = state.currentLanguage,
                            onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                            onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                        )

                        // ── Teacher Dashboard ──────────────────────────────────────
                        is AuthScreen.TeacherDashboard -> TeacherDashboardScreen(
                            uid = screen.uid,
                            orgId = screen.orgId,
                            currentLanguage = state.currentLanguage,
                            onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                            onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                        )

                        // ── Generic Home (fallback for unknown roles) ──────────────
                        is AuthScreen.GenericHome -> GenericHomeScreen(
                            uid = screen.uid,
                            role = screen.role,
                            language = state.currentLanguage,
                            onLanguageChange = { authViewModel.onEvent(AuthEvent.ChangeLanguage(it)) },
                            onLogout = { authViewModel.onEvent(AuthEvent.Logout) }
                        )
                    }
                }
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
