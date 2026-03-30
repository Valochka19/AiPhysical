package com.example.aiphysical.ui.screens.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.AnswerType
import com.example.aiphysical.data.model.BurnoutTestUiState
import com.example.aiphysical.data.model.CatState
import com.example.aiphysical.data.model.MetricSemantics
import com.example.aiphysical.data.model.StudentTestDefinition
import com.example.aiphysical.data.model.StudentTestStep
import com.example.aiphysical.data.model.StudentTestUiState
import com.example.aiphysical.data.model.displayLabel
import com.example.aiphysical.presentation.student.StudentEvent
import com.example.aiphysical.presentation.student.StudentTestType
import com.example.aiphysical.presentation.student.StudentViewModel
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.ui.theme.PsychCritical
import com.example.aiphysical.ui.theme.PsychTeal
import com.example.aiphysical.ui.theme.PsychWarning
import kotlinx.coroutines.delay

@Composable
fun StudentTestScreen(
    testState: StudentTestUiState,
    language: AppLanguage = AppLanguage.RU,
    vm: StudentViewModel,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B1E))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        when (val step = testState.step) {
            StudentTestStep.Questions -> QuestionsContent(testState, language, vm)
            StudentTestStep.LoadingResult -> LoadingResultContent(testState.definition.localizedTestName(language), language)
            is StudentTestStep.Result -> ResultContent(testState.definition, step, testState.errorMessage, language, vm)
        }
    }
}

@Composable
fun BurnoutTestScreen(
    testState: BurnoutTestUiState,
    language: AppLanguage = AppLanguage.RU,
    vm: StudentViewModel,
) = StudentTestScreen(testState = testState, language = language, vm = vm)

@Composable
private fun QuestionsContent(testState: StudentTestUiState, language: AppLanguage, vm: StudentViewModel) {
    val definition = testState.definition
    val currentIndex = testState.currentQuestionIndex
    val question = definition.questions.getOrNull(currentIndex)
    val total = definition.questions.size
    val questionScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(questionScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        definition.localizedTestName(language),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "${currentIndex + 1} / $total",
                        color = Color.White.copy(0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.07f))
                        .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { vm.onEvent(StudentEvent.CloseActiveTest) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(language.pick("✕ Закрыть", "✕ Close", "✕ Жабу"), color = Color.White.copy(0.55f), fontSize = 12.sp)
                }
            }

            StudentTestProgressBar(current = currentIndex, total = total)

            val mascotAssetVariant = question?.let {
                questionMascotAssetVariant(definition.type, it.id)
            } ?: MascotAssetVariant.NONE

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MascotComponent(
                    catState = question?.catEmotion ?: CatState.IDLE,
                    size = 104.dp,
                    language = language,
                    assetVariant = mascotAssetVariant
                )
            }

            AnimatedContent(
                targetState = question,
                transitionSpec = {
                    (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(250))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(180)))
                },
                label = "student_question_card"
            ) { q ->
                QuestionCard(text = q?.let { definition.localizedQuestionText(it.id, language) } ?: "")
            }
        }

        AnswerButtonsColumn(
            language = language,
            modifier = Modifier.padding(bottom = 8.dp),
            isAnswering = testState.isAnswering,
            onAnswer = { vm.onEvent(StudentEvent.AnswerCurrentTestQuestion(it)) }
        )
    }
}

private fun questionMascotAssetVariant(
    testType: StudentTestType,
    questionId: Int,
): MascotAssetVariant = when {
    testType == StudentTestType.BURNOUT && questionId == 1 -> MascotAssetVariant.RACCOON_HAPPY
    testType == StudentTestType.EMOTION && questionId == 1 -> MascotAssetVariant.RACCOON_HAPPY
    testType == StudentTestType.BURNOUT && questionId == 5 -> MascotAssetVariant.RACCOON_ENERGETIC
    testType == StudentTestType.STRESS && questionId == 1 -> MascotAssetVariant.RACCOON_ENERGETIC
    testType == StudentTestType.EMOTION && questionId == 3 -> MascotAssetVariant.RACCOON_ENERGETIC
    testType == StudentTestType.MOTIVATION && questionId == 1 -> MascotAssetVariant.RACCOON_ENERGETIC
    testType == StudentTestType.MOTIVATION && questionId == 7 -> MascotAssetVariant.RACCOON_ENERGETIC
    testType == StudentTestType.ANXIETY && questionId == 3 -> MascotAssetVariant.RACCOON_ENERGETIC
    else -> MascotAssetVariant.NONE
}

@Composable
private fun StudentTestProgressBar(current: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue = if (total > 0) current.toFloat() / total.toFloat() else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "student_test_progress"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(0.10f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(listOf(PsychTeal, Color(0xFF9D5FF5))),
                    RoundedCornerShape(3.dp)
                )
        )
    }
}

@Composable
private fun QuestionCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF161632))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0xFF252550), Color(0xFF252550).copy(0.3f))),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 31.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnswerButtonsColumn(
    language: AppLanguage,
    modifier: Modifier = Modifier,
    isAnswering: Boolean,
    onAnswer: (AnswerType) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        AnswerType.entries.forEach { answerType ->
            val borderColor = answerTypeBorderColor(answerType)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                borderColor.copy(if (!isAnswering) 0.14f else 0.06f),
                                Color(0xFF161632).copy(if (!isAnswering) 0.95f else 0.7f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        borderColor.copy(if (!isAnswering) 0.45f else 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        enabled = !isAnswering,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAnswer(answerType) }
                    .padding(horizontal = 18.dp, vertical = 13.dp)
            ) {
                Text(
                    text = answerType.displayLabel(language),
                    color = if (!isAnswering) Color.White.copy(0.92f) else Color.White.copy(0.35f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun answerTypeBorderColor(type: AnswerType): Color = when (type) {
    AnswerType.EXACTLY_ME -> PsychCritical
    AnswerType.SIMILAR -> PsychWarning
    AnswerType.NEUTRAL -> Color(0xFF9D5FF5)
    AnswerType.PROBABLY_NOT -> PsychTeal
    AnswerType.NOT_ME -> Color(0xFF4FD18A)
}

@Composable
private fun LoadingResultContent(testName: String, language: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")
        // Use State<Float> reference — read .value inside graphicsLayer (draw phase) to avoid
        // recomposing the entire LoadingResultContent every animation frame.
        val scaleState = infiniteTransition.animateFloat(
            initialValue = 0.90f,
            targetValue = 1.10f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse_scale"
        )
        Box(modifier = Modifier.graphicsLayer { scaleX = scaleState.value; scaleY = scaleState.value }) {
            MascotComponent(catState = CatState.IDLE, size = 120.dp, language = language)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            language.pick("Анализ данных..", "Analyzing data..", "Деректер талданып жатыр.."),
            color = PsychTeal,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            language.pick(
                "Изучаю твои ответы по тесту «$testName» и собираю содержательный разбор.",
                "I'm reviewing your answers for the \"$testName\" test and preparing a clear summary.",
                "«$testName» тесті бойынша жауаптарыңды қарап, мазмұнды қорытынды дайындап жатырмын."
            ),
            color = Color.White.copy(0.5f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        Spacer(Modifier.height(28.dp))
        CircularProgressIndicator(
            color = PsychTeal,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun ResultContent(
    definition: StudentTestDefinition,
    result: StudentTestStep.Result,
    errorMessage: String?,
    language: AppLanguage,
    vm: StudentViewModel,
) {
    val statusColor = when (result.aiAssessment) {
        "normal" -> Color(0xFF4FD18A)
        "stress" -> PsychWarning
        "critical" -> PsychCritical
        else -> PsychTeal
    }
    val statusLabel = resultStatusLabel(definition, result.aiAssessment, language)
    val mascotForResult = resultMascot(definition, result.aiAssessment)
    val showRetry = !errorMessage.isNullOrBlank() && (
        errorMessage.contains("AI", ignoreCase = true) ||
            errorMessage.contains("Gemini", ignoreCase = true)
        )

    var displayedText by remember { mutableStateOf("") }
    LaunchedEffect(result.feedbackText) {
        displayedText = ""
        for (i in result.feedbackText.indices) {
            displayedText = result.feedbackText.substring(0, i + 1)
            delay(18L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                definition.localizedTestName(language),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                language.pick("Разбор результата", "Result overview", "Нәтиже талдауы"),
                color = Color.White.copy(0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MascotComponent(catState = mascotForResult, size = 110.dp, language = language)
            Box(
                Modifier
                    .background(statusColor.copy(0.15f), RoundedCornerShape(20.dp))
                    .border(1.dp, statusColor.copy(0.50f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Text(statusLabel, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                language.pick("Результат: ${result.score}/100", "Result: ${result.score}/100", "Нәтиже: ${result.score}/100"),
                color = statusColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                when (definition.semantics) {
                    MetricSemantics.HIGH_IS_BAD -> language.pick("Для этого теста высокий процент означает более выраженный риск.", "For this test, a higher percentage means a more pronounced risk.", "Бұл тестте жоғары пайыз қауіптің айқынырақ екенін білдіреді.")
                    MetricSemantics.HIGH_IS_GOOD -> language.pick("Для этого теста высокий процент означает более устойчивое состояние.", "For this test, a higher percentage means a more stable condition.", "Бұл тестте жоғары пайыз жағдайдың тұрақтырақ екенін білдіреді.")
                },
                color = Color.White.copy(0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF171733), Color(0xFF131328))
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(statusColor.copy(0.35f), statusColor.copy(0.08f))),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    language.pick("Разбор AI-психолога", "AI psychologist summary", "AI психолог қорытындысы"),
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = displayedText,
                    color = Color.White.copy(0.92f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PsychWarning.copy(0.08f))
                    .border(1.dp, PsychWarning.copy(0.25f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(errorMessage, color = PsychWarning.copy(0.8f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (showRetry) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(0.04f))
                        .border(1.dp, PsychWarning.copy(0.30f), RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { vm.onEvent(StudentEvent.RetryCurrentTestGemini) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(language.pick("↻ Повторить AI-анализ", "↻ Retry AI analysis", "↻ AI талдауын қайталау"), color = PsychWarning, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(PsychTeal.copy(0.22f), Color(0xFF9D5FF5).copy(0.18f))))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PsychTeal.copy(0.60f), Color(0xFF9D5FF5).copy(0.50f))),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { vm.onEvent(StudentEvent.CloseActiveTest) }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(language.pick("✓ Закрыть тест", "✓ Close test", "✓ Тестті жабу"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.04f))
                    .border(1.dp, Color.White.copy(0.10f), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { vm.onEvent(StudentEvent.ResetCurrentTest) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(language.pick("↺ Пройти заново", "↺ Retake", "↺ Қайта өту"), color = Color.White.copy(0.55f), fontSize = 14.sp)
            }
        }
    }
}

private fun resultMascot(definition: StudentTestDefinition, assessment: String): CatState = when (assessment) {
    "normal" -> when (definition.type) {
        StudentTestType.BURNOUT, StudentTestType.STRESS, StudentTestType.ANXIETY -> CatState.HAPPY
        StudentTestType.EMOTION -> CatState.PEACEFUL
        StudentTestType.MOTIVATION -> CatState.PROUD
    }

    "stress" -> when (definition.type) {
        StudentTestType.BURNOUT -> CatState.TIRED
        StudentTestType.STRESS -> CatState.STRESS
        StudentTestType.EMOTION -> CatState.IDLE
        StudentTestType.MOTIVATION -> CatState.NERVOUS
        StudentTestType.ANXIETY -> CatState.NERVOUS
    }

    else -> when (definition.type) {
        StudentTestType.BURNOUT -> CatState.IN_BOX
        StudentTestType.STRESS -> CatState.OVERWHELMED
        StudentTestType.EMOTION -> CatState.TIRED
        StudentTestType.MOTIVATION -> CatState.TIRED
        StudentTestType.ANXIETY -> CatState.STRESS
    }
}

private fun resultStatusLabel(definition: StudentTestDefinition, assessment: String, language: AppLanguage): String = when (definition.type) {
    StudentTestType.BURNOUT -> when (assessment) {
        "normal" -> language.pick("Ресурс в норме 😊", "Resource level is normal 😊", "Ресурс қалыпты 😊")
        "stress" -> language.pick("Усталость накапливается 😐", "Fatigue is building up 😐", "Шаршау жиналып келеді 😐")
        else -> language.pick("Высокое выгорание 🔥", "High burnout 🔥", "Күйіп кету жоғары 🔥")
    }

    StudentTestType.STRESS -> when (assessment) {
        "normal" -> language.pick("Стресс под контролем 🌿", "Stress is under control 🌿", "Стресс бақылауда 🌿")
        "stress" -> language.pick("Напряжение повышено ⚡", "Tension is elevated ⚡", "Кернеу жоғарылаған ⚡")
        else -> language.pick("Сильный стресс 🚨", "High stress 🚨", "Қатты стресс 🚨")
    }

    StudentTestType.EMOTION -> when (assessment) {
        "normal" -> language.pick("Фон довольно устойчивый 🌤", "Your emotional state is quite stable 🌤", "Эмоциялық фон жеткілікті тұрақты 🌤")
        "stress" -> language.pick("Нужно чуть больше ресурса 🌿", "You need a bit more energy 🌿", "Сәл көбірек ресурс керек 🌿")
        else -> language.pick("Эмоциональный фон просел 🌧", "Your emotional state has dropped 🌧", "Эмоциялық фон төмендеген 🌧")
    }

    StudentTestType.MOTIVATION -> when (assessment) {
        "normal" -> language.pick("Мотивация живая 🚀", "Motivation is alive 🚀", "Мотивация жақсы 🚀")
        "stress" -> language.pick("Нужна перезагрузка 🧭", "A reset is needed 🧭", "Қайта қуаттану керек 🧭")
        else -> language.pick("Мотивация заметно снижена 🪫", "Motivation is noticeably low 🪫", "Мотивация айқын төмендеген 🪫")
    }

    StudentTestType.ANXIETY -> when (assessment) {
        "normal" -> language.pick("Тревога в пределах нормы ☀️", "Anxiety is within the normal range ☀️", "Мазасыздық қалыпты шекте ☀️")
        "stress" -> language.pick("Тревога повышена ☁️", "Anxiety is elevated ☁️", "Мазасыздық жоғарылаған ☁️")
        else -> language.pick("Высокая тревожность 🌩", "High anxiety 🌩", "Мазасыздық жоғары 🌩")
    }
}
