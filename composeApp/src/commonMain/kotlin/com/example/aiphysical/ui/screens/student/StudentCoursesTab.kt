package com.example.aiphysical.ui.screens.student

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.CourseProgress
import com.example.aiphysical.presentation.student.StudentUiState
import com.example.aiphysical.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
//  Student Courses Tab
// ══════════════════════════════════════════════════════════════════════════════

private data class CourseTemplate(
    val emoji: String,
    val title: String,
    val description: String,
    val totalLessons: Int,
    val colorHex: Long
)

private val courseCatalog = listOf(
    CourseTemplate("🧘", "Управление стрессом",        "Техники релаксации и снижения тревожности",    6, 0xFF00CED1),
    CourseTemplate("💡", "Осознанность и медитация",   "Практики внимательности для каждого дня",       8, 0xFF9D5FF5),
    CourseTemplate("⚡", "Энергия и мотивация",        "Как восстановить ресурс и найти цель",          5, 0xFF4FD18A),
    CourseTemplate("🛡️", "Эмоциональный интеллект",   "Управляй своими эмоциями и реакциями",          7, 0xFFFF8C00),
    CourseTemplate("🌙", "Здоровый сон и восстановление", "Режим дня и качество отдыха",               4, 0xFFE040FB),
    CourseTemplate("🔥", "Преодоление выгорания",      "Диагностика и профилактика выгорания",          6, 0xFFFF5370),
)

@Composable
fun StudentCoursesTab(
    state: StudentUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 28.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("КУРСЫ", color = TextHint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)
            Text("Активные курсы", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        }

        // Assigned course (from psychologist)
        if (state.profile.assignedCourseName.isNotBlank()) {
            AssignedCourseCard(
                courseName = state.profile.assignedCourseName,
                courseId   = state.profile.assignedCourseId,
                progressList = state.courseProgress
            )
        }

        // Course catalogue
        Text("КАТАЛОГ КУРСОВ", color = TextHint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)

        courseCatalog.forEach { template ->
            val progress = state.courseProgress.find { it.courseId == template.title }?.progress ?: 0f
            CourseCatalogCard(template = template, progress = progress)
        }
    }
}

@Composable
private fun AssignedCourseCard(
    courseName: String,
    courseId: String,
    progressList: List<CourseProgress>,
) {
    val progress = progressList.find { it.courseId == courseId }?.progress ?: 0f
    val animProgress by animateFloatAsState(progress, tween(1200, easing = FastOutSlowInEasing), label = "assigned_progress")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(PsychTeal.copy(0.14f), PsychTeal.copy(0.04f))))
            .border(1.5.dp, Brush.horizontalGradient(listOf(PsychTeal.copy(0.7f), PsychTeal.copy(0.2f))), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(40.dp).background(PsychTeal.copy(0.2f), RoundedCornerShape(12.dp)).border(1.dp, PsychTeal.copy(0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text("🎯", fontSize = 18.sp) }
            Column(Modifier.weight(1f)) {
                Text("НАЗНАЧЕН ПСИХОЛОГОМ", color = PsychTeal.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                Text(courseName, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }

        // Progress bar
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Прогресс", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Text("${(progress * 100).toInt()}%", color = PsychTeal, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(PsychTeal.copy(0.15f))) {
                Box(Modifier.fillMaxWidth(animProgress).fillMaxHeight().background(Brush.horizontalGradient(listOf(PsychTeal, PsychTeal.copy(0.7f))), RoundedCornerShape(4.dp)))
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PsychTeal)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (progress > 0f) "Продолжить курс →" else "Начать курс →",
                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CourseCatalogCard(template: CourseTemplate, progress: Float) {
    val color = Color(template.colorHex)
    val animProgress by animateFloatAsState(progress, tween(1000, easing = FastOutSlowInEasing), label = "course_prog_${template.title}")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.09f), Color.White.copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(color.copy(0.35f), color.copy(0.08f))), RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .background(Brush.radialGradient(listOf(color.copy(0.40f), color.copy(0.10f))), RoundedCornerShape(14.dp))
                .border(1.dp, color.copy(0.4f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Text(template.emoji, fontSize = 24.sp) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(template.title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(template.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)

            if (progress > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color.copy(0.15f))) {
                        Box(Modifier.fillMaxWidth(animProgress).fillMaxHeight().background(color, RoundedCornerShape(2.dp)))
                    }
                    Text("${(progress * 100).toInt()}%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("${template.totalLessons} уроков", color = TextHint, fontSize = 11.sp)
            }
        }

        Box(
            Modifier
                .size(34.dp)
                .background(color.copy(0.15f), CircleShape)
                .border(1.dp, color.copy(0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (progress > 0f) "▶" else "+", color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

