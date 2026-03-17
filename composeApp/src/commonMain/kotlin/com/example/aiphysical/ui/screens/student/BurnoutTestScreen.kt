package com.example.aiphysical.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.aiphysical.data.model.*
import com.example.aiphysical.presentation.student.StudentEvent
import com.example.aiphysical.presentation.student.StudentViewModel
import com.example.aiphysical.ui.theme.*
import kotlinx.coroutines.delay

// ══════════════════════════════════════════════════════════════════════════════
//  BurnoutTestScreen — full-screen overlay composable
//  Tinder/Reigns-inspired card flow with animated mascot
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun BurnoutTestScreen(
    testState: BurnoutTestUiState,
    vm: StudentViewModel,
) {
    // Intercept back-press gesture by consuming the full screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B1E))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        when (val step = testState.step) {
            is BurnoutTestStep.Questions    -> QuestionsContent(testState, vm)
            is BurnoutTestStep.LoadingResult -> LoadingResultContent()
            is BurnoutTestStep.Result       -> ResultContent(step, testState.errorMessage, vm)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Questions phase
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun QuestionsContent(testState: BurnoutTestUiState, vm: StudentViewModel) {
    val currentIndex = testState.currentQuestionIndex
    val question     = BURNOUT_QUESTIONS.getOrNull(currentIndex)
    val total        = BURNOUT_QUESTIONS.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top bar: progress + close ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${currentIndex + 1} / $total",
                color = Color.White.copy(0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // Close / exit button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.07f))
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { vm.onEvent(StudentEvent.CloseBurnoutTest) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("✕ Закрыть", color = Color.White.copy(0.55f), fontSize = 12.sp)
            }
        }

        // Progress bar
        BurnoutProgressBar(current = currentIndex, total = total)

        // ── Mascot block ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MascotComponent(
                catState = question?.catEmotion ?: CatState.IDLE,
                size = 104.dp
            )
        }

        // ── Question card (animated on change) ────────────────────────────────
        AnimatedContent(
            targetState = question,
            transitionSpec = {
                (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(250))) togetherWith
                (slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(180)))
            },
            label = "question_card"
        ) { q ->
            QuestionCard(text = q?.text ?: "")
        }

        Spacer(Modifier.weight(1f))

        // ── Answer buttons ────────────────────────────────────────────────────
        AnswerButtonsColumn(
            isAnswering = testState.isAnswering,
            onAnswer    = { vm.onEvent(StudentEvent.AnswerBurnoutQuestion(it)) }
        )
    }
}

@Composable
private fun BurnoutProgressBar(current: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue = if (total > 0) current.toFloat() / total.toFloat() else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "burnout_progress"
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
                Brush.verticalGradient(
                    listOf(Color(0xFF252550), Color(0xFF252550).copy(0.3f))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            color      = Color.White,
            fontSize   = 22.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 31.sp,
            textAlign  = TextAlign.Center
        )
    }
}

@Composable
private fun AnswerButtonsColumn(
    isAnswering: Boolean,
    onAnswer: (AnswerType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
                    text       = answerType.label,
                    color      = if (!isAnswering) Color.White.copy(0.92f) else Color.White.copy(0.35f),
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun answerTypeBorderColor(type: AnswerType): Color = when (type) {
    AnswerType.EXACTLY_ME    -> PsychCritical
    AnswerType.SIMILAR       -> PsychWarning
    AnswerType.NEUTRAL       -> Color(0xFF9D5FF5)
    AnswerType.PROBABLY_NOT  -> PsychTeal
    AnswerType.NOT_ME        -> Color(0xFF4FD18A)
}

// ══════════════════════════════════════════════════════════════════════════════
//  Loading phase — Gemini is thinking
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LoadingResultContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pulsing mascot
        val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.90f,
            targetValue  = 1.10f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse_scale"
        )
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            MascotComponent(catState = CatState.IDLE, size = 120.dp)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Кот думает...",
            color      = PsychTeal,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Анализирую твои ответы и готовлю результат 🐾",
            color     = Color.White.copy(0.5f),
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 40.dp)
        )
        Spacer(Modifier.height(28.dp))
        CircularProgressIndicator(
            color = PsychTeal,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(36.dp)
        )
    }
}


// ══════════════════════════════════════════════════════════════════════════════
//  Result phase — typewriter AI feedback
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResultContent(
    result: BurnoutTestStep.Result,
    errorMessage: String?,
    vm: StudentViewModel,
) {
    val statusColor = when (result.aiAssessment) {
        "normal"   -> Color(0xFF4FD18A)
        "stress"   -> PsychWarning
        "critical" -> PsychCritical
        else       -> PsychTeal
    }
    val statusLabel = when (result.aiAssessment) {
        "normal"   -> "Всё хорошо 😊"
        "stress"   -> "Умеренный стресс 😐"
        "critical" -> "Высокое выгорание 🔥"
        else       -> "Результат получен"
    }
    val mascotForResult = when (result.aiAssessment) {
        "normal"   -> CatState.HAPPY
        "stress"   -> CatState.NERVOUS
        "critical" -> CatState.TIRED
        else       -> CatState.PROUD
    }

    // Typewriter effect
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
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Результат теста",
                color = Color.White.copy(0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Mascot + status badge
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MascotComponent(catState = mascotForResult, size = 110.dp)
            Box(
                Modifier
                    .background(statusColor.copy(0.15f), RoundedCornerShape(20.dp))
                    .border(1.dp, statusColor.copy(0.50f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Text(statusLabel, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Score row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Wellness Score: ",
                color = Color.White.copy(0.5f),
                fontSize = 14.sp
            )
            Text(
                "${result.score}/100",
                color = statusColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // AI Feedback card with typewriter text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161632))
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(statusColor.copy(0.35f), statusColor.copy(0.08f))),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "🐾 Кот говорит:",
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text       = displayedText,
                    color      = Color.White.copy(0.88f),
                    fontSize   = 15.sp,
                    lineHeight = 23.sp
                )
            }
        }

        // Error note if any
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

        // Action buttons
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Close
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(listOf(PsychTeal.copy(0.22f), Color(0xFF9D5FF5).copy(0.18f)))
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PsychTeal.copy(0.60f), Color(0xFF9D5FF5).copy(0.50f))),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { vm.onEvent(StudentEvent.CloseBurnoutTest) }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✓ Закрыть тест", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            // Retake
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.04f))
                    .border(1.dp, Color.White.copy(0.10f), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { vm.onEvent(StudentEvent.ResetBurnoutTest) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("↺ Пройти заново", color = Color.White.copy(0.55f), fontSize = 14.sp)
            }
        }
    }
}

