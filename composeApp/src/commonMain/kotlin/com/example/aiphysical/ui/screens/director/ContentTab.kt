package com.example.aiphysical.ui.screens.director

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.AppCourseCatalog
import com.example.aiphysical.data.model.AppStudentTestCatalog
import com.example.aiphysical.data.model.BaseCourseCompletionDetails
import com.example.aiphysical.data.model.CourseCompletionMember
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.data.model.OrganizationTestStats
import com.example.aiphysical.data.model.assessmentLabel
import com.example.aiphysical.data.model.displayDescription
import com.example.aiphysical.data.model.displayTitle
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.presentation.director.DirectorDashboardState
import com.example.aiphysical.presentation.director.DirectorDashboardViewModel
import com.example.aiphysical.presentation.director.DirectorEvent
import com.example.aiphysical.ui.components.OrganizationCourseCard
import com.example.aiphysical.ui.components.PlatformCourseCard
import com.example.aiphysical.ui.theme.*

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
                    Text(strings.tabContent, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                    Text("${strings.testsTitle} & ${strings.coursesTitle}", color = Color.White.copy(0.4f), fontSize = 11.sp)
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ContentStatChip("🧪", "${AppStudentTestCatalog.items.size}", strings.testsTitle, NeonViolet, Modifier.weight(1f))
                    ContentStatChip("📚", "${AppCourseCatalog.baseCourses.size}", strings.coursesTitle, CyanAccent, Modifier.weight(1f))
                    ContentStatChip("➕", "${state.addedCourses.size}", state.currentLanguage.pick("Добавлено", "Added", "Қосылды"), AlertOrange, Modifier.weight(1f))
                }
            }

            item {
                ContentSectionHeader(
                    emoji = "🧪",
                    title = strings.testsTitle,
                    subtitle = state.currentLanguage.pick(
                        "Реальные тесты студента и статистика по организации",
                        "Real student tests and organization statistics",
                        "Студенттердің нақты тесттері және ұйым статистикасы"
                    ),
                    accentColor = NeonViolet
                )
            }
            itemsIndexed(AppStudentTestCatalog.items) { index, item ->
                val stats = state.testStats.firstOrNull { it.testType == item.type }
                DirectorTestCard(
                    index = index + 1,
                    emoji = item.emoji,
                    name = item.displayTitle(state.currentLanguage),
                    description = item.displayDescription(state.currentLanguage),
                    color = Color(item.accentColorHex),
                    stats = stats,
                    language = state.currentLanguage,
                    isLoading = state.isLoadingTestStats && state.selectedTestStats?.testType == item.type,
                    onStats = { vm.onEvent(DirectorEvent.OpenTestStats(item.type)) }
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                ContentSectionHeader(
                    emoji = "📚",
                    title = strings.coursesTitle,
                    subtitle = state.currentLanguage.pick(
                        "${AppCourseCatalog.baseCourses.size} базовых курсов платформы",
                        "${AppCourseCatalog.baseCourses.size} base platform courses",
                        "Платформаның ${AppCourseCatalog.baseCourses.size} негізгі курсы"
                    ),
                    accentColor = CyanAccent
                )
            }
            itemsIndexed(AppCourseCatalog.baseCourses) { _, course ->
                val completionStats = state.baseCourseCompletionStats.firstOrNull { it.courseId == course.id }
                PlatformCourseCard(
                    course = course,
                    language = state.currentLanguage,
                    onClick = { vm.onEvent(DirectorEvent.OpenBaseCourse(course)) },
                    badgeText = completionStats?.completedCount?.takeIf { it > 0 }?.let {
                        state.currentLanguage.pick("$it прошли", "$it completed", "$it өтті")
                    },
                    footer = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = completionStats?.let {
                                    state.currentLanguage.pick(
                                        "Прошли: ${it.completedCount} · Не начинали: ${it.notStartedCount}",
                                        "Completed: ${it.completedCount} · Not started: ${it.notStartedCount}",
                                        "Өткендер: ${it.completedCount} · Бастамағандар: ${it.notStartedCount}"
                                    )
                                } ?: state.currentLanguage.pick("Нет данных по организации", "No organization data", "Ұйым бойынша дерек жоқ"),
                                color = TextHint,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyanAccent.copy(0.14f))
                                    .border(1.dp, CyanAccent.copy(0.35f), RoundedCornerShape(10.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { vm.onEvent(DirectorEvent.OpenBaseCourseCompletion(course.id)) }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                if (state.isLoadingCourseCompletionDetails && state.selectedBaseCourseCompletion?.courseId == course.id) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CyanAccent, strokeWidth = 1.8.dp)
                                } else {
                                        Text(state.currentLanguage.pick("Кто прошёл", "Who completed", "Кім өтті"), color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                ContentSectionHeader(
                    emoji = "➕",
                    title = state.currentLanguage.pick("Добавленные курсы", "Added courses", "Қосылған курстар"),
                    subtitle = if (state.addedCourses.isEmpty()) {
                        state.currentLanguage.pick("Психолог ещё не добавил курсы", "The psychologist has not added courses yet", "Психолог әлі курстар қоспаған")
                    } else {
                        state.currentLanguage.pick(
                            "${state.addedCourses.size} курс(ов) от психолога",
                            "${state.addedCourses.size} course(s) from the psychologist",
                            "Психологтан ${state.addedCourses.size} курс"
                        )
                    },
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
                            Text(state.currentLanguage.pick("Новых курсов добавлено не было", "No new courses have been added", "Жаңа курстар қосылған жоқ"), color = Color.White.copy(0.4f), fontSize = 14.sp)
                        }
                    }
                }
            } else {
                itemsIndexed(state.addedCourses) { _, course ->
                    OrganizationCourseCard(
                        course = course,
                        language = state.currentLanguage,
                        onClick = { vm.onEvent(DirectorEvent.OpenAddedCourse(course)) }
                    )
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }

        // ── Text Course Viewer overlay ─────────────────────────────────────────
        if (state.showTextCourseViewer && state.selectedAddedCourse != null) {
            DirectorTextCourseDialog(
                course = state.selectedAddedCourse,
                language = state.currentLanguage,
                onDismiss = { vm.onEvent(DirectorEvent.CloseTextCourseViewer) }
            )
        }

        if (state.showTestStatsDialog && state.selectedTestStats != null) {
            DirectorTestStatsDialog(
                stats = state.selectedTestStats,
                language = state.currentLanguage,
                onDismiss = { vm.onEvent(DirectorEvent.CloseTestStatsDialog) }
            )
        }

        if (state.showBaseCourseCompletionDialog && state.selectedBaseCourseCompletion != null) {
            BaseCourseCompletionDialog(
                details = state.selectedBaseCourseCompletion,
                language = state.currentLanguage,
                onDismiss = { vm.onEvent(DirectorEvent.CloseBaseCourseCompletionDialog) }
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

@Composable
private fun DirectorStatRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(0.62f), fontSize = 13.sp)
        Text(value, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Director Test Card ────────────────────────────────────────────────────────

@Composable
private fun DirectorTestCard(
    index: Int,
    emoji: String,
    name: String,
    description: String,
    color: Color,
    stats: OrganizationTestStats?,
    language: com.example.aiphysical.presentation.auth.AppLanguage,
    isLoading: Boolean,
    onStats: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(color.copy(0.12f), Color.White.copy(0.04f))))
            .border(1.dp, Brush.horizontalGradient(listOf(color.copy(0.4f), Color.White.copy(0.06f))), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(46.dp).background(color.copy(0.2f), RoundedCornerShape(14.dp)).border(1.dp, color.copy(0.5f), RoundedCornerShape(14.dp)),
            Alignment.Center
        ) { Text(emoji, fontSize = 18.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(color.copy(0.2f), RoundedCornerShape(6.dp)).border(1.dp, color.copy(0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                    Text("$index", color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stats?.let {
                    language.pick(
                        "${it.totalAttempts} прохождений · ${assessmentLabel(it.mostFrequentAssessment, language)}",
                        "${it.totalAttempts} attempts · ${assessmentLabel(it.mostFrequentAssessment, language)}",
                        "${it.totalAttempts} өту · ${assessmentLabel(it.mostFrequentAssessment, language)}"
                    )
                } ?: language.pick("Нет данных по организации", "No organization data", "Ұйым бойынша дерек жоқ"),
                color = TextHint,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(
            onClick = onStats,
            modifier = Modifier.background(color.copy(0.14f), RoundedCornerShape(12.dp)).border(1.dp, color.copy(0.35f), RoundedCornerShape(12.dp))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = color, strokeWidth = 1.8.dp)
            } else {
                Text(language.pick("📊 Статистика", "📊 Stats", "📊 Статистика"), color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DirectorTestStatsDialog(
    stats: OrganizationTestStats,
    language: com.example.aiphysical.presentation.auth.AppLanguage,
    onDismiss: () -> Unit,
) {
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
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFF0D0D22))
                    .border(1.dp, Brush.horizontalGradient(listOf(CyanAccent.copy(0.4f), NeonViolet.copy(0.3f))), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.width(48.dp).height(4.dp).background(Color.White.copy(0.12f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                Text(
                    AppStudentTestCatalog.items.firstOrNull { it.type == stats.testType || it.testId == stats.testId }
                        ?.displayTitle(language)
                        ?: stats.testName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(language.pick("Статистика по студентам текущей организации", "Statistics for students in the current organization", "Ағымдағы ұйым студенттері бойынша статистика"), color = Color.White.copy(0.55f), fontSize = 13.sp)
                HorizontalDivider(color = Color.White.copy(0.08f))
                DirectorStatRow(language.pick("Всего прохождений", "Total attempts", "Жалпы өту саны"), stats.totalAttempts.toString(), CyanAccent)
                DirectorStatRow(language.pick("Чаще всего", "Most frequent", "Ең жиі"), assessmentLabel(stats.mostFrequentAssessment, language), NeonViolet)
                if (stats.totalAttempts == 0) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.04f)).border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp)).padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(language.pick("Пока нет прохождений этого теста", "There have been no attempts for this test yet", "Бұл тесттен өту әлі болған жоқ"), color = Color.White.copy(0.45f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseCourseCompletionDialog(
    details: BaseCourseCompletionDetails,
    language: com.example.aiphysical.presentation.auth.AppLanguage,
    onDismiss: () -> Unit,
) {
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
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFF0D0D22))
                    .border(1.dp, Brush.horizontalGradient(listOf(CyanAccent.copy(0.4f), NeonViolet.copy(0.3f))), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.width(48.dp).height(4.dp).background(Color.White.copy(0.12f), RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                Text(
                    AppCourseCatalog.baseCourses.firstOrNull { it.id == details.courseId }
                        ?.displayTitle(language)
                        ?: details.courseName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(language.pick("Статусы прохождения среди студентов организации", "Completion statuses among students in the organization", "Ұйым студенттері арасындағы өту мәртебелері"), color = Color.White.copy(0.55f), fontSize = 13.sp)
                CompletionSection(language.pick("Прошли курс", "Completed the course", "Курсты аяқтағандар"), details.completedMembers, StatusNormal, language)
                CompletionSection(language.pick("Начали / в процессе", "Started / in progress", "Бастаған / өтіп жатыр"), details.inProgressMembers, AlertOrange, language)
                CompletionSection(language.pick("Не начинали", "Not started", "Бастамағандар"), details.notStartedMembers, TextHint, language)
            }
        }
    }
}

@Composable
private fun CompletionSection(
    title: String,
    members: List<CourseCompletionMember>,
    color: Color,
    language: com.example.aiphysical.presentation.auth.AppLanguage,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text("${members.size}", color = Color.White.copy(0.45f), fontSize = 11.sp)
        }

        if (members.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.04f)).border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(language.pick("Нет студентов в этой группе", "There are no students in this group", "Бұл топта студенттер жоқ"), color = Color.White.copy(0.45f), fontSize = 12.sp)
            }
        } else {
            members.forEach { member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(0.04f))
                        .border(1.dp, color.copy(0.22f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(38.dp).background(color.copy(0.16f), RoundedCornerShape(12.dp)).border(1.dp, color.copy(0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(member.fullName.take(1).uppercase(), color = color, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(member.fullName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (member.lastAccessMillis > 0L) {
                                language.pick(
                                    "Последний доступ: ${formatCourseDate(member.lastAccessMillis)}",
                                    "Last access: ${formatCourseDate(member.lastAccessMillis)}",
                                    "Соңғы кіру: ${formatCourseDate(member.lastAccessMillis)}"
                                )
                            } else {
                                language.pick("Последний доступ: —", "Last access: —", "Соңғы кіру: —")
                            },
                            color = Color.White.copy(0.45f),
                            fontSize = 10.sp
                        )
                    }
                    Text("${(member.progress.coerceIn(0f, 1f) * 100).toInt()}%", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DirectorTextCourseDialog(
    course: OrganizationCourse,
    language: com.example.aiphysical.presentation.auth.AppLanguage,
    onDismiss: () -> Unit,
) {
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
                    Text(language.pick("📝 Текстовый курс", "📝 Text course", "📝 Мәтіндік курс"), color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(course.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                if (course.description.isNotBlank()) Text(course.description, color = Color.White.copy(0.55f), fontSize = 14.sp, lineHeight = 20.sp)
                HorizontalDivider(color = Color.White.copy(0.08f))
                if (course.contentText.isNotBlank()) {
                    Text(course.contentText, color = Color.White.copy(0.85f), fontSize = 15.sp, lineHeight = 24.sp)
                } else {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.05f)).padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(language.pick("Текст курса отсутствует", "Course text is missing", "Курс мәтіні жоқ"), color = Color.White.copy(0.35f), fontSize = 14.sp)
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
                ) { Text(language.pick("Закрыть", "Close", "Жабу"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun formatCourseDate(millis: Long): String {
    if (millis == 0L) return "—"
    val totalDays = millis / 86_400_000L
    val year = 1970 + (totalDays / 365.2425).toInt()
    val dayOfYear = (totalDays % 365).toInt().coerceIn(1, 365)
    val month = (dayOfYear / 30.44).toInt().coerceIn(0, 11) + 1
    val day = (dayOfYear % 31).coerceIn(1, 31)
    return "${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.$year"
}
