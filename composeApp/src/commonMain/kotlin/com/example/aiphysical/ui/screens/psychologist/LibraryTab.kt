package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.psychologist.PsychologistHomeState
import com.example.aiphysical.ui.theme.*

// ── 5 Mandatory Tests ─────────────────────────────────────────────────────────

private val MANDATORY_TESTS = listOf(
    Triple("test_burnout",   "🔥", "Тест на выгорание (Маслах)"),
    Triple("test_stress",    "⚡", "Шкала стресса PSS-10"),
    Triple("test_anxiety",   "🌀", "Опросник тревожности GAD-7"),
    Triple("test_emotion",   "💭", "Тест эмоционального истощения"),
    Triple("test_balance",   "⚖️", "Баланс работа / жизнь"),
)

// ── 5 Pre-loaded Courses ──────────────────────────────────────────────────────

private val LIBRARY_COURSES = listOf(
    Triple("course_anxiety",     "🧘", "Управление тревогой"),
    Triple("course_stress",      "🌊", "Техники снижения стресса"),
    Triple("course_burnout",     "🛡️", "Профилактика выгорания"),
    Triple("course_mindfulness", "🌿", "Осознанность и медитация"),
    Triple("course_resilience",  "💪", "Построение устойчивости"),
)

@Composable
fun LibraryTab(
    state: PsychologistHomeState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Библиотека",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Тесты и курсы платформы KASU",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // ── Psychologist profile info ──────────────────────────────────────────
        PsychProfileCard(state = state)

        // ── Tests Section ──────────────────────────────────────────────────────
        LibrarySection(title = "5 ОБЯЗАТЕЛЬНЫХ ТЕСТОВ", emoji = "📋") {
            MANDATORY_TESTS.forEachIndexed { index, (_, emoji, name) ->
                TestLibraryItem(
                    index = index + 1,
                    emoji = emoji,
                    name = name,
                    isLast = index == MANDATORY_TESTS.lastIndex
                )
            }
        }

        // ── Courses Section ────────────────────────────────────────────────────
        LibrarySection(title = "5 БАЗОВЫХ КУРСОВ", emoji = "📚") {
            LIBRARY_COURSES.forEachIndexed { index, (courseId, emoji, name) ->
                val assignedCount = state.students.count { it.assignedCourseId == courseId }
                CourseLibraryItem(
                    emoji = emoji,
                    name = name,
                    assignedCount = assignedCount,
                    isLast = index == LIBRARY_COURSES.lastIndex
                )
            }
        }

        // ── Stats overview ─────────────────────────────────────────────────────
        LibraryStatsCard(state = state)
    }
}

// ── Psychologist Profile Card ─────────────────────────────────────────────────

@Composable
private fun PsychProfileCard(state: PsychologistHomeState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    Brush.radialGradient(listOf(PsychTeal.copy(0.30f), MatteSurface)),
                    CircleShape
                )
                .border(2.dp, PsychTeal.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                state.psychologistName.take(2).uppercase(),
                color = PsychTeal,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                state.psychologistName,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Box(
                modifier = Modifier
                    .background(PsychTeal.copy(0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, PsychTeal.copy(0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    "Психолог",
                    color = PsychTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                "${state.students.size} студентов в организации",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// ── Library Section wrapper ───────────────────────────────────────────────────

@Composable
private fun LibrarySection(
    title: String,
    emoji: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Section header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(
                title,
                color = TextHint,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            )
        }
        content()
    }
}

// ── Test Item ─────────────────────────────────────────────────────────────────

@Composable
private fun TestLibraryItem(index: Int, emoji: String, name: String, isLast: Boolean) {
    val accentColors = listOf(MetricBurnout, MetricStress, MetricAnxiety, MetricEmotion, MetricMotivation)
    val color = accentColors.getOrElse(index - 1) { PsychTeal }

    var expanded by remember { mutableStateOf(false) }
    val descriptions = listOf(
        "Оценивает три компонента выгорания: эмоциональное истощение, деперсонализацию и личностные достижения. 22 вопроса.",
        "Шкала воспринимаемого стресса из 10 вопросов. Оценивает уровень стресса за последний месяц.",
        "Опросник тревожного расстройства из 7 вопросов. Широко используется в клинической практике.",
        "Оценивает степень эмоционального истощения и burnout через анализ чувств и реакций.",
        "Анализирует баланс между профессиональной деятельностью и личной жизнью студента."
    )
    val desc = descriptions.getOrElse(index - 1) { "" }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = !expanded }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(color.copy(0.15f), CircleShape)
                    .border(1.dp, color.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$index", color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(emoji, fontSize = 16.sp)
            Text(
                name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (expanded) "▲" else "▼",
                color = TextHint,
                fontSize = 10.sp
            )
        }

        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 50.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(0.07f))
                    .border(1.dp, color.copy(0.2f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(desc, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }

        if (!isLast) {
            HorizontalDivider(
                color = MatteCardBorder,
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}

// ── Course Item ───────────────────────────────────────────────────────────────

@Composable
private fun CourseLibraryItem(
    emoji: String,
    name: String,
    assignedCount: Int,
    isLast: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Emoji badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(PsychTeal.copy(0.12f), CircleShape)
                    .border(1.dp, PsychTeal.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 16.sp)
            }

            Text(
                name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            if (assignedCount > 0) {
                Box(
                    modifier = Modifier
                        .background(PsychTeal.copy(0.15f), RoundedCornerShape(10.dp))
                        .border(1.dp, PsychTeal.copy(0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "$assignedCount назн.",
                        color = PsychTeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!isLast) {
            HorizontalDivider(
                color = MatteCardBorder,
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}

// ── Library Stats Card ────────────────────────────────────────────────────────

@Composable
private fun LibraryStatsCard(state: PsychologistHomeState) {
    val totalAssigned = state.students.count { it.assignedCourseId.isNotBlank() }
    val totalCommented = state.students.count { it.psychComment.isNotBlank() }
    val criticalCount = state.criticalStudents.size

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
            "МОЯ РАБОТА",
            color = TextHint,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WorkStatItem(
                value = totalCommented.toString(),
                label = "Рекоменд.",
                color = PsychTeal
            )
            WorkStatItem(
                value = totalAssigned.toString(),
                label = "Курсов назн.",
                color = NeonViolet
            )
            WorkStatItem(
                value = criticalCount.toString(),
                label = "Критичных",
                color = PsychCritical
            )
            WorkStatItem(
                value = state.students.size.toString(),
                label = "Всего студ.",
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun WorkStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

