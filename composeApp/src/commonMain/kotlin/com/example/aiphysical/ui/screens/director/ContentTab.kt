package com.example.aiphysical.ui.screens.director

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.AppCourseCatalog
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.presentation.director.*
import com.example.aiphysical.ui.components.UmiAvatarBadge
import com.example.aiphysical.ui.theme.*

private data class TestInfo(val emoji: String, val name: String, val description: String, val color: Color)

private val mandatoryTests = listOf(
    TestInfo("🔥", "Burnout Inventory (MBI)",  "Измерение уровня профессионального выгорания",   MetricBurnout),
    TestInfo("⚡", "PSS-10 (Stress Scale)",    "Шкала воспринимаемого стресса Коэна",             MetricStress),
    TestInfo("💭", "PHQ-9 (Depression)",       "Опросник здоровья пациента — Депрессия",          Color(0xFFE040FB)),
    TestInfo("😰", "GAD-7 (Anxiety)",          "Обобщённая шкала тревожного расстройства",        MetricAnxiety),
    TestInfo("💪", "WLEIS (Motivation)",        "Шкала эмоционального интеллекта и мотивации",    MetricMotivation),
)

@Composable
fun ContentTab(
    state: DirectorDashboardState,
    vm: DirectorDashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val strings = getStrings(state.currentLanguage)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────────────
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

            // ── Stats chips ────────────────────────────────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ContentStatChip("🧪", "${mandatoryTests.size}", strings.testsTitle, NeonViolet, Modifier.weight(1f))
                    ContentStatChip("📚", "${AppCourseCatalog.baseCourses.size}", strings.coursesTitle, CyanAccent, Modifier.weight(1f))
                    ContentStatChip("➕", "${state.addedCourses.size}", "Добавлено", AlertOrange, Modifier.weight(1f))
                }
            }

            // ── Tests Section ──────────────────────────────────────────────────
            item {
                ContentSectionHeader(
                    emoji = "🧪", title = strings.testsTitle,
                    subtitle = "5 обязательных диагностических тестов", accentColor = NeonViolet
                )
            }
            itemsIndexed(mandatoryTests) { index, test ->
                PremiumTestCard(test = test, statsLabel = strings.viewStats, index = index + 1)
            }

            // ── Base Courses Section ───────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                ContentSectionHeader(
                    emoji = "📚", title = strings.coursesTitle,
                    subtitle = "${AppCourseCatalog.baseCourses.size} базовых курсов платформы", accentColor = CyanAccent
                )
            }
            itemsIndexed(AppCourseCatalog.baseCourses) { _, course ->
                BaseCourseCard(course = course)
            }

            // ── Added Courses Section ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                ContentSectionHeader(
                    emoji = "➕", title = "Добавленные курсы",
                    subtitle = if (state.addedCourses.isEmpty()) "Психолог ещё не добавил курсы"
                               else "${state.addedCourses.size} курс(ов) от психолога",
                    accentColor = AlertOrange
                )
            }
            if (state.addedCourses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(0.03f))
                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📭", fontSize = 36.sp)
                            Text("Новых курсов добавлено не было", color = Color.White.copy(0.4f), fontSize = 14.sp)
                        }
                    }
                }
            } else {
                itemsIndexed(state.addedCourses) { _, course ->
                    DirectorAddedCourseCard(course = course, onClick = { vm.onEvent(DirectorEvent.OpenAddedCourse(course)) })
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }

        // ── Text Course Viewer overlay ─────────────────────────────────────────
        if (state.showTextCourseViewer && state.selectedAddedCourse != null) {
            DirectorTextCourseDialog(
                course = state.selectedAddedCourse!!,
                onDismiss = { vm.onEvent(DirectorEvent.CloseTextCourseViewer) }
            )
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

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

// ─── Stat Chip ────────────────────────────────────────────────────────────────

@Composable
private fun ContentStatChip(emoji: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(0.08f))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp),
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
        Box(
            Modifier.size(46.dp).background(test.color.copy(0.2f), RoundedCornerShape(14.dp)).border(1.dp, test.color.copy(0.5f), RoundedCornerShape(14.dp)),
            Alignment.Center
        ) { Text(test.emoji, fontSize = 18.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(test.color.copy(0.2f), RoundedCornerShape(6.dp)).border(1.dp, test.color.copy(0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                    Text("$index", color = test.color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(test.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(test.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        TextButton(
            onClick = {},
            modifier = Modifier.background(test.color.copy(0.14f), RoundedCornerShape(12.dp)).border(1.dp, test.color.copy(0.35f), RoundedCornerShape(12.dp))
        ) { Text("📊 $statsLabel", color = test.color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
    }
}

// ─── Base Course Card (AppCourseCatalog) ──────────────────────────────────────

@Composable
private fun BaseCourseCard(course: com.example.aiphysical.data.model.BaseCourseCatalogItem) {
    val color = Color(course.accentColorHex)
    val infiniteTransition = rememberInfiniteTransition(label = "base_${course.id}")
    val aiGlow by if (course.isAiRecommended) infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_${course.id}"
    ) else remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (course.isAiRecommended) Modifier.drawBehind {
                drawRoundRect(NeonViolet.copy(alpha = aiGlow * 0.1f), cornerRadius = CornerRadius(22.dp.toPx()))
            } else Modifier)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(0.04f))
            .border(
                if (course.isAiRecommended) 1.5.dp else 1.dp,
                if (course.isAiRecommended) Brush.linearGradient(listOf(NeonViolet.copy(0.6f), CyanAccent.copy(0.5f)))
                else Brush.linearGradient(listOf(Color.White.copy(0.14f), Color.White.copy(0.04f))),
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(Brush.radialGradient(listOf(color.copy(0.4f), CyanAccent.copy(0.2f))), RoundedCornerShape(14.dp)).border(1.dp, color.copy(0.5f), RoundedCornerShape(14.dp)),
                Alignment.Center
            ) { Text(course.emoji, fontSize = 22.sp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(course.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(course.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⏱", fontSize = 12.sp)
                Text(course.durationLabel, color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (course.isAiRecommended) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(NeonViolet.copy(0.2f), CyanAccent.copy(0.12f)))).border(1.dp, Brush.horizontalGradient(listOf(NeonViolet.copy(0.5f), CyanAccent.copy(0.4f))), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                UmiAvatarBadge(
                    modifier = Modifier.size(12.dp),
                    backgroundColor = Color.White.copy(0.06f),
                    borderColor = NeonViolet.copy(0.35f),
                    borderWidth = 0.8.dp,
                    imagePadding = 1.dp,
                    contentDescription = "Уми рекомендует"
                )
                Text("AI рекомендует", style = TextStyle(brush = Brush.horizontalGradient(listOf(NeonViolet, CyanAccent)), fontSize = 10.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ─── Director Added Course Card ───────────────────────────────────────────────

@Composable
private fun DirectorAddedCourseCard(course: OrganizationCourse, onClick: () -> Unit) {
    val typeColor = if (course.type == CourseContentType.VIDEO) AlertOrange else CyanAccent
    val typeLabel = if (course.type == CourseContentType.VIDEO) "Видео" else "Текстовый"
    val typeEmoji = if (course.type == CourseContentType.VIDEO) "🎬" else "📝"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, Brush.horizontalGradient(listOf(typeColor.copy(0.35f), typeColor.copy(0.08f))), RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(46.dp).background(typeColor.copy(0.18f), RoundedCornerShape(14.dp)).border(1.dp, typeColor.copy(0.4f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Text(typeEmoji, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(course.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(typeColor.copy(0.15f), RoundedCornerShape(6.dp)).border(1.dp, typeColor.copy(0.35f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(typeLabel, color = typeColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (course.createdByName.isNotBlank()) Text(course.createdByName, color = Color.White.copy(0.4f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (course.description.isNotBlank()) Text(course.description, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = typeColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Director Text Course Dialog ──────────────────────────────────────────────

@Composable
private fun DirectorTextCourseDialog(course: OrganizationCourse, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050010).copy(0.88f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFF0D0D22))
                    .border(1.dp, Brush.horizontalGradient(listOf(CyanAccent.copy(0.4f), NeonViolet.copy(0.3f))), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.width(48.dp).height(4.dp).background(Color.White.copy(0.12f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                Box(Modifier.background(CyanAccent.copy(0.15f), RoundedCornerShape(8.dp)).border(1.dp, CyanAccent.copy(0.35f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("📝 Текстовый курс", color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(course.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                if (course.description.isNotBlank()) Text(course.description, color = Color.White.copy(0.55f), fontSize = 14.sp, lineHeight = 20.sp)
                HorizontalDivider(color = Color.White.copy(0.08f))
                if (course.contentText.isNotBlank()) {
                    Text(course.contentText, color = Color.White.copy(0.85f), fontSize = 15.sp, lineHeight = 24.sp)
                } else {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.05f)).padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("Текст курса отсутствует", color = Color.White.copy(0.35f), fontSize = 14.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(CyanAccent.copy(0.3f), NeonViolet.copy(0.25f))))
                        .border(1.dp, Brush.horizontalGradient(listOf(CyanAccent.copy(0.6f), NeonViolet.copy(0.5f))), RoundedCornerShape(14.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Закрыть", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
