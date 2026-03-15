package com.example.aiphysical.ui.screens.director

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.director.*
import com.example.aiphysical.ui.theme.*

private data class TestInfo(val emoji: String, val name: String, val description: String, val color: Color)
private data class CourseInfo(val emoji: String, val name: String, val description: String, val duration: String, val isAiRecommended: Boolean, val completionRate: Int)

private val mandatoryTests = listOf(
    TestInfo("🔥", "Burnout Inventory (MBI)",  "Измерение уровня профессионального выгорания",   MetricBurnout),
    TestInfo("⚡", "PSS-10 (Stress Scale)",    "Шкала воспринимаемого стресса Коэна",             MetricStress),
    TestInfo("💭", "PHQ-9 (Depression)",       "Опросник здоровья пациента — Депрессия",          Color(0xFFE040FB)),
    TestInfo("😰", "GAD-7 (Anxiety)",          "Обобщённая шкала тревожного расстройства",        MetricAnxiety),
    TestInfo("💪", "WLEIS (Motivation)",        "Шкала эмоционального интеллекта и мотивации",    MetricMotivation),
)

private val courseLibrary = listOf(
    CourseInfo("🧘", "Управление стрессом",      "Техники снижения стресса и тревожности",        "45 мин", true,  78),
    CourseInfo("🌱", "Ментальный иммунитет",     "Развитие психологической устойчивости",          "60 мин", true,  65),
    CourseInfo("🤝", "Командная динамика",        "Улучшение командного взаимодействия",           "30 мин", false, 42),
    CourseInfo("💡", "Эмоциональный интеллект",   "Понимание и управление эмоциями",               "50 мин", true,  83),
    CourseInfo("🌙", "Гигиена сна",              "Улучшение качества сна и отдыха",                "25 мин", false, 31),
)

@Composable
fun ContentTab(
    state: DirectorDashboardState,
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)

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
                    .background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.12f), CyanAccent.copy(0.06f))))
                    .border(1.dp, Brush.horizontalGradient(listOf(NeonViolet.copy(0.4f), CyanAccent.copy(0.2f))), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    strings.tabContent,
                    style = TextStyle(brush = Brush.horizontalGradient(listOf(NeonViolet, CyanAccent)), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                )
                Text("${strings.testsTitle} & ${strings.coursesTitle}", color = Color.White.copy(0.4f), fontSize = 11.sp)
            }
        }

        // ── Stats chips ───────────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContentStatChip("🧪", "${mandatoryTests.size}", strings.testsTitle, NeonViolet, Modifier.weight(1f))
                ContentStatChip("📚", "${courseLibrary.size}", strings.coursesTitle, CyanAccent, Modifier.weight(1f))
                ContentStatChip("🤖", "${courseLibrary.count { it.isAiRecommended }}", strings.aiRecommended, AlertOrange, Modifier.weight(1f))
            }
        }

        // ── Tests Section ─────────────────────────────────────────────────────
        item {
            ContentSectionHeader(
                emoji = "🧪",
                title = strings.testsTitle,
                subtitle = "5 обязательных диагностических тестов",
                accentColor = NeonViolet
            )
        }

        itemsIndexed(mandatoryTests) { index, test ->
            PremiumTestCard(test = test, statsLabel = strings.viewStats, index = index + 1)
        }

        // ── Courses Section ───────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(4.dp))
            ContentSectionHeader(
                emoji = "📚",
                title = strings.coursesTitle,
                subtitle = "${courseLibrary.size} курсов · ${courseLibrary.count { it.isAiRecommended }} рекомендованы AI",
                accentColor = CyanAccent
            )
        }

        itemsIndexed(courseLibrary) { _, course ->
            PremiumCourseCard(course = course, aiRecommendedLabel = strings.aiRecommended)
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

// ─── Content Section Header ───────────────────────────────────────────────────

@Composable
private fun ContentSectionHeader(emoji: String, title: String, subtitle: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(0.08f))
            .border(1.dp, accentColor.copy(0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(36.dp).background(accentColor.copy(0.2f), RoundedCornerShape(10.dp)).border(1.dp, accentColor.copy(0.4f), RoundedCornerShape(10.dp)),
            Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = accentColor.copy(0.7f), fontSize = 11.sp)
        }
    }
}

// ─── Content Stat Chip ────────────────────────────────────────────────────────

@Composable
private fun ContentStatChip(emoji: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(0.08f)).border(1.dp, color.copy(0.25f), RoundedCornerShape(14.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = color.copy(0.7f), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

// ─── Premium Test Card ────────────────────────────────────────────────────────

@Composable
private fun PremiumTestCard(test: TestInfo, statsLabel: String, index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(test.color.copy(0.12f), Color.White.copy(0.04f))))
            .border(1.dp, Brush.horizontalGradient(listOf(test.color.copy(0.4f), Color.White.copy(0.06f))), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Index + icon badge
        Box(
            Modifier.size(46.dp)
                .background(test.color.copy(0.2f), RoundedCornerShape(14.dp))
                .border(1.dp, test.color.copy(0.5f), RoundedCornerShape(14.dp)),
            Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(test.emoji, fontSize = 18.sp)
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.background(test.color.copy(0.2f), RoundedCornerShape(6.dp)).border(1.dp, test.color.copy(0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 2.dp)
                ) { Text("$index", color = test.color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
                Text(test.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(test.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }

        TextButton(
            onClick = { /* stats */ },
            modifier = Modifier
                .background(test.color.copy(0.14f), RoundedCornerShape(12.dp))
                .border(1.dp, test.color.copy(0.35f), RoundedCornerShape(12.dp))
        ) {
            Text("📊 $statsLabel", color = test.color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Premium Course Card ──────────────────────────────────────────────────────

@Composable
private fun PremiumCourseCard(course: CourseInfo, aiRecommendedLabel: String) {
    val animProgress by animateFloatAsState(
        targetValue = course.completionRate / 100f,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "course_${course.name}"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "ai_${course.name}")
    val aiGlow by if (course.isAiRecommended) infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ai_glow"
    ) else remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (course.isAiRecommended) Modifier.drawBehind {
                    drawRoundRect(NeonViolet.copy(alpha = aiGlow * 0.1f), cornerRadius = CornerRadius(22.dp.toPx()))
                } else Modifier
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(0.04f))
            .border(
                if (course.isAiRecommended) 1.5.dp else 1.dp,
                if (course.isAiRecommended) Brush.linearGradient(listOf(NeonViolet.copy(0.6f), CyanAccent.copy(0.5f)))
                else Brush.linearGradient(listOf(Color.White.copy(0.14f), Color.White.copy(0.04f))),
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Course icon
                Box(
                    Modifier.size(44.dp)
                        .background(Brush.radialGradient(listOf(NeonViolet.copy(0.4f), CyanAccent.copy(0.25f))), RoundedCornerShape(14.dp))
                        .border(1.dp, if (course.isAiRecommended) NeonViolet.copy(0.5f) else Color.White.copy(0.12f), RoundedCornerShape(14.dp)),
                    Alignment.Center
                ) { Text(course.emoji, fontSize = 22.sp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(course.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(course.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }

        // Progress bar + completion rate
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Прохождение", color = Color.White.copy(0.4f), fontSize = 10.sp)
                Text("${course.completionRate}%", color = if (course.isAiRecommended) NeonViolet else CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color.White.copy(0.07f))) {
                Box(
                    Modifier.fillMaxWidth(animProgress).fillMaxHeight().clip(CircleShape)
                        .background(
                            if (course.isAiRecommended) Brush.horizontalGradient(listOf(NeonViolet, CyanAccent.copy(0.7f)))
                            else Brush.horizontalGradient(listOf(CyanAccent, CyanAccent.copy(0.5f)))
                        )
                )
            }
        }

        // Footer row
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⏱", fontSize = 12.sp)
                Text(course.duration, color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (course.isAiRecommended) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.2f), CyanAccent.copy(0.12f))))
                        .border(1.dp, Brush.horizontalGradient(listOf(NeonViolet.copy(0.5f), CyanAccent.copy(0.4f))), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🤖", fontSize = 10.sp)
                    Text(
                        aiRecommendedLabel,
                        style = TextStyle(brush = Brush.horizontalGradient(listOf(NeonViolet, CyanAccent)), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
