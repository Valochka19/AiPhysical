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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aiphysical.composeapp.generated.resources.Res
import aiphysical.composeapp.generated.resources.enegretic_enot
import aiphysical.composeapp.generated.resources.joy_enot
import aiphysical.composeapp.generated.resources.many_enot
import aiphysical.composeapp.generated.resources.not_understand_enot
import aiphysical.composeapp.generated.resources.peace_enot
import aiphysical.composeapp.generated.resources.proud_enot
import aiphysical.composeapp.generated.resources.rejoice_enot
import aiphysical.composeapp.generated.resources.sad_enot
import aiphysical.composeapp.generated.resources.surprised_enot
import aiphysical.composeapp.generated.resources.uneasy_enot
import aiphysical.composeapp.generated.resources.wait_enot
import com.example.aiphysical.data.model.CatState
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.ui.theme.PsychTeal
import com.example.aiphysical.ui.theme.PsychWarning
import org.jetbrains.compose.resources.painterResource

// ══════════════════════════════════════════════════════════════════════════════
//  MascotComponent — compile-safe animated cat placeholder
//  Architecture is ready for PNG/Lottie replacement:
//    just replace the inner CatEmojiView with an Image/LottieAnimation composable
//    and keep the outer wrapper unchanged.
// ══════════════════════════════════════════════════════════════════════════════

enum class MascotAssetVariant {
    NONE,
    RACCOON_HAPPY,
    RACCOON_ENERGETIC,
    RACCOON_UNEASY,
    RACCOON_SAD,
    RACCOON_JOY,
    RACCOON_PEACE,
    RACCOON_PROUD,
    RACCOON_SURPRISED,
    RACCOON_MANY,
    RACCOON_WAIT,
    RACCOON_NOT_UNDERSTAND
}

@Composable
fun MascotComponent(
    catState: CatState,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    language: AppLanguage = AppLanguage.RU,
    assetVariant: MascotAssetVariant = MascotAssetVariant.NONE,
) {
    val resolvedAssetVariant = when {
        assetVariant != MascotAssetVariant.NONE -> assetVariant
        catState == CatState.HAPPY -> MascotAssetVariant.RACCOON_HAPPY
        catState == CatState.IN_BOX -> MascotAssetVariant.RACCOON_NOT_UNDERSTAND
        catState == CatState.IDLE -> MascotAssetVariant.RACCOON_WAIT
        catState == CatState.OVERWHELMED -> MascotAssetVariant.RACCOON_MANY
        catState == CatState.STRESS -> MascotAssetVariant.RACCOON_SURPRISED
        catState == CatState.PROUD -> MascotAssetVariant.RACCOON_PROUD
        catState == CatState.PEACEFUL -> MascotAssetVariant.RACCOON_PEACE
        catState == CatState.LAUGHING -> MascotAssetVariant.RACCOON_JOY
        catState == CatState.TIRED -> MascotAssetVariant.RACCOON_SAD
        catState == CatState.NERVOUS -> MascotAssetVariant.RACCOON_UNEASY
        else -> MascotAssetVariant.NONE
    }

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
            targetState = resolvedAssetVariant to catState,
            transitionSpec = {
                (scaleIn(initialScale = 0.7f, animationSpec = tween(280)) +
                 fadeIn(tween(200))) togetherWith
                (scaleOut(targetScale = 0.7f, animationSpec = tween(200)) +
                 fadeOut(tween(150)))
            },
            label = "cat_emotion"
        ) { (variant, state) ->
            when (variant) {
                MascotAssetVariant.NONE -> CatEmojiView(catState = state, size = size, language = language)
                MascotAssetVariant.RACCOON_HAPPY -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.rejoice_enot),
                    contentDescription = localizedRaccoonDescription(language, "base")
                )
                MascotAssetVariant.RACCOON_ENERGETIC -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.enegretic_enot),
                    contentDescription = localizedRaccoonDescription(language, "energetic")
                )
                MascotAssetVariant.RACCOON_UNEASY -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.uneasy_enot),
                    contentDescription = localizedRaccoonDescription(language, "uneasy")
                )
                MascotAssetVariant.RACCOON_SAD -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.sad_enot),
                    contentDescription = localizedRaccoonDescription(language, "sad")
                )
                MascotAssetVariant.RACCOON_JOY -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.joy_enot),
                    contentDescription = localizedRaccoonDescription(language, "joy")
                )
                MascotAssetVariant.RACCOON_PEACE -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.peace_enot),
                    contentDescription = localizedRaccoonDescription(language, "peace")
                )
                MascotAssetVariant.RACCOON_PROUD -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.proud_enot),
                    contentDescription = localizedRaccoonDescription(language, "proud")
                )
                MascotAssetVariant.RACCOON_SURPRISED -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.surprised_enot),
                    contentDescription = localizedRaccoonDescription(language, "surprised")
                )
                MascotAssetVariant.RACCOON_MANY -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.many_enot),
                    contentDescription = localizedRaccoonDescription(language, "overwhelmed")
                )
                MascotAssetVariant.RACCOON_WAIT -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.wait_enot),
                    contentDescription = localizedRaccoonDescription(language, "waiting")
                )
                MascotAssetVariant.RACCOON_NOT_UNDERSTAND -> RaccoonAssetView(
                    catState = state,
                    size = size,
                    painter = painterResource(Res.drawable.not_understand_enot),
                    contentDescription = localizedRaccoonDescription(language, "confused")
                )
            }
        }
    }
}

private fun localizedRaccoonDescription(language: AppLanguage, mood: String): String = when (language) {
    AppLanguage.RU -> when (mood) {
        "base" -> "Енот"
        "energetic" -> "Энергичный енот"
        "uneasy" -> "Тревожный енот"
        "sad" -> "Грустный енот"
        "joy" -> "Весёлый енот"
        "peace" -> "Спокойный енот"
        "proud" -> "Гордый енот"
        "surprised" -> "Удивлённый енот"
        "overwhelmed" -> "Перегруженный енот"
        "waiting" -> "Ждущий енот"
        else -> "Растерянный енот"
    }
    AppLanguage.EN -> when (mood) {
        "base" -> "Raccoon"
        "energetic" -> "Energetic raccoon"
        "uneasy" -> "Anxious raccoon"
        "sad" -> "Sad raccoon"
        "joy" -> "Happy raccoon"
        "peace" -> "Calm raccoon"
        "proud" -> "Proud raccoon"
        "surprised" -> "Surprised raccoon"
        "overwhelmed" -> "Overwhelmed raccoon"
        "waiting" -> "Waiting raccoon"
        else -> "Confused raccoon"
    }
    AppLanguage.KZ -> when (mood) {
        "base" -> "Жанат"
        "energetic" -> "Қуатты жанат"
        "uneasy" -> "Алаңдаулы жанат"
        "sad" -> "Мұңды жанат"
        "joy" -> "Көңілді жанат"
        "peace" -> "Сабырлы жанат"
        "proud" -> "Мақтанышты жанат"
        "surprised" -> "Таңғалған жанат"
        "overwhelmed" -> "Шамадан тыс жүктелген жанат"
        "waiting" -> "Күтіп тұрған жанат"
        else -> "Аң-таң жанат"
    }
}

// ─── Inner cat visual (swap this with PNG/Lottie when assets are ready) ───────

@Composable
private fun CatEmojiView(catState: CatState, size: Dp, language: AppLanguage) {
    val emoji       = catStateEmoji(catState)
    val label       = catStateLabel(catState, language)
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

@Composable
private fun RaccoonAssetView(
    catState: CatState,
    size: Dp,
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
) {
    val borderColor = catStateGlowColor(catState)
    val bgColor = catStateBgColor(catState)

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(bgColor)
            .border(1.5.dp, borderColor.copy(0.55f), RoundedCornerShape(size / 4))
            .padding((size * 0.04f).coerceAtLeast(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
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

private fun catStateLabel(state: CatState, language: AppLanguage): String = when (language) {
    AppLanguage.RU -> when (state) {
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
    AppLanguage.EN -> when (state) {
        CatState.IDLE        -> "Waiting..."
        CatState.HAPPY       -> "So happy!"
        CatState.NERVOUS     -> "Feeling nervous"
        CatState.PROUD       -> "So proud!"
        CatState.TIRED       -> "Tired..."
        CatState.ENERGETIC   -> "Full of energy!"
        CatState.STRESS      -> "Uh-oh!"
        CatState.PEACEFUL    -> "Calm 🌿"
        CatState.OVERWHELMED -> "Too much..."
        CatState.LAUGHING    -> "Haha!"
        CatState.IN_BOX      -> "In the box"
    }
    AppLanguage.KZ -> when (state) {
        CatState.IDLE        -> "Күтіп тұрмын..."
        CatState.HAPPY       -> "Қуанып тұрмын!"
        CatState.NERVOUS     -> "Алаңдап тұрмын"
        CatState.PROUD       -> "Мақтанып тұрмын!"
        CatState.TIRED       -> "Шаршадым..."
        CatState.ENERGETIC   -> "Қуат көп!"
        CatState.STRESS      -> "Ойпырмай!"
        CatState.PEACEFUL    -> "Тыныштық 🌿"
        CatState.OVERWHELMED -> "Тым көп..."
        CatState.LAUGHING    -> "Хаха!"
        CatState.IN_BOX      -> "Қораптың ішінде"
    }
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

