package com.example.aiphysical.ui.screens.director

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.CourseProgress
import com.example.aiphysical.data.model.TestResult
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.director.DirectorDashboardState
import com.example.aiphysical.presentation.director.DirectorEvent
import com.example.aiphysical.ui.components.*
import com.example.aiphysical.ui.theme.*
import com.example.aiphysical.util.BackPressHandler

@Composable
fun MemberDetailScreen(
    state: DirectorDashboardState,
    onEvent: (DirectorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)
    val member = state.selectedMember ?: return

    // Hardware back → go back to Dashboard
    BackPressHandler(enabled = true, onBack = { onEvent(DirectorEvent.BackToDashboard) })

    DirectorBackground {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── Top navigation bar ────────────────────────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onEvent(DirectorEvent.BackToDashboard) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.06f))
                            .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(12.dp))
                    ) {
                        Text("← ${strings.back}", color = Color.White.copy(0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    LanguageSwitcher(currentLanguage = state.currentLanguage, onLanguageChange = { onEvent(DirectorEvent.ChangeLanguage(it)) })
                }
            }

            // ── Member header card ────────────────────────────────────────────
            item { PremiumMemberHeaderCard(member = member, strings = strings) }

            // ── 5 Metrics ─────────────────────────────────────────────────────
            item { MemberMetricsCard(member = member, strings = strings) }

            // ── Loading ───────────────────────────────────────────────────────
            if (state.isLoadingDetail) {
                item {
                    Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
                        CircularProgressIndicator(color = NeonViolet, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                    }
                }
                return@LazyColumn
            }

            // ── Test History ──────────────────────────────────────────────────
            item {
                DetailSectionHeader(emoji = "🧪", title = strings.sectionTestHistory, accent = NeonViolet)
            }
            if (state.selectedMemberTestHistory.isEmpty()) {
                item { GlassDetailEmpty(strings.noTestHistory, "🧪") }
            } else {
                items(state.selectedMemberTestHistory, key = { "${it.testId}_${it.dateMillis}" }) { test ->
                    PremiumTestResultCard(test = test, strings = strings)
                }
            }

            // ── Course Progress ───────────────────────────────────────────────
            item {
                DetailSectionHeader(emoji = "📚", title = strings.sectionCourseProgress, accent = CyanAccent)
            }
            if (state.selectedMemberCourseProgress.isEmpty()) {
                item { GlassDetailEmpty(strings.noCourseProgress, "📚") }
            } else {
                items(state.selectedMemberCourseProgress, key = { it.courseId }) { course ->
                    PremiumCourseProgressCard(course = course)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─── Premium Member Header Card ───────────────────────────────────────────────

@Composable
private fun PremiumMemberHeaderCard(member: UserProfile, strings: Strings) {
    val statusColor = when (member.latestAiStatus) { "normal" -> StatusNormal; "stress" -> StatusStress; "critical" -> StatusCritical; else -> TextHint }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(listOf(NeonViolet.copy(0.18f), Color.White.copy(0.04f)))
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(listOf(NeonViolet.copy(0.6f), statusColor.copy(0.4f), Color.White.copy(0.05f))),
                RoundedCornerShape(28.dp)
            )
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            // Large gradient avatar
            Box(
                Modifier.size(72.dp)
                    .background(
                        Brush.radialGradient(listOf(NeonViolet.copy(0.7f), CyanAccent.copy(0.3f), CardSurface)),
                        CircleShape
                    )
                    .border(2.dp, Brush.sweepGradient(listOf(NeonViolet, CyanAccent, NeonViolet)), CircleShape),
                Alignment.Center
            ) {
                Text(member.fullName.take(1).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(member.fullName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(member.email, color = Color.White.copy(0.45f), fontSize = 12.sp)
                StatusBadge(status = member.latestAiStatus, label = statusLabel(member.latestAiStatus, strings, member.role))
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.5f), CyanAccent.copy(0.3f), Color.Transparent))))

        // Meta info row
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            MetaInfoChip(
                "🎭",
                strings.memberRole,
                when (member.role) {
                    "director" -> strings.roleDirectorShort
                    "psychologist" -> strings.rolePsychShort
                    "teacher" -> strings.roleTeacherShort
                    else -> strings.roleStudentShort
                },
                NeonViolet
            )
            if (member.ageGroup.isNotBlank()) MetaInfoChip("📅", strings.memberAge, member.ageGroup, CyanAccent)
            MetaInfoChip("⚡", strings.kpiStress, "${member.stressScore.toInt()}%", MetricStress)
            MetaInfoChip("📚", strings.kpiEngagement, "${member.courseProgressPercent.toInt()}%", MetricMotivation)
        }
    }
}

@Composable
private fun MetaInfoChip(emoji: String, label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(0.1f))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(label, color = color.copy(0.7f), fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

// ─── Member 5-Metrics Card ────────────────────────────────────────────────────

@Composable
private fun MemberMetricsCard(member: UserProfile, strings: Strings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.18f), Color.White.copy(0.04f))), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Детальные метрики",
            style = TextStyle(brush = Brush.horizontalGradient(listOf(NeonViolet, CyanAccent)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        )
        MetricProgressBar(label = strings.metricBurnout,    value = member.burnoutScore,    isHighBad = true,  color = MetricBurnout)
        MetricProgressBar(label = strings.kpiStress,         value = member.stressScore,     isHighBad = true,  color = MetricStress)
        MetricProgressBar(label = strings.metricEmotion,    value = member.emotionScore,    isHighBad = false, color = MetricEmotion)
        MetricProgressBar(label = strings.metricMotivation,  value = member.motivationScore, isHighBad = false, color = MetricMotivation)
        MetricProgressBar(label = strings.metricAnxiety,    value = member.anxietyScore,    isHighBad = true,  color = MetricAnxiety)
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun DetailSectionHeader(emoji: String, title: String, accent: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(0.08f))
            .border(1.dp, accent.copy(0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).background(accent.copy(0.2f), RoundedCornerShape(10.dp)).border(1.dp, accent.copy(0.4f), RoundedCornerShape(10.dp)),
            Alignment.Center
        ) { Text(emoji, fontSize = 14.sp) }
        Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GlassDetailEmpty(message: String, emoji: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(0.04f)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(18.dp)).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 30.sp)
        Text(message, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// ─── Premium Test Result Card ─────────────────────────────────────────────────

@Composable
private fun PremiumTestResultCard(test: TestResult, strings: Strings) {
    val statusColor = when (test.aiAssessment) { "critical" -> StatusCritical; "stress" -> StatusStress; else -> StatusNormal }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(statusColor.copy(0.06f))
            .border(1.dp, Brush.horizontalGradient(listOf(statusColor.copy(0.4f), Color.White.copy(0.06f))), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                Modifier.size(36.dp).background(statusColor.copy(0.2f), RoundedCornerShape(10.dp)).border(1.dp, statusColor.copy(0.4f), RoundedCornerShape(10.dp)),
                Alignment.Center
            ) {
                Text(when (test.aiAssessment) { "critical" -> "🔴"; "stress" -> "⚠️"; else -> "✅" }, fontSize = 16.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(test.testName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${strings.testDate}: ${formatDate(test.dateMillis)}", color = TextHint, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${strings.testScore}: ${test.score.toInt()}", color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier.background(statusColor.copy(0.15f), RoundedCornerShape(6.dp)).border(1.dp, statusColor.copy(0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
            ) { Text(test.aiAssessment.replaceFirstChar { it.uppercase() }, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// ─── Premium Course Progress Card ─────────────────────────────────────────────

@Composable
private fun PremiumCourseProgressCard(course: CourseProgress) {
    val animProgress by animateFloatAsState(targetValue = course.progress.coerceIn(0f, 1f), animationSpec = tween(1200), label = "course_anim")
    val progressColor = when {
        animProgress >= 0.8f -> StatusNormal
        animProgress >= 0.4f -> AlertOrange
        else -> NeonViolet
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, Brush.verticalGradient(listOf(progressColor.copy(0.3f), Color.White.copy(0.06f))), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).background(progressColor.copy(0.2f), RoundedCornerShape(10.dp)).border(1.dp, progressColor.copy(0.4f), RoundedCornerShape(10.dp)),
                    Alignment.Center
                ) { Text("📚", fontSize = 16.sp) }
                Text(course.courseName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("${(animProgress * 100).toInt()}%", color = progressColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(0.07f))) {
            Box(
                Modifier.fillMaxWidth(animProgress).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(progressColor, progressColor.copy(0.5f))), RoundedCornerShape(3.dp))
            )
        }
    }
}

// ─── Date formatting ──────────────────────────────────────────────────────────

private fun formatDate(millis: Long): String {
    if (millis == 0L) return "—"
    val totalDays = millis / 86_400_000L
    val year = 1970 + (totalDays / 365.2425).toInt()
    val dayOfYear = (totalDays % 365).toInt().coerceIn(1, 365)
    val month = (dayOfYear / 30.44).toInt().coerceIn(0, 11) + 1
    val day = (dayOfYear % 31).coerceIn(1, 31)
    return "${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.$year"
}
