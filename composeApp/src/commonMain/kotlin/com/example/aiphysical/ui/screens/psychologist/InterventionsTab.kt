package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aiphysical.data.model.AppCourseCatalog
import com.example.aiphysical.data.model.UserProfile
import com.example.aiphysical.data.model.displayTitle
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.presentation.psychologist.PsychologistEvent
import com.example.aiphysical.presentation.psychologist.PsychologistHomeState
import com.example.aiphysical.presentation.psychologist.PsychologistViewModel
import com.example.aiphysical.ui.theme.*

// Pre-loaded courses available for assignment — using shared catalog
val PREDEFINED_COURSES: List<Pair<String, String>> = AppCourseCatalog.baseCourses.map { it.id to it.title }

@Composable
fun InterventionsTab(
    state: PsychologistHomeState,
    vm: PsychologistViewModel,
    modifier: Modifier = Modifier,
) {
    val language = state.currentLanguage
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                language.pick("Рекомендации", "Recommendations", "Ұсынымдар"),
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                language.pick("Студенты, ожидающие обратной связи", "Students waiting for feedback", "Кері байланыс күтіп тұрған студенттер"),
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PsychTeal, strokeWidth = 2.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section: Pending (no recommendation yet)
                if (state.pendingRecommendations.isNotEmpty()) {
                    item {
                        InterventionSectionHeader(
                            title = language.pick("ТРЕБУЮТ ВНИМАНИЯ", "NEED ATTENTION", "НАЗАРДЫ ҚАЖЕТ ЕТЕДІ"),
                            count = state.pendingRecommendations.size,
                            color = PsychWarning
                        )
                    }
                    items(state.pendingRecommendations) { student ->
                        ActionItemCard(
                            student = student,
                            language = language,
                            hasRecommendation = false,
                            onRecommend = { vm.onEvent(PsychologistEvent.OpenRecommendationSheet(student)) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // Section: Already recommended (update available)
                val withRecommendation = state.students.filter { it.psychComment.isNotBlank() }
                if (withRecommendation.isNotEmpty()) {
                    item {
                        InterventionSectionHeader(
                            title = language.pick("С РЕКОМЕНДАЦИЕЙ", "WITH RECOMMENDATION", "ҰСЫНЫММЕН"),
                            count = withRecommendation.size,
                            color = PsychTeal
                        )
                    }
                    items(withRecommendation) { student ->
                        ActionItemCard(
                            student = student,
                            language = language,
                            hasRecommendation = true,
                            onRecommend = { vm.onEvent(PsychologistEvent.OpenRecommendationSheet(student)) }
                        )
                    }
                }

                // Empty state
                if (state.pendingRecommendations.isEmpty() && state.students.none { it.psychComment.isNotBlank() }) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("✅", fontSize = 48.sp)
                                Text(
                                    language.pick("Все студенты получили рекомендации", "All students have received recommendations", "Барлық студенттер ұсыныстар алды"),
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
    // NOTE: RecommendationSheet is rendered globally in PsychologistDashboardScreen
}

@Composable
private fun InterventionSectionHeader(title: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(title, color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Spacer(Modifier.weight(1f))
        Text("$count", color = color.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Action Item Card ──────────────────────────────────────────────────────────

@Composable
private fun ActionItemCard(
    student: UserProfile,
    language: com.example.aiphysical.presentation.auth.AppLanguage,
    hasRecommendation: Boolean,
    onRecommend: () -> Unit,
) {
    val statusColor = when (student.latestAiStatus) {
        "critical" -> PsychCritical
        "stress"   -> PsychWarning
        "normal"   -> PsychTeal
        else       -> TextHint
    }
    val statusLabel = when (student.latestAiStatus) {
        "critical" -> language.pick("Критично", "Critical", "Критикалық")
        "stress"   -> language.pick("Стресс", "Stress", "Стресс")
        "normal"   -> language.pick("Норма", "Normal", "Қалыпты")
        else       -> language.pick("Неизвестно", "Unknown", "Белгісіз")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MatteSurface)
            .border(
                1.dp,
                if (!hasRecommendation) PsychWarning.copy(0.3f) else MatteCardBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(statusColor.copy(0.15f), CircleShape)
                .border(1.5.dp, statusColor.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                student.fullName.take(1).uppercase(),
                color = statusColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Info
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                student.fullName,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(statusColor.copy(0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(statusLabel, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                if (hasRecommendation) {
                    Text(language.pick("✓ Есть рекомендация", "✓ Recommendation added", "✓ Ұсыным бар"), color = PsychTeal, fontSize = 10.sp)
                }
            }
            if (student.psychComment.isNotBlank()) {
                Text(
                    student.psychComment,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (!hasRecommendation) PsychWarning.copy(0.18f) else MatteSurface
                )
                .border(
                    1.dp,
                    if (!hasRecommendation) PsychWarning.copy(0.5f) else MatteCardBorder,
                    RoundedCornerShape(10.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onRecommend
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                if (!hasRecommendation) language.pick("Написать", "Write", "Жазу") else language.pick("Изменить", "Edit", "Өзгерту"),
                color = if (!hasRecommendation) PsychWarning else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Recommendation Sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecommendationSheet(
    state: PsychologistHomeState,
    vm: PsychologistViewModel,
) {
    val student = state.recommendationTarget ?: return
    val language = state.currentLanguage

    Dialog(
        onDismissRequest = { vm.onEvent(PsychologistEvent.DismissRecommendationSheet) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PsychBackground.copy(0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { vm.onEvent(PsychologistEvent.DismissRecommendationSheet) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MatteSurface)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PsychTeal.copy(0.4f), MatteCardBorder)),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* absorb clicks */ }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Handle indicator
                Box(
                    Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(MatteCardBorder, RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                // Title
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        language.pick("Рекомендация для", "Recommendation for", "Кімге арналған ұсыным"),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        student.fullName,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                HorizontalDivider(color = MatteCardBorder)

                // Comment field
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        language.pick("ПРОФЕССИОНАЛЬНЫЙ КОММЕНТАРИЙ", "PROFESSIONAL COMMENT", "КӘСІБИ ПІКІР"),
                        color = TextHint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PsychBackground)
                            .border(
                                1.dp,
                                if (state.recommendationComment.isNotBlank()) PsychTeal.copy(0.5f) else MatteCardBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        BasicTextField(
                            value = state.recommendationComment,
                            onValueChange = { vm.onEvent(PsychologistEvent.UpdateRecommendationComment(it)) },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp),
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            ),
                            decorationBox = { inner ->
                                if (state.recommendationComment.isEmpty()) {
                                    Text(
                                        language.pick("Введите профессиональный комментарий и рекомендации для студента...", "Enter a professional comment and recommendations for the student...", "Студентке арналған кәсіби пікір мен ұсыныстарды енгізіңіз..."),
                                        color = TextHint,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }

                // Course selection
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        language.pick("НАЗНАЧИТЬ КУРС", "ASSIGN COURSE", "КУРС ТАҒАЙЫНДАУ"),
                        color = TextHint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    PREDEFINED_COURSES.forEach { (courseId, courseName) ->
                        val isSelected = state.recommendationCourseId == courseId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) PsychTeal.copy(0.15f) else PsychBackground
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) PsychTeal.copy(0.6f) else MatteCardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (isSelected) {
                                        vm.onEvent(PsychologistEvent.SelectRecommendationCourse("", ""))
                                    } else {
                                        vm.onEvent(PsychologistEvent.SelectRecommendationCourse(courseId, courseName))
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(if (isSelected) "✓" else "○", color = if (isSelected) PsychTeal else TextHint, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                AppCourseCatalog.baseCourses.firstOrNull { it.id == courseId }?.displayTitle(language) ?: courseName,
                                color = if (isSelected) PsychTeal else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Priority selection
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        language.pick("ПРИОРИТЕТ", "PRIORITY", "БАСЫМДЫҚ"),
                        color = TextHint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            "LOW" to language.pick("Низкий", "Low", "Төмен"),
                            "MEDIUM" to language.pick("Средний", "Medium", "Орташа"),
                            "HIGH" to language.pick("Высокий", "High", "Жоғары")
                        ).forEach { (key, label) ->
                            val color = when (key) {
                                "LOW"    -> PsychPriorityLow
                                "MEDIUM" -> PsychPriorityMedium
                                "HIGH"   -> PsychPriorityHigh
                                else     -> TextHint
                            }
                            val isSelected = state.recommendationPriority == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) color.copy(0.22f) else PsychBackground)
                                    .border(1.dp, if (isSelected) color.copy(0.7f) else MatteCardBorder, RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { vm.onEvent(PsychologistEvent.SetRecommendationPriority(key)) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) color else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MatteCardBorder)

                // Send button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (state.recommendationComment.isNotBlank())
                                Brush.horizontalGradient(listOf(PsychTeal.copy(0.9f), PsychTeal.copy(0.6f)))
                            else
                                Brush.horizontalGradient(listOf(MatteCardBorder, MatteCardBorder))
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !state.isSendingRecommendation
                        ) {
                            if (state.recommendationComment.isNotBlank()) {
                                vm.onEvent(PsychologistEvent.SendRecommendation)
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isSendingRecommendation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            language.pick("Отправить рекомендацию", "Send recommendation", "Ұсынымды жіберу"),
                            color = if (state.recommendationComment.isNotBlank()) Color.White else TextHint,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

