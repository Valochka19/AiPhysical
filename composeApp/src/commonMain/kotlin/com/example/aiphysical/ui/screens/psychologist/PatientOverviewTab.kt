package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.psychologist.PsychologistEvent
import com.example.aiphysical.presentation.psychologist.PsychologistHomeState
import com.example.aiphysical.presentation.psychologist.PsychologistViewModel
import com.example.aiphysical.ui.theme.*

@Composable
fun PatientOverviewTab(
    state: PsychologistHomeState,
    vm: PsychologistViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── 1. Header ─────────────────────────────────────────────────────────
        PsychHeaderSection(
            name = state.psychologistName,
            urgentCount = state.criticalStudents.size + state.stressStudents.size,
            onLogout = onLogout
        )

        if (state.isLoading) {
            PsychLoadingSkeleton()
        } else {
            // ── 2. Alert Zone: Critical cases ──────────────────────────────────
            if (state.criticalStudents.isNotEmpty()) {
                CriticalAlertZone(
                    criticalStudents = state.criticalStudents,
                    onViewStudent = { vm.onEvent(PsychologistEvent.SelectStudent(it)) },
                    onRecommend = { vm.onEvent(PsychologistEvent.OpenRecommendationSheet(it)) }
                )
            }

            // ── 3. Psychological Climate Card ──────────────────────────────────
            PsychClimateCard(
                climate = state.psychClimate,
                totalStudents = state.students.size,
                criticalCount = state.criticalStudents.size,
                stressCount = state.stressStudents.size,
                avgBurnout = state.avgBurnout,
                avgStress = state.avgStress,
                avgAnxiety = state.avgAnxiety
            )

            // ── 4. Pending interventions nudge ─────────────────────────────────
            if (state.pendingRecommendations.isNotEmpty()) {
                PendingInterventionsCard(
                    count = state.pendingRecommendations.size,
                    onClick = { vm.onEvent(PsychologistEvent.NavigateToTab(
                        com.example.aiphysical.presentation.psychologist.PsychologistTab.Interventions
                    )) }
                )
            }

            // ── 5. Stress students compact list ───────────────────────────────
            if (state.stressStudents.isNotEmpty()) {
                StressStudentsSection(
                    stressStudents = state.stressStudents.take(4),
                    onViewStudent = { vm.onEvent(PsychologistEvent.SelectStudent(it)) }
                )
            }

            // ── 6. No students empty state ─────────────────────────────────────
            if (state.students.isEmpty()) {
                EmptyPsychState(
                    message = "Студенты ещё не зарегистрировались в организации"
                )
            }
        }
    }
}

// ── 1. Header ─────────────────────────────────────────────────────────────────

@Composable
private fun PsychHeaderSection(
    name: String,
    urgentCount: Int,
    onLogout: () -> Unit,
) {
    val firstName = name.split(" ").firstOrNull() ?: name

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Добрый день,",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = firstName,
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }
            // Logout button – minimal
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MatteSurface)
                    .border(1.dp, MatteCardBorder, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null, onClick = onLogout
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text("Выйти", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        // Subtitle with urgency info
        if (urgentCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(8.dp).background(PsychCritical, CircleShape))
                Text(
                    text = "Сегодня $urgentCount студент${urgentCountSuffix(urgentCount)} требуют внимания",
                    color = PsychCritical,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = "Все показатели в норме",
                color = PsychTeal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun urgentCountSuffix(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> ""
    count % 10 in 2..4 && count % 100 !in 12..14 -> "а"
    else -> "ов"
}

// ── 2. Critical Alert Zone ────────────────────────────────────────────────────

@Composable
private fun CriticalAlertZone(
    criticalStudents: List<UserProfile>,
    onViewStudent: (UserProfile) -> Unit,
    onRecommend: (UserProfile) -> Unit,
) {
    // Pulsing border animation
    val infiniteTransition = rememberInfiniteTransition(label = "alert_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2A0B14))
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(PsychCritical.copy(pulseAlpha), PsychCritical.copy(pulseAlpha * 0.4f))
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Zone header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PsychCritical.copy(0.20f), CircleShape)
                    .border(1.dp, PsychCritical.copy(0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🔴", fontSize = 16.sp)
            }
            Column {
                Text(
                    "КРИТИЧЕСКИЕ СЛУЧАИ",
                    color = PsychCritical,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    "${criticalStudents.size} студент${urgentCountSuffix(criticalStudents.size)} требуют немедленного внимания",
                    color = PsychCritical.copy(0.7f),
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = PsychCritical.copy(0.15f))

        // Critical student cards
        criticalStudents.forEach { student ->
            CriticalStudentCard(
                student = student,
                onView = { onViewStudent(student) },
                onRecommend = { onRecommend(student) }
            )
        }
    }
}

@Composable
private fun CriticalStudentCard(
    student: UserProfile,
    onView: () -> Unit,
    onRecommend: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PsychCritical.copy(0.07f))
            .border(1.dp, PsychCritical.copy(0.25f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { expanded = !expanded }
            .animateContentSize(animationSpec = tween(300))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        Brush.radialGradient(listOf(PsychCritical.copy(0.35f), Color(0xFF2A0B14))),
                        CircleShape
                    )
                    .border(1.5.dp, PsychCritical.copy(0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    student.fullName.take(1).uppercase(),
                    color = PsychCritical,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    student.fullName,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (student.ageGroup.isNotBlank()) {
                        Text(student.ageGroup, color = TextSecondary, fontSize = 11.sp)
                        Text("·", color = TextHint, fontSize = 11.sp)
                    }
                    Text(student.email, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // CRITICAL badge
            Box(
                modifier = Modifier
                    .background(PsychCritical, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "КРИТИЧНО",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Metric bars (visible when not expanded, always shown)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniMetricPill("Стресс", student.stressScore, PsychCritical, Modifier.weight(1f))
            MiniMetricPill("Выгорание", student.burnoutScore, PsychWarning, Modifier.weight(1f))
            MiniMetricPill("Тревога", student.anxietyScore, MetricAnxiety, Modifier.weight(1f))
        }

        // Expanded: action buttons
        if (expanded) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View detail
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MatteSurface)
                        .border(1.dp, MatteCardBorder, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null, onClick = onView
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📊 Профиль", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                // Send recommendation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PsychCritical)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null, onClick = onRecommend
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💬 Рекомендация", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── 3. Psychological Climate Card ─────────────────────────────────────────────

@Composable
private fun PsychClimateCard(
    climate: String,
    totalStudents: Int,
    criticalCount: Int,
    stressCount: Int,
    avgBurnout: Float,
    avgStress: Float,
    avgAnxiety: Float,
) {
    val (climateColor, climateLabel, climateEmoji) = when (climate) {
        "critical" -> Triple(PsychCritical, "Критический климат", "🔴")
        "warning"  -> Triple(PsychWarning,  "Требует внимания",  "⚠️")
        "good"     -> Triple(PsychTeal,     "Благоприятный климат", "✅")
        else       -> Triple(TextHint,      "Данных недостаточно", "❓")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(climateEmoji, fontSize = 22.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    "Психологический климат",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Text(
                    climateLabel,
                    color = climateColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$totalStudents",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("студентов", color = TextSecondary, fontSize = 10.sp)
            }
        }

        HorizontalDivider(color = MatteCardBorder)

        // Quick stats row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ClimateStatItem("Критичных", criticalCount.toString(), PsychCritical)
            ClimateStatItem("Стресс", stressCount.toString(), PsychWarning)
            ClimateStatItem("Норма", (totalStudents - criticalCount - stressCount).coerceAtLeast(0).toString(), PsychTeal)
        }

        HorizontalDivider(color = MatteCardBorder)

        // Average metrics
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "СРЕДНИЕ ПОКАЗАТЕЛИ",
                color = TextHint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            MatteMetricBar("Выгорание", avgBurnout, MetricBurnout)
            MatteMetricBar("Стресс", avgStress, MetricStress)
            MatteMetricBar("Тревожность", avgAnxiety, MetricAnxiety)
        }
    }
}

@Composable
private fun ClimateStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

// ── 4. Pending Interventions Nudge ────────────────────────────────────────────

@Composable
private fun PendingInterventionsCard(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MatteSurface)
            .border(1.dp, PsychWarning.copy(0.4f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("💬", fontSize = 24.sp)
        Column(Modifier.weight(1f)) {
            Text(
                "$count студент${urgentCountSuffix(count)} ожидают рекомендации",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Перейти в раздел «Помощь»",
                color = PsychWarning,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text("›", color = PsychWarning, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

// ── 5. Stress Students Section ────────────────────────────────────────────────

@Composable
private fun StressStudentsSection(
    stressStudents: List<UserProfile>,
    onViewStudent: (UserProfile) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(8.dp).background(PsychWarning, CircleShape))
            Text(
                "ПОВЫШЕННЫЙ СТРЕСС",
                color = PsychWarning,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        }
        stressStudents.forEach { student ->
            StressStudentRow(student = student, onClick = { onViewStudent(student) })
        }
    }
}

@Composable
private fun StressStudentRow(student: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(PsychWarning.copy(0.15f), CircleShape)
                .border(1.dp, PsychWarning.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(student.fullName.take(1).uppercase(), color = PsychWarning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(student.fullName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Стресс: ${student.stressScore.toInt()}%", color = PsychWarning, fontSize = 11.sp)
        }
        Text("›", color = TextHint, fontSize = 16.sp)
    }
}

// ── Loading Skeleton ──────────────────────────────────────────────────────────

@Composable
private fun PsychLoadingSkeleton() {
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmerTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MatteSurface.copy(alpha = shimmerAlpha))
            )
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyPsychState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(20.dp))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🧠", fontSize = 40.sp)
        Text(message, color = TextSecondary, fontSize = 13.sp)
    }
}

// ── Shared small components ───────────────────────────────────────────────────

@Composable
fun MiniMetricPill(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    val animVal by animateFloatAsState(value, tween(800, easing = FastOutSlowInEasing), label = "pill_$label")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.10f))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${animVal.toInt()}%", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = color.copy(0.7f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun MatteMetricBar(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    val animVal by animateFloatAsState(
        targetValue = (value / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "bar_$label"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(90.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animVal)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(color, color.copy(0.6f))),
                        RoundedCornerShape(3.dp)
                    )
            )
        }
        Text("${value.toInt()}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
    }
}

