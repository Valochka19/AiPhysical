package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.AppCourseCatalog
import com.example.aiphysical.data.model.AppStudentTestCatalog
import com.example.aiphysical.data.model.OrganizationTestStats
import com.example.aiphysical.data.model.assessmentLabel
import com.example.aiphysical.data.model.CourseContentType
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.data.model.displayDescription
import com.example.aiphysical.data.model.displayTitle
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.presentation.psychologist.PsychologistEvent
import com.example.aiphysical.presentation.psychologist.PsychologistHomeState
import com.example.aiphysical.presentation.psychologist.PsychologistViewModel
import com.example.aiphysical.ui.components.OrganizationCustomTestCard
import com.example.aiphysical.ui.components.OrganizationCourseCard
import com.example.aiphysical.ui.components.PlatformCourseCard
import com.example.aiphysical.ui.theme.*


@Composable
fun LibraryTab(
    state: PsychologistHomeState,
    vm: PsychologistViewModel,
    modifier: Modifier = Modifier,
) {
    val language = state.currentLanguage
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(language.pick("Библиотека", "Library", "Кітапхана"), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(language.pick("Тесты и курсы платформы KASU", "KASU platform tests and courses", "KASU платформасының тесттері мен курстары"), color = TextSecondary, fontSize = 13.sp)
            }
        }

        item(key = "profile") {
            PsychProfileCard(state = state, language = language)
        }

        item(key = "tests") {
            LibrarySection(title = language.pick("5 ОБЯЗАТЕЛЬНЫХ ТЕСТОВ", "5 REQUIRED TESTS", "5 МІНДЕТТІ ТЕСТ"), emoji = "📋") {
                AppStudentTestCatalog.items.forEachIndexed { index, item ->
                    val stats = state.testStats.firstOrNull { it.testType == item.type }
                    PsychologistTestLibraryCard(
                        index = index + 1,
                        emoji = item.emoji,
                        name = item.displayTitle(language),
                        description = item.displayDescription(language),
                        stats = stats,
                        language = language,
                        isLoadingStats = state.isLoadingTestStats && state.selectedTestStats?.testType == item.type,
                        onStatsClick = { vm.onEvent(PsychologistEvent.OpenTestStats(item.type)) },
                        isLast = index == AppStudentTestCatalog.items.lastIndex
                    )
                }
            }
        }

        item(key = "base_courses") {
            LibrarySection(title = language.pick("5 БАЗОВЫХ КУРСОВ", "5 BASE COURSES", "5 НЕГІЗГІ КУРС"), emoji = "📚") {
                AppCourseCatalog.baseCourses.forEach { item ->
                    val assignedCount = state.students.count { it.assignedCourseId == item.id }
                    PlatformCourseCard(
                        course = item,
                        language = language,
                        badgeText = if (assignedCount > 0) {
                            language.pick("$assignedCount назн.", "$assignedCount assigned", "$assignedCount тағайындалды")
                        } else null,
                        onClick = { vm.onEvent(PsychologistEvent.OpenBaseCourse(item)) }
                    )
                }
            }
        }

        item(key = "action_add_course") {
            PsychActionButton(
                emoji = "➕",
                title = language.pick("Добавление курса", "Add course", "Курс қосу"),
                subtitle = language.pick("Создать и опубликовать новый курс", "Create and publish a new course", "Жаңа курсты жасап, жариялау"),
                accentColor = PsychTeal,
                onClick = { vm.onEvent(PsychologistEvent.OpenAddCourseSheet) }
            )
        }

        item(key = "action_added_courses") {
            PsychActionButton(
                emoji = "📂",
                title = language.pick("Добавленные курсы", "Added courses", "Қосылған курстар"),
                subtitle = if (state.addedCourses.isEmpty()) {
                    language.pick("Курсов пока нет", "No courses yet", "Әзірге курстар жоқ")
                } else {
                    language.pick(
                        "${state.addedCourses.size} курс(ов) опубликовано",
                        "${state.addedCourses.size} course(s) published",
                        "${state.addedCourses.size} курс жарияланды"
                    )
                },
                accentColor = NeonViolet,
                badge = if (state.addedCourses.isNotEmpty()) state.addedCourses.size.toString() else null,
                onClick = { vm.onEvent(PsychologistEvent.OpenAddedCourses) }
            )
        }

        item(key = "action_publish_test") {
            PsychActionButton(
                emoji = "🧪",
                title = language.pick("Загрузить тест", "Publish test", "Тест жүктеу"),
                subtitle = language.pick("Создать и опубликовать тест для студентов", "Create and publish a test for students", "Студенттерге арналған тест жасап, жариялау"),
                accentColor = AlertOrange,
                onClick = { vm.onEvent(PsychologistEvent.OpenAddTestScreen) }
            )
        }

        if (state.showAddedCoursesViewer) {
            item(key = "added_courses_viewer") {
                PsychAddedCoursesSection(
                    language = language,
                    courses = state.addedCourses,
                    onCourse = { vm.onEvent(PsychologistEvent.OpenAddedCourse(it)) },
                    onDelete = { vm.onEvent(PsychologistEvent.DeleteAddedCourse(it)) },
                    onClose = { vm.onEvent(PsychologistEvent.CloseAddedCourses) }
                )
            }
        }

        item(key = "custom_tests") {
            LibrarySection(title = language.pick("ДОБАВЛЕННЫЕ ТЕСТЫ", "ADDED TESTS", "ҚОСЫЛҒАН ТЕСТТЕР"), emoji = "🧪") {
                if (state.isLoadingCustomTests) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AlertOrange, strokeWidth = 2.dp)
                    }
                } else if (state.customTests.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📝", fontSize = 36.sp)
                            Text(language.pick("Пока нет опубликованных тестов", "No published tests yet", "Әзірге жарияланған тесттер жоқ"), color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    state.customTests.forEachIndexed { index, test ->
                        OrganizationCustomTestCard(
                            test = test,
                            language = language,
                            onClick = {},
                            accentColor = AlertOrange,
                            badgeText = language.pick("${test.questions.size} вопрос(ов)", "${test.questions.size} question(s)", "${test.questions.size} сұрақ"),
                            metaText = test.createdByName.ifBlank { language.pick("Опубликовано", "Published", "Жарияланған") },
                            ctaText = language.pick("Опубликован", "Published", "Жарияланды")
                        )
                        if (index != state.customTests.lastIndex) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        item(key = "stats") {
            LibraryStatsCard(state = state, language = language)
        }
    }

    // ── Add Course Sheet (dialog) ──────────────────────────────────────────────
    if (state.showAddCourseSheet) {
        AddCourseSheet(state = state, vm = vm)
    }

    // ── Text course viewer ─────────────────────────────────────────────────────
    if (state.showTextCourseViewer && state.selectedAddedCourse != null) {
        PsychTextCourseDialog(
            course = state.selectedAddedCourse,
            language = language,
            onDismiss = { vm.onEvent(PsychologistEvent.CloseTextCourseViewer) }
        )
    }

    if (state.showTestStatsDialog && state.selectedTestStats != null) {
        TestStatsDialog(
            stats = state.selectedTestStats,
            language = language,
            onDismiss = { vm.onEvent(PsychologistEvent.CloseTestStatsDialog) }
        )
    }
}

// ── Psychologist Profile Card ─────────────────────────────────────────────────

@Composable
private fun PsychProfileCard(state: PsychologistHomeState, language: com.example.aiphysical.presentation.auth.AppLanguage) {
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
                    language.pick("Психолог", "Psychologist", "Психолог"),
                    color = PsychTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                language.pick(
                    "${state.students.size} студентов в организации",
                    "${state.students.size} students in the organization",
                    "Ұйымда ${state.students.size} студент бар"
                ),
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// ── Library Section wrapper ───────────────────────────────────────────────────

@Composable
private fun LibrarySection(title: String, emoji: String, content: @Composable ColumnScope.() -> Unit) {
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
            Text(emoji, fontSize = 18.sp)
            Text(title, color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        }
        content()
    }
}

// ── Premium Action Button ─────────────────────────────────────────────────────

@Composable
private fun PsychActionButton(
    emoji: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(accentColor.copy(0.12f), accentColor.copy(0.06f))))
            .border(1.5.dp, Brush.horizontalGradient(listOf(accentColor.copy(0.55f), accentColor.copy(0.2f))), RoundedCornerShape(20.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .background(accentColor.copy(0.2f), RoundedCornerShape(16.dp))
                .border(1.dp, accentColor.copy(0.45f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 22.sp) }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        if (badge != null) {
            Box(
                Modifier
                    .background(accentColor.copy(0.2f), RoundedCornerShape(10.dp))
                    .border(1.dp, accentColor.copy(0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(badge, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
        }
        Text("›", color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Added Courses Section (inline) ────────────────────────────────────────────

@Composable
private fun PsychAddedCoursesSection(
    language: com.example.aiphysical.presentation.auth.AppLanguage,
    courses: List<OrganizationCourse>,
    onCourse: (OrganizationCourse) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, Brush.verticalGradient(listOf(NeonViolet.copy(0.3f), MatteCardBorder)), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(language.pick("ДОБАВЛЕННЫЕ КУРСЫ", "ADDED COURSES", "ҚОСЫЛҒАН КУРСТАР"), color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MatteCardBorder.copy(0.4f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(language.pick("✕ Свернуть", "✕ Collapse", "✕ Жинау"), color = TextSecondary, fontSize = 11.sp) }
        }

        if (courses.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📭", fontSize = 36.sp)
                    Text(language.pick("Новых курсов добавлено не было", "No new courses have been added", "Жаңа курстар қосылған жоқ"), color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            courses.forEach { course ->
                OrganizationCourseCard(
                    course = course,
                    language = language,
                    onClick = { onCourse(course) },
                    showDeleteButton = true,
                    onDelete = { onDelete(course.id) }
                )
            }
        }
    }
}

// ── Add Course Sheet ──────────────────────────────────────────────────────────

@Composable
private fun AddCourseSheet(state: PsychologistHomeState, vm: PsychologistViewModel) {
    val language = state.currentLanguage
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { vm.onEvent(PsychologistEvent.CloseAddCourseSheet) },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PsychBackground.copy(0.88f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    vm.onEvent(PsychologistEvent.CloseAddCourseSheet)
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MatteSurface)
                    .border(1.dp, Brush.horizontalGradient(listOf(PsychTeal.copy(0.4f), MatteCardBorder)), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(Modifier.width(48.dp).height(4.dp).background(MatteCardBorder, RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(language.pick("Добавление курса", "Add course", "Курс қосу"), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(language.pick("Создайте курс для вашей организации", "Create a course for your organization", "Ұйымыңыз үшін курс жасаңыз"), color = TextSecondary, fontSize = 13.sp)
                }

                HorizontalDivider(color = MatteCardBorder)

                // Title field
                FormField(
                    label = language.pick("НАЗВАНИЕ КУРСА", "COURSE TITLE", "КУРС АТАУЫ"),
                    value = state.newCourseTitle,
                    placeholder = language.pick("Введите название...", "Enter a title...", "Атауын енгізіңіз..."),
                    onValueChange = { vm.onEvent(PsychologistEvent.UpdateNewCourseTitle(it)) }
                )

                // Description field
                FormField(
                    label = language.pick("ОПИСАНИЕ", "DESCRIPTION", "СИПАТТАМА"),
                    value = state.newCourseDescription,
                    placeholder = language.pick("Краткое описание курса...", "Short course description...", "Курстың қысқаша сипаттамасы..."),
                    onValueChange = { vm.onEvent(PsychologistEvent.UpdateNewCourseDescription(it)) }
                )

                // Type selector
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(language.pick("ТИП КУРСА", "COURSE TYPE", "КУРС ТҮРІ"), color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            CourseContentType.TEXT to ("📝" to language.pick("Текстовый", "Text", "Мәтіндік")),
                            CourseContentType.VIDEO to ("🎬" to language.pick("Видео", "Video", "Видео"))
                        ).forEach { (type, info) ->
                            val (emoji, label) = info
                            val isSelected = state.newCourseType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) PsychTeal.copy(0.18f) else PsychBackground)
                                    .border(1.dp, if (isSelected) PsychTeal.copy(0.6f) else MatteCardBorder, RoundedCornerShape(14.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        vm.onEvent(PsychologistEvent.UpdateNewCourseType(type))
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(emoji, fontSize = 22.sp)
                                    Text(label, color = if (isSelected) PsychTeal else TextSecondary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                // Conditional fields
                if (state.newCourseType == CourseContentType.VIDEO) {
                    FormField(
                        label = language.pick("ССЫЛКА НА ВИДЕО", "VIDEO LINK", "ВИДЕО СІЛТЕМЕСІ"),
                        value = state.newCourseVideoUrl,
                        placeholder = "https://youtube.com/...",
                        onValueChange = { vm.onEvent(PsychologistEvent.UpdateNewCourseVideoUrl(it)) }
                    )
                } else {
                    FormField(
                        label = language.pick("ТЕКСТ КУРСА", "COURSE TEXT", "КУРС МӘТІНІ"),
                        value = state.newCourseTextContent,
                        placeholder = language.pick("Введите текст курса...", "Enter the course text...", "Курс мәтінін енгізіңіз..."),
                        minHeight = 180.dp,
                        onValueChange = { vm.onEvent(PsychologistEvent.UpdateNewCourseTextContent(it)) }
                    )
                }

                HorizontalDivider(color = MatteCardBorder)

                val isFormValid = state.newCourseTitle.isNotBlank() && state.newCourseDescription.isNotBlank() &&
                    when (state.newCourseType) {
                        CourseContentType.VIDEO -> state.newCourseVideoUrl.isNotBlank()
                        CourseContentType.TEXT  -> state.newCourseTextContent.isNotBlank()
                    }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isFormValid) Brush.horizontalGradient(listOf(PsychTeal.copy(0.9f), PsychTeal.copy(0.6f)))
                            else Brush.horizontalGradient(listOf(MatteCardBorder, MatteCardBorder))
                        )
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !state.isPublishingCourse) {
                            if (isFormValid) vm.onEvent(PsychologistEvent.PublishCourse)
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isPublishingCourse) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            language.pick("Опубликовать курс", "Publish course", "Курсты жариялау"),
                            color = if (isFormValid) Color.White else TextHint,
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    minHeight: androidx.compose.ui.unit.Dp = 56.dp,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PsychBackground)
                .border(1.dp, if (value.isNotBlank()) PsychTeal.copy(0.5f) else MatteCardBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = minHeight),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, color = TextHint, fontSize = 14.sp)
                    inner()
                }
            )
        }
    }
}

// ── Text Course Viewer Dialog ─────────────────────────────────────────────────

@Composable
private fun PsychTextCourseDialog(
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
                .background(PsychBackground.copy(0.88f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MatteSurface)
                    .border(1.dp, Brush.horizontalGradient(listOf(PsychTeal.copy(0.4f), MatteCardBorder)), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.width(48.dp).height(4.dp).background(MatteCardBorder, RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                Box(Modifier.background(PsychTeal.copy(0.15f), RoundedCornerShape(8.dp)).border(1.dp, PsychTeal.copy(0.35f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(language.pick("📝 Текстовый курс", "📝 Text course", "📝 Мәтіндік курс"), color = PsychTeal, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(course.title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                if (course.description.isNotBlank()) Text(course.description, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                HorizontalDivider(color = MatteCardBorder)
                if (course.contentText.isNotBlank()) {
                    Text(course.contentText, color = TextPrimary, fontSize = 15.sp, lineHeight = 24.sp)
                } else {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MatteCardBorder.copy(0.3f)).padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(language.pick("Текст курса отсутствует", "Course text is missing", "Курс мәтіні жоқ"), color = TextHint, fontSize = 14.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(PsychTeal.copy(0.9f), PsychTeal.copy(0.6f))))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text(language.pick("Закрыть", "Close", "Жабу"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ── Test Item ─────────────────────────────────────────────────────────────────

@Composable
private fun PsychologistTestLibraryCard(
    index: Int,
    emoji: String,
    name: String,
    description: String,
    stats: OrganizationTestStats?,
    language: com.example.aiphysical.presentation.auth.AppLanguage,
    isLoadingStats: Boolean,
    onStatsClick: () -> Unit,
    isLast: Boolean,
) {
    val accentColors = listOf(MetricBurnout, MetricStress, MetricEmotion, MetricMotivation, MetricAnxiety)
    val color = accentColors.getOrElse(index - 1) { PsychTeal }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(34.dp).background(color.copy(0.15f), CircleShape).border(1.dp, color.copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Text("$index", color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(emoji, fontSize = 16.sp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val statsText = stats?.let {
                    language.pick(
                        "${it.totalAttempts} прохожд. · ${assessmentLabel(it.mostFrequentAssessment, language)}",
                        "${it.totalAttempts} attempts · ${assessmentLabel(it.mostFrequentAssessment, language)}",
                        "${it.totalAttempts} өту · ${assessmentLabel(it.mostFrequentAssessment, language)}"
                    )
                } ?: language.pick("Нет данных по организации", "No organization data", "Ұйым бойынша дерек жоқ")
                Text(statsText, color = TextHint, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(
                onClick = onStatsClick,
                modifier = Modifier
                    .background(color.copy(0.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, color.copy(0.35f), RoundedCornerShape(10.dp))
            ) {
                if (isLoadingStats) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = color, strokeWidth = 1.8.dp)
                } else {
                    Text(language.pick("📊 Статистика", "📊 Stats", "📊 Статистика"), color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (!isLast) HorizontalDivider(color = MatteCardBorder, modifier = Modifier.padding(start = 50.dp))
    }
}

@Composable
private fun TestStatsDialog(
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
                .background(PsychBackground.copy(0.88f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MatteSurface)
                    .border(1.dp, Brush.horizontalGradient(listOf(PsychTeal.copy(0.4f), MatteCardBorder)), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.width(48.dp).height(4.dp).background(MatteCardBorder, RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                Text(
                    AppStudentTestCatalog.items.firstOrNull { it.type == stats.testType || it.testId == stats.testId }
                        ?.displayTitle(language)
                        ?: stats.testName,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(language.pick("Статистика по студентам вашей организации", "Statistics for students in your organization", "Ұйымыңыздағы студенттер бойынша статистика"), color = TextSecondary, fontSize = 13.sp)
                HorizontalDivider(color = MatteCardBorder)
                LibraryStatsRow(label = language.pick("Всего прохождений", "Total attempts", "Жалпы өту саны"), value = stats.totalAttempts.toString(), color = PsychTeal)
                LibraryStatsRow(label = language.pick("Чаще всего", "Most frequent", "Ең жиі"), value = assessmentLabel(stats.mostFrequentAssessment, language), color = NeonViolet)
                if (stats.totalAttempts == 0) {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PsychBackground).border(1.dp, MatteCardBorder, RoundedCornerShape(14.dp)).padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(language.pick("Пока нет прохождений этого теста в текущей организации", "There have been no attempts for this test in the current organization yet", "Ағымдағы ұйымда бұл тесттен өту әлі болған жоқ"), color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryStatsRow(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(0.12f)).border(1.dp, color.copy(0.35f), RoundedCornerShape(14.dp)).padding(14.dp)
        ) {
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Library Stats Card ────────────────────────────────────────────────────────

@Composable
private fun LibraryStatsCard(state: PsychologistHomeState, language: com.example.aiphysical.presentation.auth.AppLanguage) {
    val totalAssigned = state.students.count { it.assignedCourseId.isNotBlank() }
    val totalCommented = state.students.count { it.psychComment.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(language.pick("МОЯ РАБОТА", "MY WORK", "МЕНІҢ ЖҰМЫСЫМ"), color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            WorkStatItem(totalCommented.toString(), language.pick("Рекоменд.", "Recs", "Ұсыным"), PsychTeal)
            WorkStatItem(totalAssigned.toString(), language.pick("Курсов назн.", "Assigned", "Тағайын."), NeonViolet)
            WorkStatItem(state.criticalStudents.size.toString(), language.pick("Критичных", "Critical", "Критик."), PsychCritical)
            WorkStatItem(state.students.size.toString(), language.pick("Всего студ.", "Students", "Студ."), TextSecondary)
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
