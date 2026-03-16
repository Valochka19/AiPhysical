package com.example.aiphysical.ui.screens.student

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.AppCourseCatalog
import com.example.aiphysical.presentation.student.StudentEvent
import com.example.aiphysical.presentation.student.StudentTestType
import com.example.aiphysical.presentation.student.StudentUiState
import com.example.aiphysical.presentation.student.StudentViewModel
import com.example.aiphysical.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ══════════════════════════════════════════════════════════════════════════════
//  Student Home Tab
//  Layout: Greeting → Test Carousel → Health Indicator → Psychologist Msg → AI Courses
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun StudentHomeTab(
    state: StudentUiState,
    vm: StudentViewModel,
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
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // ① Greeting header
        StudentGreetingHeader(
            name = state.profile.fullName,
            status = state.profile.latestAiStatus,
            onLogout = onLogout
        )

        if (state.isLoading) {
            StudentShimmerSkeleton()
        } else {
            // ② Test Carousel ("Stories" style)
            TestCarouselSection(
                completedIds = state.completedTestIds,
                onTestClick = { vm.onEvent(StudentEvent.StartTest(it)) }
            )

            // ③ Overall health indicator
            OverallHealthCard(
                score = state.overallScore,
                status = state.profile.latestAiStatus,
                onGenerateReport = { vm.onEvent(StudentEvent.GenerateReport) }
            )

            // ④ Psychologist recommendation (only if present)
            if (state.profile.psychComment.isNotBlank()) {
                PsychologistMessageCard(
                    comment  = state.profile.psychComment,
                    priority = state.profile.psychPriority,
                    courseName = state.profile.assignedCourseName,
                    onCoursesClick = { vm.onEvent(StudentEvent.NavigateToTab(com.example.aiphysical.presentation.student.StudentTab.Courses)) }
                )
            }

            // ⑤ AI course recommendations
            AiCourseRecommendationsSection(
                assignedCourseName = state.profile.assignedCourseName,
                onViewCourses = { vm.onEvent(StudentEvent.NavigateToTab(com.example.aiphysical.presentation.student.StudentTab.Courses)) }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ① Greeting Header
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StudentGreetingHeader(name: String, status: String, onLogout: () -> Unit) {
    val firstName = name.split(" ").firstOrNull() ?: name
    val orbColor = statusToColor(status)

    val infiniteTransition = rememberInfiniteTransition(label = "student_orb")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI Orb
        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(68.dp)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(orbColor.copy(glowAlpha * 0.7f), orbColor.copy(0.1f), Color.Transparent),
                        radius = size.minDimension / 2f
                    )
                )
            }
            Canvas(Modifier.size(56.dp).graphicsLayer { rotationZ = rotation }) {
                drawArc(
                    Brush.sweepGradient(listOf(orbColor, orbColor.copy(0.3f), Color.Transparent, Color.Transparent)),
                    startAngle = 0f, sweepAngle = 210f, useCenter = false,
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Box(
                Modifier
                    .size(42.dp)
                    .background(
                        Brush.radialGradient(listOf(orbColor.copy(0.7f), orbColor.copy(0.25f), Color(0xFF080B1A).copy(0.9f))),
                        CircleShape
                    )
                    .border(1.5.dp, Brush.verticalGradient(listOf(orbColor.copy(0.9f), orbColor.copy(0.3f))), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🎓", fontSize = 18.sp) }
        }

        // Text
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Привет,", color = TextSecondary, fontSize = 13.sp)
            Text(firstName, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
            val (statusEmoji, statusText, statusColor) = when (status) {
                "critical" -> Triple("🔴", "Требует внимания", PsychCritical)
                "stress"   -> Triple("🟡", "Небольшой стресс",  PsychWarning)
                "normal"   -> Triple("🟢", "Всё хорошо",        PsychTeal)
                else       -> Triple("⚪", "Пройди тест",       TextHint)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(statusEmoji, fontSize = 10.sp)
                Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Logout
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(0.06f))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.18f), Color.White.copy(0.04f))), RoundedCornerShape(12.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onLogout)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) { Text("Выйти", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ② Test Carousel — "Astana Hub Stories" style
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TestCarouselSection(
    completedIds: Set<String>,
    onTestClick: (StudentTestType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "📋 Психологические тесты",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(StudentTestType.entries) { test ->
                TestStoryCard(
                    test = test,
                    isCompleted = test.testId in completedIds,
                    onClick = { onTestClick(test) }
                )
            }
        }
    }
}

@Composable
private fun TestStoryCard(
    test: StudentTestType,
    isCompleted: Boolean,
    onClick: () -> Unit,
) {
    val colorStart = Color(test.colorStartHex)
    val colorEnd   = Color(test.colorEndHex)

    val infiniteTransition = rememberInfiniteTransition(label = "story_${test.testId}")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "border_${test.testId}"
    )

    Box(
        modifier = Modifier
            .width(108.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(22.dp))
            // Card background: gradient fill
            .background(
                Brush.verticalGradient(
                    listOf(colorStart.copy(0.32f), colorEnd.copy(0.20f), Color.White.copy(0.04f))
                )
            )
            // Neon glow border (animated)
            .border(
                width = if (isCompleted) 2.dp else 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        colorStart.copy(if (isCompleted) 1f else borderAlpha),
                        colorEnd.copy(if (isCompleted) 0.8f else borderAlpha * 0.5f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner card content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            // Icon box (like Astana Hub card icons)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(colorStart.copy(0.50f), colorEnd.copy(0.30f))
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .border(1.dp, colorStart.copy(0.40f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(test.emoji, fontSize = 30.sp)
            }
            Text(
                test.label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 14.sp
            )
        }

        // Completed checkmark badge
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
                    .background(PsychTeal, CircleShape)
                    .border(1.5.dp, Color.White.copy(0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ③ Overall Health Indicator + Generate Report CTA
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OverallHealthCard(
    score: Float,
    status: String,
    onGenerateReport: () -> Unit,
) {
    val accentColor = statusToColor(status)
    val hasData = score > 0f
    val displayScore = if (hasData) score else 50f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.03f))))
            .background(accentColor.copy(0.06f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(accentColor.copy(0.55f), Color.White.copy(0.12f), accentColor.copy(0.15f))),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "ТВОЁ МЕНТАЛЬНОЕ СОСТОЯНИЕ",
            color = TextHint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.8.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular indicator
            HealthCircularIndicator(
                score = displayScore,
                color = accentColor,
                hasData = hasData,
                modifier = Modifier.size(120.dp)
            )

            // Right side info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val (label, description) = when (status) {
                    "critical" -> "Критическое\nсостояние" to "Необходима помощь психолога"
                    "stress"   -> "Повышенный\nстресс"     to "Рекомендуем пройти курсы"
                    "normal"   -> "Норма"                  to "Показатели в пределах нормы"
                    else       -> "Нет данных"             to "Пройди тест для оценки"
                }
                Text(
                    label,
                    color = accentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp
                )
                Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, lineHeight = 18.sp)

                // Mini metrics row
                if (hasData) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniScoreDot("😊", accentColor)
                        MiniScoreDot("🚀", PsychTeal)
                        MiniScoreDot("☁️", MetricAnxiety)
                    }
                }
            }
        }

        HorizontalDivider(color = accentColor.copy(0.15f))

        // Generate Report CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF8A2BE2).copy(0.20f), PsychTeal.copy(0.20f))))
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(Color(0xFF9D5FF5).copy(0.80f), PsychTeal.copy(0.80f))),
                    RoundedCornerShape(14.dp)
                )
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onGenerateReport)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(30.dp)
                        .background(Color(0xFF8A2BE2).copy(0.25f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF9D5FF5).copy(0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🤖", fontSize = 14.sp) }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Сгенерировать общий отчёт", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text("AI-анализ твоего состояния", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun HealthCircularIndicator(
    score: Float,
    color: Color,
    hasData: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(score, hasData) {
        animatable.snapTo(0f)
        if (hasData) animatable.animateTo(score / 100f, tween(1400, easing = FastOutSlowInEasing))
    }
    val progress by animatable.asState()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 10.dp.toPx()
            val inset   = strokeW / 2f
            val arcSize = Size(size.width - strokeW, size.height - strokeW)
            val offset  = Offset(inset, inset)

            // Track (background ring)
            drawArc(
                color = color.copy(0.12f),
                startAngle = -225f, sweepAngle = 270f,
                useCenter = false, topLeft = offset, size = arcSize,
                style = Stroke(strokeW, cap = StrokeCap.Round)
            )

            // Glow layer (slightly thicker, lower alpha)
            if (hasData && progress > 0f) {
                drawArc(
                    color = color.copy(0.25f),
                    startAngle = -225f, sweepAngle = 270f * progress,
                    useCenter = false, topLeft = offset, size = arcSize,
                    style = Stroke(strokeW + 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Filled arc
            if (hasData && progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(color.copy(0.7f), color, color.copy(0.8f))
                    ),
                    startAngle = -225f, sweepAngle = 270f * progress,
                    useCenter = false, topLeft = offset, size = arcSize,
                    style = Stroke(strokeW, cap = StrokeCap.Round)
                )
            }

            // Vertex dot at progress end
            if (hasData && progress > 0.02f) {
                val angle = (-225f + 270f * progress) * (PI / 180).toFloat()
                val cx    = size.width / 2f
                val cy    = size.height / 2f
                val r     = (size.minDimension - strokeW) / 2f
                drawCircle(color, radius = 5.dp.toPx(), center = Offset(cx + r * cos(angle), cy + r * sin(angle)))
                drawCircle(color.copy(0.3f), radius = 9.dp.toPx(), center = Offset(cx + r * cos(angle), cy + r * sin(angle)))
            }
        }

        // Centre text
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                if (hasData) "${score.toInt()}%" else "—",
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text("из 100", color = TextHint, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MiniScoreDot(emoji: String, color: Color) {
    Box(
        Modifier
            .size(26.dp)
            .background(color.copy(0.15f), CircleShape)
            .border(1.dp, color.copy(0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text(emoji, fontSize = 12.sp) }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ④ Psychologist Recommendation Card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PsychologistMessageCard(
    comment: String,
    priority: String,
    courseName: String,
    onCoursesClick: () -> Unit,
) {
    val (priorityColor, priorityLabel) = when (priority.uppercase()) {
        "HIGH"   -> PsychCritical to "🔴 Высокий приоритет"
        "MEDIUM" -> PsychWarning  to "🟡 Средний приоритет"
        "LOW"    -> PsychTeal     to "🟢 Низкий приоритет"
        else     -> TextHint      to "📩 Сообщение"
    }

    val pulse = rememberInfiniteTransition(label = "msg_pulse")
    val borderAlpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "msg_border"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(priorityColor.copy(0.12f), priorityColor.copy(0.04f))))
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(priorityColor.copy(borderAlpha), priorityColor.copy(borderAlpha * 0.3f))),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(40.dp)
                    .background(priorityColor.copy(0.2f), CircleShape)
                    .border(1.dp, priorityColor.copy(0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🧠", fontSize = 18.sp) }
            Column(Modifier.weight(1f)) {
                Text(
                    "СООБЩЕНИЕ ОТ ПСИХОЛОГА",
                    color = priorityColor.copy(0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
                Text(priorityLabel, color = priorityColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = priorityColor.copy(0.15f))

        Text(
            comment,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp
        )

        if (courseName.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(priorityColor.copy(0.10f))
                    .border(1.dp, priorityColor.copy(0.30f), RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCoursesClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📚", fontSize = 16.sp)
                Column(Modifier.weight(1f)) {
                    Text("Рекомендованный курс", color = priorityColor.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(courseName, color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("→", color = priorityColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ⑤ AI Course Recommendations Section
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AiCourseRecommendationsSection(
    assignedCourseName: String,
    onViewCourses: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🤖", fontSize = 18.sp)
                Text("ИИ рекомендует вам:", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(PsychTeal.copy(0.12f))
                    .border(1.dp, PsychTeal.copy(0.30f), RoundedCornerShape(10.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onViewCourses)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) { Text("Все →", color = PsychTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }

        // Show assigned course first (if any)
        if (assignedCourseName.isNotBlank()) {
            AiCourseChip("🎯", assignedCourseName, "Назначен психологом", PsychTeal, onClick = onViewCourses)
        }

        // Always show first 3 canonical base courses as suggestions
        AppCourseCatalog.baseCourses.take(3).forEach { item ->
            AiCourseChip(item.emoji, item.title, item.durationLabel, Color(item.accentColorHex), onClick = onViewCourses)
        }
    }
}

@Composable
private fun AiCourseChip(
    emoji: String,
    name: String,
    meta: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.09f), Color.White.copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(color.copy(0.35f), color.copy(0.08f))), RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(color.copy(0.18f), RoundedCornerShape(10.dp))
                .border(1.dp, color.copy(0.4f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, color = TextHint, fontSize = 11.sp)
        }
        Text("›", color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Loading skeleton
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StudentShimmerSkeleton() {
    val shimmer = rememberInfiniteTransition(label = "student_shimmer")
    val shimX by shimmer.animateFloat(
        initialValue = -700f, targetValue = 2400f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "shimX"
    )
    val shimmerMod = Modifier.drawWithContent {
        drawContent()
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(0.07f), Color.White.copy(0.13f), Color.White.copy(0.07f), Color.Transparent),
                start = Offset(shimX, 0f), end = Offset(shimX + 500f, size.height)
            )
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Carousel skeleton
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) {
                Box(Modifier.width(108.dp).height(150.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(0.07f)).then(shimmerMod))
            }
        }
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(0.07f)).then(shimmerMod))
        Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.07f)).then(shimmerMod))
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

internal fun statusToColor(status: String): Color = when (status) {
    "critical" -> PsychCritical
    "stress"   -> PsychWarning
    "normal"   -> PsychTeal
    else       -> Color(0xFF8A2BE2)
}

