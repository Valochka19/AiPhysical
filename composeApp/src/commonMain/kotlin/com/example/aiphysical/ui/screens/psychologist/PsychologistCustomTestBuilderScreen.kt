package com.example.aiphysical.ui.screens.psychologist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.presentation.psychologist.PsychologistEvent
import com.example.aiphysical.presentation.psychologist.PsychologistHomeState
import com.example.aiphysical.presentation.psychologist.PsychologistViewModel
import com.example.aiphysical.ui.theme.AlertOrange
import com.example.aiphysical.ui.theme.MatteCardBorder
import com.example.aiphysical.ui.theme.MatteSurface
import com.example.aiphysical.ui.theme.PsychBackground
import com.example.aiphysical.ui.theme.PsychCritical
import com.example.aiphysical.ui.theme.TextHint
import com.example.aiphysical.ui.theme.TextPrimary
import com.example.aiphysical.ui.theme.TextSecondary

@Composable
fun PsychologistCustomTestBuilderScreen(
    state: PsychologistHomeState,
    vm: PsychologistViewModel,
    modifier: Modifier = Modifier,
) {
    val language = state.currentLanguage
    val isCurrentQuestionValid = state.currentTestDraftTitle.trim().isNotBlank() &&
        state.currentDraftQuestionText.trim().isNotBlank() &&
        state.currentDraftOption1.trim().isNotBlank() &&
        state.currentDraftOption2.trim().isNotBlank() &&
        state.currentDraftOption3.trim().isNotBlank()
    val canPublish = state.draftQuestions.isNotEmpty() || isCurrentQuestionValid

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PsychBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { vm.onEvent(PsychologistEvent.CloseAddTestScreen) }) {
                Text(language.pick("✕ Закрыть", "✕ Close", "✕ Жабу"), color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(language.pick("Создание теста", "Create test", "Тест құру"), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(language.pick("Вопрос ${state.currentDraftQuestionIndex}", "Question ${state.currentDraftQuestionIndex}", "${state.currentDraftQuestionIndex}-сұрақ"), color = AlertOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        BuilderInfoCard(state = state, language = language)

        BuilderField(
            label = language.pick("НАЗВАНИЕ ТЕСТА", "TEST TITLE", "ТЕСТ АТАУЫ"),
            value = state.currentTestDraftTitle,
            placeholder = language.pick("Например: Самочувствие после нагрузки", "Example: Well-being after workload", "Мысалы: Жүктемеден кейінгі көңіл күй"),
            onValueChange = { vm.onEvent(PsychologistEvent.UpdateDraftTestTitle(it)) }
        )

        BuilderField(
            label = language.pick("ВОПРОС", "QUESTION", "СҰРАҚ"),
            value = state.currentDraftQuestionText,
            placeholder = language.pick("Введите формулировку вопроса...", "Enter the question text...", "Сұрақ мәтінін енгізіңіз..."),
            minHeight = 120.dp,
            onValueChange = { vm.onEvent(PsychologistEvent.UpdateDraftQuestionText(it)) }
        )

        BuilderField(
            label = language.pick("ВАРИАНТ 1", "OPTION 1", "1-НҰСҚА"),
            value = state.currentDraftOption1,
            placeholder = language.pick("Первый вариант ответа", "First answer option", "Бірінші жауап нұсқасы"),
            onValueChange = { vm.onEvent(PsychologistEvent.UpdateDraftOption1(it)) }
        )
        BuilderField(
            label = language.pick("ВАРИАНТ 2", "OPTION 2", "2-НҰСҚА"),
            value = state.currentDraftOption2,
            placeholder = language.pick("Второй вариант ответа", "Second answer option", "Екінші жауап нұсқасы"),
            onValueChange = { vm.onEvent(PsychologistEvent.UpdateDraftOption2(it)) }
        )
        BuilderField(
            label = language.pick("ВАРИАНТ 3", "OPTION 3", "3-НҰСҚА"),
            value = state.currentDraftOption3,
            placeholder = language.pick("Третий вариант ответа", "Third answer option", "Үшінші жауап нұсқасы"),
            onValueChange = { vm.onEvent(PsychologistEvent.UpdateDraftOption3(it)) }
        )

        if (state.draftQuestions.isNotEmpty()) {
            DraftQuestionsPreview(state = state, language = language)
        }

        HorizontalDivider(color = MatteCardBorder)

        BuilderActionButton(
            title = language.pick("Добавить следующий вопрос", "Add next question", "Келесі сұрақты қосу"),
            subtitle = language.pick("Текущий вопрос сохранится и откроется следующий шаг", "The current question will be saved and the next step will open", "Ағымдағы сұрақ сақталып, келесі қадам ашылады"),
            accent = AlertOrange,
            enabled = isCurrentQuestionValid && !state.isPublishingCustomTest,
            loading = false,
            onClick = { vm.onEvent(PsychologistEvent.AddNextDraftQuestion) }
        )

        BuilderActionButton(
            title = language.pick("Опубликовать тест", "Publish test", "Тестті жариялау"),
            subtitle = if (canPublish) {
                language.pick("После публикации тест увидят студенты", "Students will see the test after publishing", "Жарияланғаннан кейін тестті студенттер көреді")
            } else {
                language.pick("Добавьте минимум один полностью заполненный вопрос", "Add at least one fully completed question", "Кемінде толық толтырылған бір сұрақ қосыңыз")
            },
            accent = Color(0xFF00C896),
            enabled = canPublish && !state.isPublishingCustomTest,
            loading = state.isPublishingCustomTest,
            onClick = { vm.onEvent(PsychologistEvent.PublishDraftTest) }
        )
    }

    if (state.showDiscardCustomTestDialog) {
        AlertDialog(
            onDismissRequest = { vm.onEvent(PsychologistEvent.DismissCloseAddTestScreen) },
            containerColor = MatteSurface,
            title = { Text(language.pick("Отменить создание теста?", "Cancel test creation?", "Тест құрудан бас тарту керек пе?"), color = TextPrimary, fontWeight = FontWeight.ExtraBold) },
            text = { Text(language.pick("Черновик теста будет удалён без сохранения.", "The test draft will be deleted without saving.", "Тест нобайы сақталмай өшіріледі."), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { vm.onEvent(PsychologistEvent.ConfirmCloseAddTestScreen) }) {
                    Text(language.pick("Удалить", "Delete", "Өшіру"), color = PsychCritical, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onEvent(PsychologistEvent.DismissCloseAddTestScreen) }) {
                    Text(language.pick("Вернуться", "Go back", "Артқа оралу"), color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun BuilderInfoCard(state: PsychologistHomeState, language: com.example.aiphysical.presentation.auth.AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(AlertOrange.copy(0.14f), Color.White.copy(0.03f))))
            .border(1.dp, Brush.horizontalGradient(listOf(AlertOrange.copy(0.35f), MatteCardBorder)), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(language.pick("Соберите тест по одному вопросу за шаг", "Build the test one question at a time", "Тестті әр қадамда бір сұрақтан құрастырыңыз"), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(language.pick("Чтобы перейти дальше, заполните название, текст вопроса и все 3 варианта ответа.", "To continue, fill in the title, question text, and all 3 answer options.", "Жалғастыру үшін атауын, сұрақ мәтінін және барлық 3 жауап нұсқасын толтырыңыз."), color = TextSecondary, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BuilderMiniChip(language.pick("Сохранено", "Saved", "Сақталған"), state.draftQuestions.size.toString(), AlertOrange)
            BuilderMiniChip(language.pick("Текущий шаг", "Current step", "Ағымдағы қадам"), state.currentDraftQuestionIndex.toString(), Color(0xFF00C896))
        }
    }
}

@Composable
private fun BuilderMiniChip(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(0.14f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BuilderField(
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
                .background(MatteSurface)
                .border(1.dp, if (value.isNotBlank()) AlertOrange.copy(0.55f) else MatteCardBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = minHeight),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp),
                decorationBox = { inner ->
                    if (value.isBlank()) Text(placeholder, color = TextHint, fontSize = 14.sp)
                    inner()
                }
            )
        }
    }
}

@Composable
private fun DraftQuestionsPreview(state: PsychologistHomeState, language: com.example.aiphysical.presentation.auth.AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MatteSurface)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(language.pick("СОХРАНЁННЫЕ ВОПРОСЫ", "SAVED QUESTIONS", "САҚТАЛҒАН СҰРАҚТАР"), color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        state.draftQuestions.forEach { question ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PsychBackground)
                    .border(1.dp, MatteCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(AlertOrange.copy(0.18f), RoundedCornerShape(10.dp))
                            .border(1.dp, AlertOrange.copy(0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(question.order.toString(), color = AlertOrange, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(question.text, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                question.options.forEach { option ->
                    Text("• ${option.text}", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun BuilderActionButton(
    title: String,
    subtitle: String,
    accent: Color,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(accent.copy(0.9f), accent.copy(0.65f)))
                else Brush.horizontalGradient(listOf(MatteCardBorder, MatteCardBorder))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(title, color = if (enabled) Color.White else TextHint, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = if (enabled) Color.White.copy(0.78f) else TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.size(12.dp))
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(if (enabled) "→" else "•", color = if (enabled) Color.White else TextHint, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
