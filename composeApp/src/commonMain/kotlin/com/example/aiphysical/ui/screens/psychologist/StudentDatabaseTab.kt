package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.aiphysical.ui.components.GlassSearchBar
import com.example.aiphysical.ui.theme.*
import kotlin.math.*

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
            onBack = { vm.onEvent(PsychologistEvent.BackToDashboard) },
            onRecommend = { vm.onEvent(PsychologistEvent.OpenRecommendationSheet(state.selectedStudent)) }
        )
    } else {
        StudentListView(state = state, vm = vm, modifier = modifier)
    }
}

// ── Student List ──────────────────────────────────────────────────────────────

@Composable
private fun StudentListView(
    state: PsychologistHomeState,
    vm: PsychologistViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "База студентов",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "${state.students.size} студентов в организации",
                color = TextSecondary,
                fontSize = 13.sp
            )

            GlassSearchBar(
                query = state.searchQuery,
                hint = "Поиск по имени или email...",
                onQueryChange = { vm.onEvent(PsychologistEvent.SearchStudents(it)) }
            )

            // Filter chips
            FilterChipsRow(students = state.students)
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PsychTeal, strokeWidth = 2.dp)
            }
        } else if (state.filteredStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔍", fontSize = 36.sp)
                    Text(
                        if (state.searchQuery.isBlank()) "Студентов пока нет" else "Ничего не найдено",
                        color = TextSecondary, fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.filteredStudents) { student ->
                    StudentCard(
                        student = student,
                        onClick = { vm.onEvent(PsychologistEvent.SelectStudent(student)) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(students: List<UserProfile>) {
    val statusGroups = listOf(
        Triple("Все", students.size, TextPrimary),
        Triple("Критично", students.count { it.latestAiStatus == "critical" }, PsychCritical),
        Triple("Стресс", students.count { it.latestAiStatus == "stress" }, PsychWarning),
        Triple("Норма", students.count { it.latestAiStatus == "normal" }, PsychTeal),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        statusGroups.forEach { (label, count, color) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(0.12f))
                    .border(1.dp, color.copy(0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    "$label ($count)",
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Student Card ──────────────────────────────────────────────────────────────

@Composable
private fun StudentCard(student: UserProfile, onClick: () -> Unit) {
    val healthScore = computeHealthScore(student)
    val (statusColor, _) = statusColorAndEmoji(student.latestAiStatus)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar with status ring
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    Brush.radialGradient(listOf(statusColor.copy(0.30f), MatteSurface)),
                    CircleShape
                )
                .border(2.dp, statusColor.copy(0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                student.fullName.take(1).uppercase(),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Info
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                student.fullName,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (student.ageGroup.isNotBlank()) {
                Text(student.ageGroup, color = TextSecondary, fontSize = 11.sp)
            }
            Text(student.email, color = TextHint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // Health score gauge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HealthScoreCircle(score = healthScore, color = statusColor)
            Text("здоровье", color = TextHint, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun HealthScoreCircle(score: Float, color: Color) {
    val animScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "health_score"
    )
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val stroke = 4.dp.toPx()
            // Background arc
            drawArc(
                color = color.copy(0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (animScore / 100f),
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            "${score.toInt()}%",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

// ── Student Detail View ───────────────────────────────────────────────────────

@Composable
private fun StudentDetailView(
    student: UserProfile,
    testHistory: List<TestResult>,
    isLoading: Boolean,
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
        // Back header
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Профиль студента",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Identity card
        StudentIdentityCard(student = student)

        // Radar chart
        PsychRadarCard(student = student)

        // Test history timeline
        TestHistoryCard(
            testHistory = testHistory,
            isLoading = isLoading
        )

        // Existing recommendation (if any)
        if (student.psychComment.isNotBlank()) {
            ExistingRecommendationCard(student = student)
        }

        // CTA — write recommendation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(PsychTeal.copy(0.25f), PsychTeal.copy(0.10f))
                    )
                )
                .border(1.dp, PsychTeal.copy(0.5f), RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onRecommend
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💬", fontSize = 20.sp)
                Text(
                    if (student.psychComment.isBlank()) "Написать рекомендацию" else "Обновить рекомендацию",
                    color = PsychTeal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StudentIdentityCard(student: UserProfile) {
    val (statusColor, statusEmoji) = statusColorAndEmoji(student.latestAiStatus)
    val statusLabel = when (student.latestAiStatus) {
        "critical" -> "Критично"
        "stress"   -> "Стресс"
        "normal"   -> "Норма"
        else       -> "Неизвестно"
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
        // Big avatar
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
            Text(
                student.fullName,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
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
private fun PsychRadarCard(student: UserProfile) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(student.uid) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
    }
    val progress by animProgress.asState()

    // Radar values (0–1), normalized. Higher = worse for burnout/stress/anxiety; higher = better for emotion/motivation
    val axes = listOf(
        "Выгорание" to student.burnoutScore / 100f,
        "Стресс"   to student.stressScore / 100f,
        "Тревога"  to student.anxietyScore / 100f,
        "Состояние" to student.emotionScore / 100f,
        "Мотивация" to student.motivationScore / 100f,
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
            "ПСИХОЛОГИЧЕСКИЙ ПРОФИЛЬ",
            color = TextHint,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )

        // Radar canvas
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

            // Draw grid rings
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

            // Draw axis lines
            for (i in 0 until n) {
                val angle = -PI.toFloat() / 2 + i * angleStep
                drawLine(
                    color = MatteCardBorder.copy(0.6f),
                    start = Offset(cx, cy),
                    end = Offset(cx + maxR * cos(angle), cy + maxR * sin(angle)),
                    strokeWidth = 1f
                )
            }

            // Draw data polygon (animated)
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
            drawPath(dataPath, PsychTeal.copy(0.8f), style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Draw axis dots
            axes.forEachIndexed { i, (_, v) ->
                val angle = -PI.toFloat() / 2 + i * angleStep
                val r = maxR * v * progress
                drawCircle(axisColors[i], radius = 5f, center = Offset(cx + r * cos(angle), cy + r * sin(angle)))
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            axes.forEachIndexed { i, (label, v) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(8.dp).background(axisColors[i], CircleShape))
                    Spacer(Modifier.height(3.dp))
                    Text(label, color = TextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center)
                    Text("${(v * 100).toInt()}%", color = axisColors[i], fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Test History Timeline ─────────────────────────────────────────────────────

@Composable
private fun TestHistoryCard(
    testHistory: List<TestResult>,
    isLoading: Boolean,
) {
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
            "ИСТОРИЯ ТЕСТОВ",
            color = TextHint,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )

        if (isLoading) {
            repeat(3) {
                val shimmerTransition = rememberInfiniteTransition(label = "shimmer_$it")
                val alpha by shimmerTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 0.7f,
                    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                    label = "s_$it"
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteCardBorder.copy(alpha))
                )
            }
        } else if (testHistory.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📋", fontSize = 28.sp)
                Text("Тесты ещё не проходились", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            testHistory.forEachIndexed { index, result ->
                TestTimelineItem(
                    result = result,
                    isLast = index == testHistory.lastIndex
                )
            }
        }
    }
}

@Composable
private fun TestTimelineItem(result: TestResult, isLast: Boolean) {
    val (color, emoji) = when (result.aiAssessment) {
        "critical" -> PsychCritical to "🔴"
        "stress"   -> PsychWarning  to "⚠️"
        "normal"   -> PsychTeal     to "✅"
        else       -> TextHint      to "❓"
    }
    val label = when (result.aiAssessment) {
        "critical" -> "Критично"
        "stress"   -> "Стресс"
        "normal"   -> "Норма"
        else       -> "Неизвестно"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline indicator
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
                Box(
                    Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(MatteCardBorder)
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
        ) {
            Text(
                result.testName,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .background(color.copy(0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, color.copy(0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Балл: ${result.score.toInt()}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (result.dateMillis > 0L) {
                    Text(
                        formatDate(result.dateMillis),
                        color = TextHint,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ── Existing Recommendation Card ──────────────────────────────────────────────

@Composable
private fun ExistingRecommendationCard(student: UserProfile) {
    val (priorityColor, priorityLabel) = when (student.psychPriority) {
        "HIGH"   -> PsychCritical to "Высокий"
        "MEDIUM" -> PsychWarning  to "Средний"
        "LOW"    -> PsychTeal     to "Низкий"
        else     -> TextHint      to "Не указан"
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
            Text(
                "МОЯ РЕКОМЕНДАЦИЯ",
                color = PsychTeal,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .background(priorityColor.copy(0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, priorityColor.copy(0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Приоритет: $priorityLabel", color = priorityColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(student.psychComment, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
        if (student.assignedCourseName.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("📚", fontSize = 14.sp)
                Text(
                    "Курс: ${student.assignedCourseName}",
                    color = PsychTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun computeHealthScore(profile: UserProfile): Float =
    (100f - (
        profile.burnoutScore * 0.35f +
        profile.stressScore * 0.30f +
        profile.anxietyScore * 0.25f +
        (100f - profile.motivationScore) * 0.10f
    )).coerceIn(0f, 100f)

private fun statusColorAndEmoji(status: String): Pair<Color, String> = when (status) {
    "critical" -> PsychCritical to "🔴"
    "stress"   -> PsychWarning  to "⚠️"
    "normal"   -> PsychTeal     to "✅"
    else       -> TextHint      to "❓"
}

private fun formatDate(millis: Long): String {
    if (millis == 0L) return ""
    // Simple date formatting without platform APIs
    val days = millis / 86400000L
    val epochDays = 25569L // 1970-01-01 in Excel days
    val d = days - epochDays
    return "${d % 30 + 1}.${(d / 30) % 12 + 1}.${1970 + d / 365}"
}

