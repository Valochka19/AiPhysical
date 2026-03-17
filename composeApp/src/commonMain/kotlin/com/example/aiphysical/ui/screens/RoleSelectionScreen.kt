package com.example.aiphysical.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.auth.AuthEvent
import com.example.aiphysical.presentation.auth.AuthUiState
import com.example.aiphysical.presentation.auth.UserRole
import com.example.aiphysical.ui.components.AnimatedBackground
import com.example.aiphysical.ui.components.LanguageSwitcher
import com.example.aiphysical.ui.theme.*

@Composable
fun RoleSelectionScreen(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
) {
    val strings = getStrings(uiState.currentLanguage)

    AnimatedBackground(animate = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            // ── Top bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onEvent(AuthEvent.NavigateToLogin) }) {
                    Text("← ${strings.back}", color = TextSecondary, fontSize = 14.sp)
                }
                LanguageSwitcher(
                    currentLanguage = uiState.currentLanguage,
                    onLanguageChange = { onEvent(AuthEvent.ChangeLanguage(it)) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Title ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("👋", fontSize = 42.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = strings.chooseRole,
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = strings.register,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Role Cards ──────────────────────────────────────────────────
            RoleCard(
                emoji = "🏢",
                title = strings.roleDirector,
                description = strings.roleDirectorDesc,
                gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF4C1D95)),
                onClick = { onEvent(AuthEvent.SelectRole(UserRole.DIRECTOR)) }
            )

            Spacer(Modifier.height(16.dp))

            RoleCard(
                emoji = "🧠",
                title = strings.rolePsychologist,
                description = strings.rolePsychDesc,
                gradientColors = listOf(Color(0xFF0891B2), Color(0xFF164E63)),
                onClick = { onEvent(AuthEvent.SelectRole(UserRole.PSYCHOLOGIST)) }
            )

            Spacer(Modifier.height(16.dp))

            RoleCard(
                emoji = "👤",
                title = strings.roleStudent,
                description = strings.roleStudentDesc,
                gradientColors = listOf(Color(0xFFBE185D), Color(0xFF7C1D45)),
                onClick = { onEvent(AuthEvent.SelectRole(UserRole.STUDENT)) }
            )

            Spacer(Modifier.height(32.dp))

            // ── Already have account? ───────────────────────────────────────
            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { onEvent(AuthEvent.NavigateToLogin) }
            ) {
                Text(strings.haveAccount, color = VioletGlow, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun RoleCard(
    emoji: String,
    title: String,
    description: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        gradientColors[0].copy(alpha = 0.22f),
                        gradientColors[1].copy(alpha = 0.15f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        gradientColors[0].copy(alpha = 0.6f),
                        gradientColors[1].copy(alpha = 0.3f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(gradientColors[0].copy(alpha = 0.5f), Color.Transparent)
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, gradientColors[0].copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 26.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Text("›", color = gradientColors[0], fontSize = 24.sp, fontWeight = FontWeight.Light)
        }
    }
}

