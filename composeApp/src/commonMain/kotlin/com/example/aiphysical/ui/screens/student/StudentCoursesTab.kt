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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.AppCourseCatalog
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.CourseProgress
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.presentation.student.StudentEvent
import com.example.aiphysical.presentation.student.StudentUiState
import com.example.aiphysical.presentation.student.StudentViewModel
import com.example.aiphysical.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
//  Student Courses Tab — uses AppCourseCatalog as single source of truth
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun StudentCoursesTab(
    state: StudentUiState,
    vm: StudentViewModel,
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
                courseName   = state.profile.assignedCourseName,
                courseId     = state.profile.assignedCourseId,
                progressList = state.courseProgress
            )
        }

        // ── Base courses catalogue (5 canonical courses) ──────────────────────
        Text("КАТАЛОГ КУРСОВ", color = TextHint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.8.sp)

        AppCourseCatalog.baseCourses.forEach { item ->
            val progress = state.courseProgress.find { it.courseId == item.id }?.progress ?: 0f
            CourseCatalogCard(
                emoji       = item.emoji,
                title       = item.title,
                description = item.description,
                duration    = item.durationLabel,
                colorHex    = item.accentColorHex,
                progress    = progress
            )
        }

        // ── Added courses button ──────────────────────────────────────────────
        AddedCoursesButton(
            count   = state.addedCourses.size,
            onClick = { vm.onEvent(StudentEvent.OpenAddedCourses) }
        )

        // ── Added courses inline viewer ───────────────────────────────────────
        if (state.showAddedCoursesViewer) {
            AddedCoursesSection(
                courses  = state.addedCourses,
                onCourse = { vm.onEvent(StudentEvent.OpenAddedCourse(it)) },
                onClose  = { vm.onEvent(StudentEvent.CloseAddedCourses) }
            )
        }
    }
}

// ── Assigned course card ──────────────────────────────────────────────────────

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
                Text(courseName, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
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

// ── Base catalogue card ───────────────────────────────────────────────────────

@Composable
private fun CourseCatalogCard(
    emoji: String,
    title: String,
    description: String,
    duration: String,
    colorHex: Long,
    progress: Float,
) {
    val color = Color(colorHex)
    val animProgress by animateFloatAsState(progress, tween(1000, easing = FastOutSlowInEasing), label = "course_prog_$title")

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
        ) { Text(emoji, fontSize = 24.sp) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (progress > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color.copy(0.15f))) {
                        Box(Modifier.fillMaxWidth(animProgress).fillMaxHeight().background(color, RoundedCornerShape(2.dp)))
                    }
                    Text("${(progress * 100).toInt()}%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(duration, color = TextHint, fontSize = 11.sp)
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

// ── "Added courses" action button ─────────────────────────────────────────────

@Composable
private fun AddedCoursesButton(count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF9D5FF5).copy(0.16f), PsychTeal.copy(0.12f))))
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(Color(0xFF9D5FF5).copy(0.55f), PsychTeal.copy(0.45f))),
                RoundedCornerShape(18.dp)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(Brush.radialGradient(listOf(Color(0xFF9D5FF5).copy(0.35f), PsychTeal.copy(0.15f))), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF9D5FF5).copy(0.4f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Text("➕", fontSize = 20.sp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Добавленные курсы", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (count > 0) "$count курс(ов) от психолога" else "Нажмите, чтобы посмотреть",
                    color = TextSecondary, fontSize = 12.sp
                )
            }
            if (count > 0) {
                Box(
                    Modifier
                        .background(Color(0xFF9D5FF5).copy(0.2f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF9D5FF5).copy(0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("$count", color = Color(0xFF9D5FF5), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Text("›", color = Color(0xFF9D5FF5), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Added courses inline section ──────────────────────────────────────────────

@Composable
private fun AddedCoursesSection(
    courses: List<OrganizationCourse>,
    onCourse: (OrganizationCourse) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0xFF9D5FF5).copy(0.35f), PsychTeal.copy(0.2f))), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ДОБАВЛЕННЫЕ КУРСЫ", color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(0.06f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text("✕ Свернуть", color = TextHint, fontSize = 11.sp) }
        }

        if (courses.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📭", fontSize = 36.sp)
                    Text("Новых курсов добавлено не было", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            courses.forEach { course ->
                AddedCourseCard(course = course, onClick = { onCourse(course) })
            }
        }
    }
}

@Composable
internal fun AddedCourseCard(
    course: OrganizationCourse,
    onClick: () -> Unit,
    showDeleteButton: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    val typeColor = if (course.type == CourseContentType.VIDEO) Color(0xFFFF8C00) else PsychTeal
    val typeLabel = if (course.type == CourseContentType.VIDEO) "Видео" else "Текстовый"
    val typeEmoji = if (course.type == CourseContentType.VIDEO) "🎬" else "📝"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.08f), Color.White.copy(0.03f))))
            .border(1.dp, Brush.verticalGradient(listOf(typeColor.copy(0.35f), typeColor.copy(0.08f))), RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(typeColor.copy(0.18f), RoundedCornerShape(12.dp))
                .border(1.dp, typeColor.copy(0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Text(typeEmoji, fontSize = 20.sp) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(course.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(typeColor.copy(0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, typeColor.copy(0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(typeLabel, color = typeColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (course.createdByName.isNotBlank()) {
                    Text(course.createdByName, color = TextHint, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (course.description.isNotBlank()) {
                Text(course.description, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (showDeleteButton && onDelete != null) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(PsychCritical.copy(0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, PsychCritical.copy(0.4f), RoundedCornerShape(8.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDelete),
                contentAlignment = Alignment.Center
            ) { Text("🗑", fontSize = 14.sp) }
        } else {
            Text("›", color = typeColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
