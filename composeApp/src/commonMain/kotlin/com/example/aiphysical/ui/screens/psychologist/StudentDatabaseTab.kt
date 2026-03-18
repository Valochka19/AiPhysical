package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.TestResult
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.psychologist.PsychologistEvent
import com.example.aiphysical.presentation.psychologist.PsychologistHomeState
import com.example.aiphysical.presentation.psychologist.PsychologistScreen
import com.example.aiphysical.presentation.psychologist.PsychologistViewModel
import com.example.aiphysical.ui.theme.*
import com.example.aiphysical.ui.theme.getStrings
import kotlin.math.*

// ══════════════════════════════════════════════════════════════════════════════
//  Entry Point
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun StudentDatabaseTab(
    state: PsychologistHomeState,
    vm: PsychologistViewModel,
    modifier: Modifier = Modifier,
) {
    if (state.currentScreen == PsychologistScreen.StudentDetail && state.selectedStudent != null) {
        StudentDetailView(
            student = state.selectedStudent,
            testHistory = state.selectedStudentTestHistory,
            isLoading = state.isLoadingDetail,
            strings = getStrings(state.currentLanguage),
            onBack = { vm.onEvent(PsychologistEvent.BackToDashboard) },
            onRecommend = { vm.onEvent(PsychologistEvent.OpenRecommendationSheet(state.selectedStudent)) }
        )
    } else {
        PsychAnalyticsListView(state = state, vm = vm, modifier = modifier)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Analytics List View — mirrors Director's AnalyticsTab (Psychologist theme)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PsychAnalyticsListView(
    state: PsychologistHomeState,
    vm: PsychologistViewModel,
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)

    // Filter: only role=="user" students are already in state.students (filtered in ViewModel)
    val displayedStudents = remember(state.students, state.analyticsFilter) {
        when (state.analyticsFilter) {
            "JUNIOR" -> state.students.filter { it.ageGroup.equals("JUNIOR", ignoreCase = true) }
            "MIDDLE" -> state.students.filter { it.ageGroup.equals("MIDDLE", ignoreCase = true) }
            "SENIOR" -> state.students.filter { it.ageGroup.equals("SENIOR", ignoreCase = true) }
            else     -> state.students   // "ALL"
        }
    }

    val ageFilters = listOf(
        "ALL"    to strings.filterAll,
        "JUNIOR" to strings.filterJunior,
        "MIDDLE" to strings.filterMiddle,
        "SENIOR" to strings.filterSenior,
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(listOf(PsychTeal.copy(0.12f), Color.White.copy(0.04f)))
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PsychTeal.copy(0.45f), PsychTeal.copy(0.10f))),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    strings.dbTitle,
                    style = TextStyle(
                        brush = Brush.horizontalGradient(listOf(PsychTeal, Color.White.copy(0.85f))),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    "${state.students.size} ${strings.sectionMembers.lowercase()} · ${strings.dbAnalytics}",
                    color = Color.White.copy(0.4f),
                    fontSize = 11.sp
                )
            }
        }

        // ── Age-group Filter Chips ────────────────────────────────────────────
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(ageFilters) { (key, label) ->
                    PsychGlassFilterChip(
                        label = label,
                        isActive = state.analyticsFilter == key,
                        onClick = { vm.onEvent(PsychologistEvent.SetAnalyticsFilter(key)) }
                    )
                }
            }
        }

        // ── Filtered Count Badge ──────────────────────────────────────────────
        item {
            AnimatedVisibility(visible = state.analyticsFilter != "ALL") {
                Row(
                    modifier = Modifier
                        .background(PsychTeal.copy(0.10f), RoundedCornerShape(8.dp))
                        .border(1.dp, PsychTeal.copy(0.30f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).background(PsychTeal, CircleShape))
                    Text(
                        "${strings.dbShown} ${displayedStudents.size}",
                        color = PsychTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Loading / Empty / Cards ───────────────────────────────────────────
        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PsychTeal, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                }
            }
            return@LazyColumn
        }

        if (displayedStudents.isEmpty()) {
            item {
                PsychGlassEmptyState(
                    emoji = "🔍",
                    title = strings.dbNoStudents,
                    subtitle = strings.dbChangeFilter
                )
            }
        } else {
            items(displayedStudents, key = { it.uid }) { student ->
                PsychExpandableMemberCard(
                    member = student,
                    strings = strings,
                    onViewDetails = { vm.onEvent(PsychologistEvent.SelectStudent(student)) }
                )
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

// ─── Psych Glass Filter Chip ──────────────────────────────────────────────────

@Composable
private fun PsychGlassFilterChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pchip_$label")
    val glowAlpha by if (isActive) infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pchip_glow"
    ) else remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .then(
                if (isActive) Modifier.drawBehind {
                    drawRoundRect(
                        PsychTeal.copy(alpha = 0.20f * glowAlpha),
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isActive)
                    Brush.horizontalGradient(listOf(PsychTeal, PsychTeal.copy(0.70f)))
                else
                    Brush.horizontalGradient(listOf(Color.White.copy(0.06f), Color.White.copy(0.03f)))
            )
            .border(
                1.dp,
                if (isActive) PsychTeal.copy(0.80f) else Color.White.copy(0.12f),
                RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = if (isActive) Color(0xFF050010) else Color.White.copy(0.5f),
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ─── Psych Glass Empty State ──────────────────────────────────────────────────

@Composable
private fun PsychGlassEmptyState(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, PsychTeal.copy(0.15f), RoundedCornerShape(24.dp))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 40.sp)
        Text(
            title,
            color = TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(subtitle, color = TextHint, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

// ─── Psych Expandable Member Card ─────────────────────────────────────────────

@Composable
private fun PsychExpandableMemberCard(
    member: UserProfile,
    strings: com.example.aiphysical.ui.theme.Strings,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (member.latestAiStatus) {
        "normal"   -> StatusNormal
        "stress"   -> StatusStress
        "critical" -> StatusCritical
        else       -> TextHint
    }
    // Show health status when known; otherwise show the person's role
    val statusLabel = when (member.latestAiStatus) {
        "normal"   -> strings.statusNormal
        "stress"   -> strings.statusStress
        "critical" -> strings.statusCritical
        else -> when (member.role) {
            "user"         -> strings.roleStudentShort
            "teacher"      -> strings.roleTeacherShort
            "psychologist" -> strings.rolePsychShort
            "director"     -> strings.roleDirectorShort
            else           -> strings.statusUnknown
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (expanded)
                    Brush.verticalGradient(listOf(PsychTeal.copy(0.08f), Color.White.copy(0.04f)))
                else
                    Brush.verticalGradient(listOf(Color.White.copy(0.05f), Color.White.copy(0.03f)))
            )
            .border(
                1.dp,
                if (expanded)
                    Brush.linearGradient(listOf(PsychTeal.copy(0.50f), PsychTeal.copy(0.20f)))
                else
                    Brush.linearGradient(listOf(Color.White.copy(0.14f), Color.White.copy(0.04f))),
                RoundedCornerShape(20.dp)
            )
            .animateContentSize(animationSpec = tween(300))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { expanded = !expanded }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Collapsed Header Row ──────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        Brush.radialGradient(listOf(PsychTeal.copy(0.45f), MatteSurface)),
                        CircleShape
                    )
                    .border(1.5.dp, statusColor.copy(0.70f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.fullName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Name + email + age group
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    member.fullName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    member.email,
                    color = Color.White.copy(0.35f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (member.ageGroup.isNotBlank()) {
                    Text(
                        member.ageGroup,
                        color = PsychTeal.copy(0.75f),
                        fontSize = 10.sp
                    )
                }
            }

            // Status badge + burnout + expand indicator
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(0.18f), RoundedCornerShape(8.dp))
                        .border(1.dp, statusColor.copy(0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${member.burnoutScore.toInt()}%",
                        color = MetricBurnout,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (expanded) "▲" else "▼",
                        color = PsychTeal.copy(0.7f),
                        fontSize = 9.sp
                    )
                }
            }
        }

        // ── Expanded Section ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(300)),
            exit  = fadeOut(tween(150)) + shrinkVertically(tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Teal divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    PsychTeal.copy(0.55f),
                                    PsychTeal.copy(0.20f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 5 metric progress bars
                PsychMetricProgressBar(label = strings.metricBurnout,   value = member.burnoutScore,    isHighBad = true,  color = MetricBurnout)
                PsychMetricProgressBar(label = strings.metricStress,     value = member.stressScore,     isHighBad = true,  color = MetricStress)
                PsychMetricProgressBar(label = strings.metricEmotion,   value = member.emotionScore,    isHighBad = false, color = MetricEmotion)
                PsychMetricProgressBar(label = strings.metricMotivation, value = member.motivationScore, isHighBad = false, color = MetricMotivation)
                PsychMetricProgressBar(label = strings.metricAnxiety,   value = member.anxietyScore,    isHighBad = true,  color = MetricAnxiety)

                Spacer(Modifier.height(2.dp))

                // "View full profile" button
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(PsychTeal.copy(0.20f), PsychTeal.copy(0.08f))),
                            RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(PsychTeal.copy(0.60f), PsychTeal.copy(0.30f))),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Text(strings.dbViewProfile, color = PsychTeal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Psych Metric Progress Bar ────────────────────────────────────────────────

@Composable
private fun PsychMetricProgressBar(
    label: String,
    value: Float,
    isHighBad: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 100f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "pmetric_$label"
    )
    val barColor = when {
        isHighBad -> when {
            animValue > 70f -> ErrorColor
            animValue > 40f -> AlertOrange
            else            -> SuccessColor
        }
        else -> when {
            animValue < 30f -> ErrorColor
            animValue < 60f -> AlertOrange
            else            -> SuccessColor
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            color = color.copy(0.80f),
            fontSize = 11.sp,
            modifier = Modifier.width(84.dp),
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.07f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animValue / 100f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(listOf(barColor, barColor.copy(0.55f)))
                    )
            )
        }
        Text(
            "${animValue.toInt()}%",
            color = barColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Student Detail View (full profile — navigation target when card is tapped)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StudentDetailView(
    student: UserProfile,
    testHistory: List<TestResult>,
    isLoading: Boolean,
    strings: com.example.aiphysical.ui.theme.Strings,
    onBack: () -> Unit,
    onRecommend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MatteSurface)
                    .border(1.dp, MatteCardBorder, RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text(strings.dbStudentProfile, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }

        StudentIdentityCard(student = student, strings = strings)
        PsychRadarCard(student = student, strings = strings)
        TestHistoryCard(testHistory = testHistory, isLoading = isLoading, strings = strings)

        if (student.psychComment.isNotBlank()) {
            ExistingRecommendationCard(student = student, strings = strings)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(PsychTeal.copy(0.25f), PsychTeal.copy(0.10f))))
                .border(1.dp, PsychTeal.copy(0.5f), RoundedCornerShape(16.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRecommend)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("💬", fontSize = 20.sp)
                Text(
                    if (student.psychComment.isBlank()) strings.dbWriteRec else strings.dbUpdateRec,
                    color = PsychTeal, fontSize = 15.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StudentIdentityCard(student: UserProfile, strings: com.example.aiphysical.ui.theme.Strings) {
    val (statusColor, statusEmoji) = statusColorAndEmoji(student.latestAiStatus)
    val statusLabel = when (student.latestAiStatus) {
        "critical" -> strings.statusCritical
        "stress"   -> strings.statusStress
        "normal"   -> strings.statusNormal
        else -> when (student.role) {
            "user"         -> strings.roleStudentShort
            "teacher"      -> strings.roleTeacherShort
            "psychologist" -> strings.rolePsychShort
            "director"     -> strings.roleDirectorShort
            else           -> strings.statusUnknown
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    Brush.radialGradient(listOf(statusColor.copy(0.30f), MatteSurface)),
                    CircleShape
                )
                .border(2.dp, statusColor.copy(0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                student.fullName.take(2).uppercase(),
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(student.fullName, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            if (student.ageGroup.isNotBlank()) {
                Text(student.ageGroup, color = TextSecondary, fontSize = 12.sp)
            }
            Text(student.email, color = TextHint, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(statusColor.copy(0.18f), RoundedCornerShape(8.dp))
                    .border(1.dp, statusColor.copy(0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "$statusEmoji $statusLabel",
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Radar Chart (5 metrics) ───────────────────────────────────────────────────

@Composable
private fun PsychRadarCard(student: UserProfile, strings: com.example.aiphysical.ui.theme.Strings) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(student.uid) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
    }
    val progress by animProgress.asState()

    val axes = listOf(
        strings.metricBurnout   to student.burnoutScore / 100f,
        strings.metricStress    to student.stressScore  / 100f,
        strings.metricAnxiety   to student.anxietyScore / 100f,
        strings.metricEmotion   to student.emotionScore / 100f,
        strings.metricMotivation to student.motivationScore / 100f,
    )
    val axisColors = listOf(MetricBurnout, MetricStress, MetricAnxiety, MetricEmotion, MetricMotivation)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            strings.dbPsychSection,
            color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = minOf(cx, cy) * 0.80f
            val n = axes.size
            val angleStep = 2 * PI.toFloat() / n

            for (ring in 1..4) {
                val r = maxR * ring / 4f
                val ringPath = Path()
                for (i in 0..n) {
                    val angle = -PI.toFloat() / 2 + i * angleStep
                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)
                    if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                }
                ringPath.close()
                drawPath(ringPath, MatteCardBorder.copy(0.5f), style = Stroke(1f))
            }

            for (i in 0 until n) {
                val angle = -PI.toFloat() / 2 + i * angleStep
                drawLine(
                    color = MatteCardBorder.copy(0.6f),
                    start = Offset(cx, cy),
                    end = Offset(cx + maxR * cos(angle), cy + maxR * sin(angle)),
                    strokeWidth = 1f
                )
            }

            val dataPath = Path()
            axes.forEachIndexed { i, (_, v) ->
                val angle = -PI.toFloat() / 2 + i * angleStep
                val r = maxR * v * progress
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()
            drawPath(dataPath, PsychTeal.copy(0.25f))
            drawPath(
                dataPath, PsychTeal.copy(0.80f),
                style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            axes.forEachIndexed { i, (_, v) ->
                val angle = -PI.toFloat() / 2 + i * angleStep
                val r = maxR * v * progress
                drawCircle(
                    axisColors[i],
                    radius = 5f,
                    center = Offset(cx + r * cos(angle), cy + r * sin(angle))
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            axes.forEachIndexed { i, (label, v) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(8.dp).background(axisColors[i], CircleShape))
                    Spacer(Modifier.height(3.dp))
                    Text(label, color = TextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center)
                    Text(
                        "${(v * 100).toInt()}%",
                        color = axisColors[i],
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Test History Timeline ─────────────────────────────────────────────────────

@Composable
private fun TestHistoryCard(testHistory: List<TestResult>, isLoading: Boolean, strings: com.example.aiphysical.ui.theme.Strings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(strings.sectionTestHistory.uppercase(), color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)

        if (isLoading) {
            repeat(3) {
                val shimmerTransition = rememberInfiniteTransition(label = "shimmer_$it")
                val alpha by shimmerTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s_$it"
                )
                Box(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).background(MatteCardBorder.copy(alpha)))
            }
        } else if (testHistory.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📋", fontSize = 28.sp)
                Text(strings.noTestHistory, color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            testHistory.forEachIndexed { index, result ->
                TestTimelineItem(result = result, isLast = index == testHistory.lastIndex, strings = strings)
            }
        }
    }
}

@Composable
private fun TestTimelineItem(result: TestResult, isLast: Boolean, strings: com.example.aiphysical.ui.theme.Strings) {
    val (color, emoji) = when (result.aiAssessment) {
        "critical" -> PsychCritical to "🔴"
        "stress"   -> PsychWarning  to "⚠️"
        "normal"   -> PsychTeal     to "✅"
        else       -> TextHint      to "❓"
    }
    val label = when (result.aiAssessment) {
        "critical" -> strings.statusCritical
        "stress"   -> strings.statusStress
        "normal"   -> strings.statusNormal
        else       -> strings.statusUnknown
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(0.18f), CircleShape)
                    .border(2.dp, color.copy(0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 13.sp)
            }
            if (!isLast) {
                Box(Modifier.width(1.dp).height(20.dp).background(MatteCardBorder))
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
        ) {
            Text(result.testName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .background(color.copy(0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, color.copy(0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("${strings.scoreLabel} ${result.score.toInt()}", color = TextSecondary, fontSize = 11.sp)
                if (result.dateMillis > 0L) {
                    Text(formatDate(result.dateMillis), color = TextHint, fontSize = 10.sp)
                }
            }
        }
    }
}

// ── Existing Recommendation Card ──────────────────────────────────────────────

@Composable
private fun ExistingRecommendationCard(student: UserProfile, strings: com.example.aiphysical.ui.theme.Strings) {
    val (priorityColor, priorityLabel) = when (student.psychPriority) {
        "HIGH"   -> PsychCritical to strings.priorityHigh
        "MEDIUM" -> PsychWarning  to strings.priorityMedium
        "LOW"    -> PsychTeal     to strings.priorityLow
        else     -> TextHint      to strings.priorityNone
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, PsychTeal.copy(0.3f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("💬", fontSize = 18.sp)
            Text(strings.dbMyRec, color = PsychTeal, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .background(priorityColor.copy(0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, priorityColor.copy(0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("${strings.dbPriority} $priorityLabel", color = priorityColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(student.psychComment, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
        if (student.assignedCourseName.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("📚", fontSize = 14.sp)
                Text("${strings.dbCourse} ${student.assignedCourseName}", color = PsychTeal, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun statusColorAndEmoji(status: String): Pair<Color, String> = when (status) {
    "critical" -> PsychCritical to "🔴"
    "stress"   -> PsychWarning  to "⚠️"
    "normal"   -> PsychTeal     to "✅"
    else       -> TextHint      to "❓"
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return ""
    val days = millis / 86400000L
    val epochDays = 25569L
    val d = days - epochDays
    return "${d % 30 + 1}.${(d / 30) % 12 + 1}.${1970 + d / 365}"
}
