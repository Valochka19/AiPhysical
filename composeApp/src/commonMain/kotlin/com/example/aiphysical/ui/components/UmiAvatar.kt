package com.example.aiphysical.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun UmiAvatar(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Уми",
)

@Composable
fun UmiAvatarBadge(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    backgroundBrush: Brush? = null,
    backgroundColor: Color = Color.Transparent,
    borderBrush: Brush? = null,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 1.dp,
    imagePadding: Dp = 0.dp,
    contentDescription: String? = "Уми",
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(
                when {
                    backgroundBrush != null -> Modifier.background(backgroundBrush, shape)
                    backgroundColor != Color.Transparent -> Modifier.background(backgroundColor, shape)
                    else -> Modifier
                }
            )
            .then(
                when {
                    borderBrush != null -> Modifier.border(borderWidth, borderBrush, shape)
                    borderColor != Color.Transparent -> Modifier.border(borderWidth, borderColor, shape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        UmiAvatar(
            modifier = Modifier
                .fillMaxSize()
                .padding(imagePadding),
            contentDescription = contentDescription
        )
    }
}

@Composable
fun FloatingUmiAvatarBadge(
    modifier: Modifier = Modifier,
    levitationAmplitude: Dp = 9.dp,
    durationMillis: Int = 2800,
    shape: Shape = CircleShape,
    backgroundBrush: Brush? = null,
    backgroundColor: Color = Color.Transparent,
    borderBrush: Brush? = null,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 1.dp,
    imagePadding: Dp = 0.dp,
    contentDescription: String? = "Уми",
) {
    val density = LocalDensity.current
    val amplitudePx = with(density) { levitationAmplitude.toPx() }
    val transition = rememberInfiniteTransition(label = "umi_levitation")
    val translationY = transition.animateFloat(
        initialValue = -amplitudePx,
        targetValue = amplitudePx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "umi_levitation_y"
    ).value

    Box(
        modifier = modifier.graphicsLayer {
            this.translationY = translationY
        },
        contentAlignment = Alignment.Center
    ) {
        UmiAvatarBadge(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            backgroundBrush = backgroundBrush,
            backgroundColor = backgroundColor,
            borderBrush = borderBrush,
            borderColor = borderColor,
            borderWidth = borderWidth,
            imagePadding = imagePadding,
            contentDescription = contentDescription
        )
    }
}

