package com.example.aiphysical.ui.screens.student

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.student.StudentUiState
import com.example.aiphysical.ui.theme.*
import com.example.aiphysical.ui.theme.getStrings

// ══════════════════════════════════════════════════════════════════════════════
//  Student Profile Tab
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun StudentProfileTab(
    state: StudentUiState,
    onLogout: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)
    val profile = state.profile
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 28.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Avatar + Name ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF8A2BE2).copy(0.55f), Color(0xFF8A2BE2).copy(0.15f))),
                        CircleShape
                    )
                    .border(2.dp, Brush.verticalGradient(listOf(Color(0xFF9D5FF5).copy(0.9f), Color(0xFF9D5FF5).copy(0.3f))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile.fullName.take(1).uppercase().ifBlank { "?" },
                    color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.fullName, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text(profile.email, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                val statusColor = statusToColor(profile.latestAiStatus)
                val statusLabel = when (profile.latestAiStatus) {
                    "critical" -> strings.statusCriticalFull
                    "stress"   -> strings.statusStressFull
                    "normal"   -> strings.statusNormalFull
                    else       -> strings.statusNoData
                }
                Box(
                    Modifier
                        .background(statusColor.copy(0.15f), RoundedCornerShape(20.dp))
                        .border(1.dp, statusColor.copy(0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Language Switcher ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.language, color = TextHint, fontSize = 11.sp, modifier = Modifier.weight(1f))
            listOf(AppLanguage.RU, AppLanguage.EN, AppLanguage.KZ).forEach { lang ->
                val isSelected = state.currentLanguage == lang
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PsychTeal.copy(0.20f) else Color.White.copy(0.05f))
                        .border(
                            1.dp,
                            if (isSelected) PsychTeal.copy(0.60f) else Color.White.copy(0.12f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onLanguageChange(lang) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        lang.code.uppercase(),
                        color = if (isSelected) PsychTeal else TextHint,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
        }

        // ── Info cards ─────────────────────────────────────────────────────────
        ProfileInfoSection(profile = profile, testCount = state.testHistory.size, strings = strings)

        // ── Test history preview ──────────────────────────────────────────────
        if (state.testHistory.isNotEmpty()) {
            TestHistorySection(history = state.testHistory.take(5), strings = strings)
        }

        HorizontalDivider(color = Color.White.copy(0.08f))

        // ── About app section ──────────────────────────────────────────────────
        AboutAppCard(strings = strings)

        // ── Logout ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PsychCritical.copy(0.12f))
                .border(1.dp, Brush.verticalGradient(listOf(PsychCritical.copy(0.5f), PsychCritical.copy(0.15f))), RoundedCornerShape(16.dp))
                .clickable { onLogout() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🚪", fontSize = 18.sp)
                Text(strings.profileLogout, color = PsychCritical, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileInfoSection(
    profile: com.example.aiphysical.data.model.UserProfile,
    testCount: Int,
    strings: Strings,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.09f), Color.White.copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.20f), Color.White.copy(0.05f))), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(strings.profileInfoTitle, color = TextHint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)
        listOf(
            Triple("👤", strings.profileRole,           strings.roleStudentShort),
            Triple("🏫", strings.profileGroup,          profile.ageGroup.ifBlank { strings.profileNotSpecified }),
            Triple("📊", strings.profileTestsDone,      "$testCount"),
            Triple("📈", strings.profileCourseProgress, "${profile.courseProgressPercent.toInt()}%"),
        ).forEach { (emoji, label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(emoji, fontSize = 16.sp)
                    Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TestHistorySection(history: List<com.example.aiphysical.data.model.TestResult>, strings: Strings) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(strings.sectionTestHistory.uppercase(), color = TextHint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)
        history.forEach { result ->
            val statusColor = when (result.aiAssessment) {
                "critical" -> PsychCritical; "stress" -> PsychWarning; else -> PsychTeal
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color.White.copy(0.07f), Color.White.copy(0.02f))))
                    .border(1.dp, Brush.verticalGradient(listOf(statusColor.copy(0.30f), statusColor.copy(0.08f))), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(result.testName.ifBlank { result.testId }, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(formatDate(result.dateMillis), color = TextHint, style = MaterialTheme.typography.bodySmall)
                }
                Box(
                    Modifier.background(statusColor.copy(0.15f), RoundedCornerShape(8.dp)).border(1.dp, statusColor.copy(0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${strings.scoreLabel} ${result.score.toInt()}", color = statusColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AboutAppCard(strings: Strings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF8A2BE2).copy(0.10f), Color(0xFF8A2BE2).copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0xFF9D5FF5).copy(0.40f), Color(0xFF9D5FF5).copy(0.10f))), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🧠", fontSize = 22.sp)
            Text(strings.profileAbout, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            "AiPhysical — ${strings.appSubtitle}.",
            color = TextSecondary, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp
        )
        Text("v1.0.0 · 2025", color = TextHint, style = MaterialTheme.typography.bodySmall)
    }
}


private fun formatDate(millis: Long): String {
    if (millis == 0L) return "—"
    var remaining = millis / 86_400_000L
    var year = 1970L
    while (true) {
        val daysInYear = if (isLeap(year)) 366L else 365L
        if (remaining < daysInYear) break
        remaining -= daysInYear
        year++
    }
    val monthDays = intArrayOf(31, if (isLeap(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 0
    var day = remaining.toInt()
    while (month < 12 && day >= monthDays[month]) { day -= monthDays[month]; month++ }
    return "${day + 1}.${month + 1}.$year"
}

private fun isLeap(year: Long): Boolean =
    (year % 4L == 0L && year % 100L != 0L) || year % 400L == 0L

