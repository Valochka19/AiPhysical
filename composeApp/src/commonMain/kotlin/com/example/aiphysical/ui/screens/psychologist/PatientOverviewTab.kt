package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.presentation.psychologist.PsychologistEvent
import com.example.aiphysical.presentation.psychologist.PsychologistHomeState
import com.example.aiphysical.presentation.psychologist.PsychologistTab
import com.example.aiphysical.presentation.psychologist.PsychologistViewModel
import com.example.aiphysical.presentation.psychologist.RecentTestFeedItem
import com.example.aiphysical.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ══════════════════════════════════════════════════════════════════════════════
//  PatientOverviewTab — Glassmorphic Redesign
//  Role: Psychologist Home Screen
// ══════════════════════════════════════════════════════════════════════════════

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
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ① AI-Orb header + greeting
        GlassAiOrbHeader(
            name = state.psychologistName,
            climate = state.psychClimate,
            urgentCount = state.criticalStudents.size + state.stressStudents.size,
            onLogout = onLogout
        )

        if (state.isLoading) {
            GlassLoadingSkeleton()
        } else {
            // ② Group Climate Hub (CTA)
            GroupClimateHubCard(
                climate = state.psychClimate,
                totalStudents = state.students.size,
                criticalCount = state.criticalStudents.size,
                stressCount = state.stressStudents.size,
                onViewReports = {
                    vm.onEvent(PsychologistEvent.NavigateToTab(PsychologistTab.Database))
                }
            )

            // ③ Radar chart (requires data)
            if (state.students.isNotEmpty()) {
                GlassRadarCard(
                    avgBurnout = state.avgBurnout,
                    avgStress = state.avgStress,
                    avgAnxiety = state.avgAnxiety,
                    avgEmotion = state.avgEmotion,
                    avgMotivation = state.avgMotivation,
                    climate = state.psychClimate
                )
            }

            // ④ Summary glass chips
            if (state.students.isNotEmpty()) {
                SummaryGlassChips(
                    total = state.students.size,
                    critical = state.criticalStudents.size,
                    stable = (state.students.size - state.criticalStudents.size - state.stressStudents.size)
                        .coerceAtLeast(0)
                )
            }

            // ⑤ Critical alert zone
            if (state.criticalStudents.isNotEmpty()) {
                GlassCriticalAlertZone(
                    criticalStudents = state.criticalStudents,
                    onViewStudent = { vm.onEvent(PsychologistEvent.SelectStudent(it)) },
                    onRecommend = { vm.onEvent(PsychologistEvent.OpenRecommendationSheet(it)) }
                )
            }

            // ⑥ Recent test feed
            if (state.recentTestFeed.isNotEmpty()) {
                GlassTestResultsFeed(
                    items = state.recentTestFeed.take(5),
                    onViewResult = { vm.onEvent(PsychologistEvent.ViewTestResult(it)) },
                    onViewAll = { vm.onEvent(PsychologistEvent.NavigateToTab(PsychologistTab.Database)) }
                )
            }

            // ⑦ Pending interventions nudge
            if (state.pendingRecommendations.isNotEmpty()) {
                GlassPendingInterventionsCard(
                    count = state.pendingRecommendations.size,
                    onClick = { vm.onEvent(PsychologistEvent.NavigateToTab(PsychologistTab.Interventions)) }
                )
            }

            // ⑧ Empty state
            if (state.students.isEmpty()) {
                GlassEmptyState("Студенты ещё не зарегистрировались в организации")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ① AI-Orb Header
//  Design: Animated 3D-feel orb with neon glow. Orb color reacts to climate.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassAiOrbHeader(
    name: String,
    climate: String,
    urgentCount: Int,
    onLogout: () -> Unit,
) {
    val firstName = name.split(" ").firstOrNull() ?: name
    val orbColor = climateAccentColor(climate)

    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")
    val orbGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb_glow"
    )
    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "orb_rotation"
    )
    val innerSweep by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart),
        label = "inner_sweep"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── AI Orb ────────────────────────────────────────────────────────────
        Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
            // Layer 1: wide ambient glow
            Canvas(Modifier.size(76.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            orbColor.copy(alpha = orbGlowAlpha * 0.7f),
                            orbColor.copy(alpha = orbGlowAlpha * 0.2f),
                            Color.Transparent
                        ),
                        radius = size.minDimension / 2f
                    )
                )
            }
            // Layer 2: rotating sweep arc
            Canvas(Modifier.size(64.dp).graphicsLayer { rotationZ = orbRotation }) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(orbColor.copy(1.0f), orbColor.copy(0.4f), Color.Transparent, Color.Transparent)
                    ),
                    startAngle = 0f, sweepAngle = 200f, useCenter = false,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Layer 3: counter-rotating thin ring
            Canvas(Modifier.size(54.dp).graphicsLayer { rotationZ = -innerSweep * 0.6f }) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color.White.copy(0.5f), Color.White.copy(0.15f), Color.Transparent)
                    ),
                    startAngle = 60f, sweepAngle = 120f, useCenter = false,
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Layer 4: solid orb body
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(orbColor.copy(0.75f), orbColor.copy(0.30f), Color(0xFF080B1A).copy(0.85f))
                        ),
                        CircleShape
                    )
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(listOf(orbColor.copy(0.9f), orbColor.copy(0.3f))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 20.sp)
            }
        }

        // ── Greeting text ─────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Добрый день,", color = TextSecondary, fontSize = 13.sp)
            Text(
                firstName, color = TextPrimary, fontSize = 27.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp
            )
            if (urgentCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(7.dp).background(PsychCritical, CircleShape))
                    Text("$urgentCount требуют внимания", color = PsychCritical, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text("Все показатели в норме ✓", color = PsychTeal, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // ── Logout ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(0.06f))
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(0.20f), Color.White.copy(0.04f))),
                    RoundedCornerShape(12.dp)
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onLogout)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text("Выйти", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ② Group Climate Hub Card
//  PRIMARY ACTION: "View Test Reports" — wide neon CTA button
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GroupClimateHubCard(
    climate: String,
    totalStudents: Int,
    criticalCount: Int,
    stressCount: Int,
    onViewReports: () -> Unit,
) {
    val accentColor = climateAccentColor(climate)
    val statusIcon  = when (climate) { "critical" -> "⚠️"; "warning" -> "📊"; "good" -> "✅"; else -> "🔍" }

    val infiniteTransition = rememberInfiniteTransition(label = "hub_glow")
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hub_border"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.11f), Color.White.copy(0.04f))))
            .background(accentColor.copy(alpha = 0.07f))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(accentColor.copy(borderPulse), Color.White.copy(0.12f), accentColor.copy(borderPulse * 0.2f))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Verdict row
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        Brush.radialGradient(listOf(accentColor.copy(0.55f), accentColor.copy(0.10f))),
                        CircleShape
                    )
                    .border(1.5.dp, accentColor.copy(0.70f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(statusIcon, fontSize = 22.sp) }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("ПСИХОЭМОЦИОНАЛЬНЫЙ ФОН", color = accentColor.copy(0.70f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
                Text(climateVerdict(climate), color = accentColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                Text(climateSubText(climate), color = accentColor.copy(0.60f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider(color = accentColor.copy(0.14f))

        // Stats row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            ClimateStatColumn(totalStudents, "Всего", TextPrimary)
            Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.12f)))
            ClimateStatColumn(criticalCount, "Критично", PsychCritical)
            Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.12f)))
            ClimateStatColumn(stressCount, "Стресс", PsychWarning)
            Box(Modifier.width(1.dp).height(36.dp).background(Color.White.copy(0.12f)))
            ClimateStatColumn((totalStudents - criticalCount - stressCount).coerceAtLeast(0), "Норма", PsychTeal)
        }

        HorizontalDivider(color = accentColor.copy(0.14f))

        // ── PRIMARY CTA ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(PsychTeal.copy(0.18f), Color(0xFF8A2BE2).copy(0.18f))))
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(PsychTeal.copy(0.85f), Color(0xFF9D5FF5).copy(0.85f))),
                    RoundedCornerShape(16.dp)
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onViewReports)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(PsychTeal.copy(0.22f), RoundedCornerShape(9.dp))
                        .border(1.dp, PsychTeal.copy(0.55f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("📋", fontSize = 17.sp) }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Просмотреть отчёты тестов", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Полная аналитика по группе →", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ClimateStatColumn(value: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$value", color = color, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = color.copy(0.60f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ③ Glass Radar Card + Spider Chart
//  Replaces 5 circular gauges with a single psychographic radar diagram.
// ══════════════════════════════════════════════════════════════════════════════

private val radarAxisLabels = listOf("Выгорание", "Стресс", "Тревога", "Состояние", "Мотивация")
private val radarAxisColors = listOf(MetricBurnout, MetricStress, MetricAnxiety, MetricEmotion, MetricMotivation)

@Composable
private fun GlassRadarCard(
    avgBurnout: Float,
    avgStress: Float,
    avgAnxiety: Float,
    avgEmotion: Float,
    avgMotivation: Float,
    climate: String,
) {
    val accentColor = climateAccentColor(climate)
    val values = listOf(avgBurnout, avgStress, avgAnxiety, avgEmotion, avgMotivation)

    // wrapContentHeight — card strictly hugs its content, no dead space
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.03f))))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(0.24f), Color.White.copy(0.05f))),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Header row: title pinned left, chip pinned right ──────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "ПСИХОЛОГИЧЕСКИЙ ПРОФИЛЬ",
                    color = TextHint,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.6.sp
                )
                Text(
                    "Радарная диаграмма группы",
                    color = TextSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .background(accentColor.copy(0.14f), RoundedCornerShape(8.dp))
                    .border(1.dp, accentColor.copy(0.40f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "Ср. по группе",
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Hero radar chart — fixed 270dp, centred, fills 80%+ of card width ─
        RadarSpiderChart(
            values = values,
            fillColor = accentColor,
            vertexColors = radarAxisColors,
            modifier = Modifier
                .size(270.dp)
                .align(Alignment.CenterHorizontally)
        )

        // ── Compact legend: colour dot + label + value ─────────────────────────
        // 5 items in one balanced Row; bodySmall keeps text from wrapping
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            radarAxisLabels.forEachIndexed { i, label ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(radarAxisColors[i], CircleShape)
                    )
                    Text(
                        label,
                        color = TextHint,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        "${values[i].toInt()}%",
                        color = radarAxisColors[i],
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Glassmorphic Spider / Radar chart — fully multiplatform (no Android APIs).
 *
 * Design choices:
 *  • 5 concentric web rings provide depth & scale reference
 *  • Animated polygon entrance (0 → full in 1500 ms)
 *  • Radial gradient fill creates Z-axis depth illusion
 *  • Per-vertex color dots match the legend, enabling instant axis identification
 */
@Composable
private fun RadarSpiderChart(
    values: List<Float>,        // 0f–100f, one per axis
    fillColor: Color,           // polygon fill (matches climate accent)
    vertexColors: List<Color>,  // per-axis vertex color
    modifier: Modifier = Modifier,
) {
    val count = values.size
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(values) {
        animatable.snapTo(0f)
        animatable.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }
    val progress by animatable.asState()

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        // 82% of the half-dimension → chart fills the canvas as a hero element
        val maxR = minOf(cx, cy) * 0.82f
        val levels = 5

        // ── Concentric web rings ──────────────────────────────────────────────
        for (level in 1..levels) {
            val r = maxR * level / levels
            val pts = (0 until count).map { i ->
                val a = (2.0 * PI / count * i - PI / 2).toFloat()
                Offset(cx + r * cos(a), cy + r * sin(a))
            }
            for (i in pts.indices) {
                drawLine(
                    color = Color.White.copy(alpha = if (level == levels) 0.18f else 0.08f),
                    start = pts[i],
                    end = pts[(i + 1) % count],
                    strokeWidth = if (level == levels) 1.2.dp.toPx() else 0.7.dp.toPx()
                )
            }
        }

        // ── Axis lines ────────────────────────────────────────────────────────
        for (i in 0 until count) {
            val a = (2.0 * PI / count * i - PI / 2).toFloat()
            drawLine(
                color = Color.White.copy(0.14f),
                start = Offset(cx, cy),
                end = Offset(cx + maxR * cos(a), cy + maxR * sin(a)),
                strokeWidth = 0.8.dp.toPx()
            )
        }

        // ── Data polygon ──────────────────────────────────────────────────────
        val pts = (0 until count).map { i ->
            val a = (2.0 * PI / count * i - PI / 2).toFloat()
            val r = maxR * (values[i] / 100f).coerceIn(0f, 1f) * progress
            Offset(cx + r * cos(a), cy + r * sin(a))
        }

        val fillPath = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until count) lineTo(pts[i].x, pts[i].y)
            close()
        }

        // Semi-transparent radial fill
        drawPath(
            path = fillPath,
            brush = Brush.radialGradient(
                colors = listOf(fillColor.copy(0.45f), fillColor.copy(0.12f)),
                center = Offset(cx, cy), radius = maxR
            )
        )

        // Neon polygon outline
        drawPath(
            path = fillPath,
            color = fillColor.copy(0.92f),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // ── Vertex dots ───────────────────────────────────────────────────────
        pts.forEachIndexed { i, pt ->
            val vc = vertexColors[i]
            drawCircle(vc.copy(0.22f), radius = 9.dp.toPx(),  center = pt) // glow
            drawCircle(vc.copy(0.55f), radius = 5.dp.toPx(),  center = pt) // ring
            drawCircle(vc,             radius = 3.dp.toPx(),  center = pt) // core
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ④ Summary Glass Chips
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SummaryGlassChips(total: Int, critical: Int, stable: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassStatChip("👥", "$total",    "Студентов", TextPrimary,   Color.White.copy(0.28f), Modifier.weight(1f))
        GlassStatChip("🔴", "$critical", "Критично",  PsychCritical, PsychCritical.copy(0.55f), Modifier.weight(1f))
        GlassStatChip("✅", "$stable",   "Стабильно", PsychTeal,     PsychTeal.copy(0.55f),  Modifier.weight(1f))
    }
}

@Composable
private fun GlassStatChip(icon: String, value: String, label: String, color: Color, borderColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(color.copy(0.13f), color.copy(0.04f))))
            .border(1.dp, Brush.verticalGradient(listOf(borderColor, borderColor.copy(0.25f))), RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 20.sp)
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(label, color = color.copy(0.65f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ⑤ Critical Alert Zone (Glassmorphic)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassCriticalAlertZone(
    criticalStudents: List<UserProfile>,
    onViewStudent: (UserProfile) -> Unit,
    onRecommend: (UserProfile) -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "crit_pulse")
    val borderAlpha by pulse.animateFloat(
        initialValue = 0.40f, targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "crit_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(PsychCritical.copy(0.13f), PsychCritical.copy(0.04f))))
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(PsychCritical.copy(borderAlpha), PsychCritical.copy(borderAlpha * 0.25f))),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PsychCritical.copy(0.20f), CircleShape)
                    .border(1.dp, PsychCritical.copy(0.60f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🚨", fontSize = 16.sp) }
            Column {
                Text("КРИТИЧЕСКИЕ СЛУЧАИ", color = PsychCritical, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.0.sp)
                Text("${criticalStudents.size} студент${psySuffix(criticalStudents.size)} требуют немедленного внимания", color = PsychCritical.copy(0.70f), fontSize = 12.sp)
            }
        }
        HorizontalDivider(color = PsychCritical.copy(0.15f))
        criticalStudents.forEach { s ->
            GlassCriticalStudentCard(student = s, onView = { onViewStudent(s) }, onRecommend = { onRecommend(s) })
        }
    }
}

@Composable
private fun GlassCriticalStudentCard(student: UserProfile, onView: () -> Unit, onRecommend: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, PsychCritical.copy(0.26f), RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
            .animateContentSize(animationSpec = tween(300))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Brush.radialGradient(listOf(PsychCritical.copy(0.42f), Color.Transparent)), CircleShape)
                    .border(1.5.dp, PsychCritical.copy(0.60f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(student.fullName.take(1).uppercase(), color = PsychCritical, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(student.fullName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(student.email, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.background(PsychCritical, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("КРИТИЧНО", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniMetricPill("Стресс",    student.stressScore,   PsychCritical, Modifier.weight(1f))
            MiniMetricPill("Выгорание", student.burnoutScore,  PsychWarning,  Modifier.weight(1f))
            MiniMetricPill("Тревога",   student.anxietyScore,  MetricAnxiety, Modifier.weight(1f))
        }
        if (expanded) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.07f))
                        .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(10.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onView)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("📊 Профиль", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(PsychCritical)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRecommend)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("💬 Рекомендация", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ⑥ Test Results Feed (Glassmorphic)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassTestResultsFeed(
    items: List<RecentTestFeedItem>,
    onViewResult: (RecentTestFeedItem) -> Unit,
    onViewAll: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Последние результаты", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(PsychTeal.copy(0.12f))
                    .border(1.dp, PsychTeal.copy(0.30f), RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onViewAll)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text("Все →", color = PsychTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        items.forEach { item -> GlassTestFeedCard(item = item, onView = { onViewResult(item) }) }
    }
}

@Composable
private fun GlassTestFeedCard(item: RecentTestFeedItem, onView: () -> Unit) {
    val (statusColor, statusLabel) = when (item.studentStatus) {
        "critical" -> PsychCritical to "Критично"
        "stress"   -> PsychWarning  to "Стресс"
        "normal"   -> PsychTeal     to "Норма"
        else       -> TextHint      to "Неизвестно"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.09f), Color.White.copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(statusColor.copy(0.38f), statusColor.copy(0.08f))), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(50.dp).background(statusColor.copy(0.15f), CircleShape).border(2.dp, statusColor.copy(0.50f), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(item.studentName.take(1).uppercase(), color = statusColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.studentName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("завершил(а) психологическую оценку", color = TextSecondary, fontSize = 12.sp)
            Box(
                modifier = Modifier
                    .background(statusColor.copy(0.15f), RoundedCornerShape(6.dp))
                    .border(1.dp, statusColor.copy(0.30f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) { Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PsychTeal.copy(0.15f))
                .border(1.dp, PsychTeal.copy(0.40f), RoundedCornerShape(12.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onView)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) { Text("Смотреть\nрезультат", color = PsychTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ⑦ Pending Interventions Card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassPendingInterventionsCard(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(PsychWarning.copy(0.13f), PsychWarning.copy(0.04f))))
            .border(1.dp, Brush.verticalGradient(listOf(PsychWarning.copy(0.55f), PsychWarning.copy(0.15f))), RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("💬", fontSize = 26.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("$count студент${psySuffix(count)} ожидают рекомендации", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Перейти в раздел «Помощь»", color = PsychWarning, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Text("›", color = PsychWarning, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Loading Skeleton — glass shimmer effect
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassLoadingSkeleton() {
    val shimmerTransition = rememberInfiniteTransition(label = "glass_shimmer")
    val shimX by shimmerTransition.animateFloat(
        initialValue = -700f, targetValue = 2400f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "shimX"
    )
    val glassShimmer = Modifier.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(0.06f), Color.White.copy(0.12f), Color.White.copy(0.06f), Color.Transparent),
                start = Offset(shimX, 0f),
                end = Offset(shimX + 520f, size.height)
            )
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(Triple(180.dp, 24.dp, true), Triple(280.dp, 24.dp, true), Triple(100.dp, 18.dp, false), Triple(80.dp, 16.dp, false)).forEach { (h, r, _) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(h)
                    .clip(RoundedCornerShape(r))
                    .background(Color.White.copy(0.07f))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(r))
                    .then(glassShimmer)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Empty State
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassEmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.09f), Color.White.copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.22f), Color.White.copy(0.06f))), RoundedCornerShape(20.dp))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🧠", fontSize = 44.sp)
        Text(message, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Public shared metric components (used across tabs)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun MiniMetricPill(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    val animVal by animateFloatAsState(value, tween(800, easing = FastOutSlowInEasing), label = "pill_$label")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(0.10f))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${animVal.toInt()}%", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = color.copy(0.70f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun MatteMetricBar(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    val animVal by animateFloatAsState(
        targetValue = (value / 100f).coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "bar_$label"
    )
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(90.dp))
        Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(color.copy(0.15f))) {
            Box(Modifier.fillMaxWidth(animVal).fillMaxHeight().background(Brush.horizontalGradient(listOf(color, color.copy(0.60f))), RoundedCornerShape(4.dp)))
        }
        Text("${value.toInt()}%", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Helpers
// ══════════════════════════════════════════════════════════════════════════════

private fun climateAccentColor(climate: String): Color = when (climate) {
    "critical" -> PsychCritical
    "warning"  -> PsychWarning
    "good"     -> PsychTeal
    else       -> Color(0xFF8A2BE2)
}

private fun climateVerdict(climate: String): String = when (climate) {
    "critical" -> "Обнаружен риск выгорания"
    "warning"  -> "Требует внимания"
    "good"     -> "Состояние в норме"
    else       -> "Данных недостаточно"
}

private fun climateSubText(climate: String): String = when (climate) {
    "critical" -> "Немедленное вмешательство необходимо"
    "warning"  -> "Часть студентов испытывают стресс"
    "good"     -> "Показатели группы в пределах нормы"
    else       -> "Ожидание результатов тестирования"
}

private fun psySuffix(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11         -> ""
    count % 10 in 2..4 && count % 100 !in 12..14 -> "а"
    else                                           -> "ов"
}
