package com.example.aiphysical.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.auth.AuthEvent
import com.example.aiphysical.presentation.auth.AuthUiState
import com.example.aiphysical.ui.components.*
import com.example.aiphysical.ui.theme.*

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
) {
    val strings = getStrings(uiState.currentLanguage)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AnimatedBackground(animate = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            // ── Top bar: language switcher ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageSwitcher(
                    currentLanguage = uiState.currentLanguage,
                    onLanguageChange = { onEvent(AuthEvent.ChangeLanguage(it)) }
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── App Logo / Title ────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(VioletPrimary.copy(alpha = 0.8f), AccentPink.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧠", fontSize = 38.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = strings.appTitle,
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = strings.appSubtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── Error Banner ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it }
            ) {
                Column {
                    uiState.errorMessage?.let {
                        ErrorBanner(message = it, onDismiss = { onEvent(AuthEvent.DismissError) })
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Login Form Card ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24.dp, bgAlpha = 0.10f, borderAlpha = 0.22f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = strings.login,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                GlassTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = strings.email,
                    keyboardType = KeyboardType.Email,
                    leadingEmoji = "✉️"
                )

                GlassTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = strings.password,
                    isPassword = true,
                    leadingEmoji = "🔒"
                )

                GlassPrimaryButton(
                    text = strings.login,
                    isLoading = uiState.isLoading,
                    onClick = { onEvent(AuthEvent.Login(email.trim(), password)) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Navigate to Register ────────────────────────────────────────
            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { onEvent(AuthEvent.NavigateToRoleSelection) }
            ) {
                Text(
                    text = strings.noAccount,
                    color = VioletGlow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Decorative bottom text ──────────────────────────────────────
            Text(
                text = "© AI Physical 2026",
                color = TextHint,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

