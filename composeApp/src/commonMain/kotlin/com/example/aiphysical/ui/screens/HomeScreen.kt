package com.example.aiphysical.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.auth.AuthEvent
import com.example.aiphysical.presentation.auth.AuthUiState
import com.example.aiphysical.ui.components.AnimatedBackground
import com.example.aiphysical.ui.components.GlassPrimaryButton
import com.example.aiphysical.ui.components.LanguageSwitcher
import com.example.aiphysical.ui.components.glassCard
import com.example.aiphysical.ui.theme.*

@Composable
fun HomeScreen(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
) {
    val strings = getStrings(uiState.currentLanguage)

    AnimatedBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            // ── Top Bar ──────────────────────────────────────────────────────
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

            // ── Welcome Banner ───────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(VioletPrimary.copy(alpha = 0.7f), AccentPink.copy(alpha = 0.5f))
                            ),
                            RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✅", fontSize = 42.sp)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = strings.welcomeHome,
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.appSubtitle,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── Status card ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 20.dp, bgAlpha = 0.10f, borderAlpha = 0.20f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🎉 ${strings.login}",
                    color = SuccessColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                Text(
                    text = strings.appTitle,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings.appSubtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Feature grid placeholder ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureTile(emoji = "📊", title = "Analytics", modifier = Modifier.weight(1f))
                FeatureTile(emoji = "💬", title = "Chat", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureTile(emoji = "🎯", title = "Goals", modifier = Modifier.weight(1f))
                FeatureTile(emoji = "📝", title = "Reports", modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(40.dp))

            // ── Logout button ────────────────────────────────────────────────
            GlassPrimaryButton(
                text = "🚪 ${strings.logoutBtn}",
                onClick = { onEvent(AuthEvent.Logout) }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureTile(emoji: String, title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(90.dp)
            .background(GlassBg, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 28.sp)
            Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
