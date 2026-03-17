package com.example.aiphysical.ui.screens.student

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.data.model.CatState
import com.example.aiphysical.ui.theme.PsychTeal
import com.example.aiphysical.ui.theme.PsychWarning

// ══════════════════════════════════════════════════════════════════════════════
//  MascotComponent — compile-safe animated cat placeholder
//  Architecture is ready for PNG/Lottie replacement:
//    just replace the inner CatEmojiView with an Image/LottieAnimation composable
//    and keep the outer wrapper unchanged.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun MascotComponent(
    catState: CatState,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
) {
    // Gentle breathing animation for the container
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_idle")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue  = 1.03f,
        animationSpec = infiniteRepeatable(
            animation   = tween(1400, easing = FastOutSlowInEasing),
            repeatMode  = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Glow ring behind the mascot
        val glowColor = catStateGlowColor(catState)
        Box(
            modifier = Modifier
                .size(size)
                .scale(breathScale)
                .background(
                    Brush.radialGradient(
                        listOf(glowColor.copy(0.30f), glowColor.copy(0.08f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        // AnimatedContent — transitions between cat emotions
        AnimatedContent(
            targetState = catState,
            transitionSpec = {
                (scaleIn(initialScale = 0.7f, animationSpec = tween(280)) +
                 fadeIn(tween(200))) togetherWith
                (scaleOut(targetScale = 0.7f, animationSpec = tween(200)) +
                 fadeOut(tween(150)))
            },
            label = "cat_emotion"
        ) { state ->
            CatEmojiView(catState = state, size = size)
        }
    }
}

// ─── Inner cat visual (swap this with PNG/Lottie when assets are ready) ───────

@Composable
private fun CatEmojiView(catState: CatState, size: Dp) {
    val emoji       = catStateEmoji(catState)
    val label       = catStateLabel(catState)
    val borderColor = catStateGlowColor(catState)
    val bgColor     = catStateBgColor(catState)

    Column(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(bgColor)
            .border(1.5.dp, borderColor.copy(0.55f), RoundedCornerShape(size / 4)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text     = emoji,
            fontSize = (size.value * 0.42f).sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text       = label,
            color      = borderColor.copy(0.9f),
            fontSize   = (size.value * 0.10f).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center,
            maxLines   = 1
        )
    }
}

// ─── Mapping helpers ──────────────────────────────────────────────────────────

private fun catStateEmoji(state: CatState): String = when (state) {
    CatState.IDLE        -> "🐱"
    CatState.HAPPY       -> "😸"
    CatState.NERVOUS     -> "😾"
    CatState.PROUD       -> "😺"
    CatState.TIRED       -> "😿"
    CatState.ENERGETIC   -> "🐈"
    CatState.STRESS      -> "🙀"
    CatState.PEACEFUL    -> "😻"
    CatState.OVERWHELMED -> "😩"
    CatState.LAUGHING    -> "😹"
    CatState.IN_BOX      -> "📦"
}

private fun catStateLabel(state: CatState): String = when (state) {
    CatState.IDLE        -> "Жду..."
    CatState.HAPPY       -> "Радуюсь!"
    CatState.NERVOUS     -> "Нервничаю"
    CatState.PROUD       -> "Горжусь!"
    CatState.TIRED       -> "Устал..."
    CatState.ENERGETIC   -> "Энергия!"
    CatState.STRESS      -> "Ой-ой!"
    CatState.PEACEFUL    -> "Мир 🌿"
    CatState.OVERWHELMED -> "Много..."
    CatState.LAUGHING    -> "Хаха!"
    CatState.IN_BOX      -> "В коробке"
}

private fun catStateGlowColor(state: CatState): Color = when (state) {
    CatState.IDLE, CatState.PEACEFUL  -> PsychTeal
    CatState.HAPPY, CatState.PROUD,
    CatState.LAUGHING, CatState.ENERGETIC -> Color(0xFF4FD18A)
    CatState.NERVOUS, CatState.STRESS,
    CatState.OVERWHELMED               -> PsychWarning
    CatState.TIRED, CatState.IN_BOX    -> Color(0xFF9D5FF5)
}

private fun catStateBgColor(state: CatState): Color = when (state) {
    CatState.IDLE, CatState.PEACEFUL  -> Color(0xFF0D1F20)
    CatState.HAPPY, CatState.PROUD,
    CatState.LAUGHING, CatState.ENERGETIC -> Color(0xFF0D1A14)
    CatState.NERVOUS, CatState.STRESS,
    CatState.OVERWHELMED               -> Color(0xFF1A1200)
    CatState.TIRED, CatState.IN_BOX    -> Color(0xFF100B1E)
}

