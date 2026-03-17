package com.example.aiphysical.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.ui.theme.*

// ─── Glass Modifier ──────────────────────────────────────────────────────────

fun Modifier.glassCard(
    cornerRadius: Dp = 20.dp,
    bgAlpha: Float = 0.12f,
    borderAlpha: Float = 0.28f,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = bgAlpha + 0.06f),
                Color.White.copy(alpha = bgAlpha)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha + 0.15f),
                Color.White.copy(alpha = borderAlpha)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

// ─── Matte Card Modifier (clean, expensive look) ─────────────────────────────

fun Modifier.matteCard(cornerRadius: Dp = 20.dp): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color.White.copy(alpha = 0.05f))
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(0.18f), Color.White.copy(0.04f))
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

// ─── Animated Background ─────────────────────────────────────────────────────

@Composable
fun AnimatedBackground(
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val infiniteTransition = if (animate) rememberInfiniteTransition(label = "bg_anim") else null
    val pulse1 = if (animate) {
        infiniteTransition!!.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulse1"
        ).value
    } else 0.35f
    val pulse2 = if (animate) {
        infiniteTransition!!.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulse2"
        ).value
    } else 0.55f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundDeep, BackgroundMid, BackgroundDark)
                )
            )
    ) {
        // Violet orb — top-left
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = (-90 + pulse1 * 50).dp, y = (-80 + pulse1 * 40).dp)
                .background(
                    Brush.radialGradient(listOf(VioletPrimary.copy(alpha = 0.28f), Color.Transparent)),
                    CircleShape
                )
        )
        // Pink orb — bottom-right
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (70 - pulse2 * 50).dp, y = (70 - pulse2 * 40).dp)
                .background(
                    Brush.radialGradient(listOf(AccentPink.copy(alpha = 0.20f), Color.Transparent)),
                    CircleShape
                )
        )
        // Cyan orb — center-right
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 90.dp, y = (pulse1 * 60 - 30).dp)
                .background(
                    Brush.radialGradient(listOf(AccentCyan.copy(alpha = 0.12f), Color.Transparent)),
                    CircleShape
                )
        )
        // Content
        content()
    }
}

// ─── Language Switcher ───────────────────────────────────────────────────────

@Composable
fun LanguageSwitcher(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .glassCard(cornerRadius = 12.dp, bgAlpha = 0.18f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🌐", fontSize = 15.sp)
                Text(
                    text = currentLanguage.displayName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(SurfaceDeep)
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        ) {
            AppLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (lang == currentLanguage) "●" else "○",
                                color = if (lang == currentLanguage) VioletGlow else TextHint,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${lang.displayName} (${lang.code.uppercase()})",
                                color = if (lang == currentLanguage) VioletGlow else TextPrimary,
                                fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    },
                    onClick = { onLanguageChange(lang); expanded = false }
                )
            }
        }
    }
}

// ─── Glass Text Field ────────────────────────────────────────────────────────

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingEmoji: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val visualTransformation = if (isPassword && !passwordVisible)
        PasswordVisualTransformation() else VisualTransformation.None

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = if (isError) ErrorColor else TextSecondary, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = if (isError) ErrorColor else VioletPrimary,
                unfocusedBorderColor = if (isError) ErrorColor.copy(alpha = 0.6f) else GlassBorder,
                cursorColor = VioletGlow,
                focusedLabelColor = if (isError) ErrorColor else VioletGlow,
                unfocusedLabelColor = TextSecondary,
                focusedContainerColor = GlassBg,
                unfocusedContainerColor = GlassBg,
            ),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            isError = isError,
            singleLine = true,
            leadingIcon = leadingEmoji?.let {
                { Text(it, fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp)) }
            },
            trailingIcon = if (isPassword) {
                {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "🙈" else "👁",
                            fontSize = 16.sp
                        )
                    }
                }
            } else null
        )
        if (isError && errorMessage != null) {
            Text(
                text = "⚠ $errorMessage",
                color = ErrorColor,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 14.dp, top = 2.dp)
            )
        }
    }
}

// ─── Primary Button ──────────────────────────────────────────────────────────

@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val alpha = if (enabled && !isLoading) 1f else 0.5f
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            VioletPrimary.copy(alpha = alpha),
                            AccentPink.copy(alpha = alpha)
                        )
                    ),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─── Error Banner ─────────────────────────────────────────────────────────────

@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 14.dp, bgAlpha = 0.06f, borderAlpha = 0.15f)
            .background(ErrorColor.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚠️", fontSize = 18.sp)
            Text(
                text = message,
                color = Color(0xFFFFB3B3),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        TextButton(onClick = onDismiss) {
            Text("✕", color = ErrorColor, fontSize = 14.sp)
        }
    }
}

// ─── Section Divider with text ───────────────────────────────────────────────

@Composable
fun OrDivider(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = GlassBorder)
        Text(text, color = TextHint, fontSize = 12.sp)
        HorizontalDivider(modifier = Modifier.weight(1f), color = GlassBorder)
    }
}

// ─── Shimmer Effect Modifier ──────────────────────────────────────────────────

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -600f, targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmerX"
    )
    return this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.07f),
                    Color.White.copy(alpha = 0.13f),
                    Color.White.copy(alpha = 0.07f),
                    Color.Transparent
                ),
                start = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
                end = androidx.compose.ui.geometry.Offset(shimmerX + 500f, size.height)
            )
        )
    }
}

// ─── Director Background (NeonBackground + Updated Orbs) ─────────────────────

@Composable
fun DirectorBackground(content: @Composable BoxScope.() -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "dir_bg")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "dp1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "dp2"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NeonBackground, Color(0xFF0E0E2A), Color(0xFF080816))
                )
            )
    ) {
        // Neon Violet orb — top left
        Box(
            modifier = Modifier
                .size(380.dp)
                .offset(x = (-100 + pulse1 * 60).dp, y = (-100 + pulse1 * 50).dp)
                .background(
                    Brush.radialGradient(listOf(NeonViolet.copy(alpha = 0.22f), Color.Transparent)),
                    CircleShape
                )
        )
        // Cyan orb — bottom right
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (60 - pulse2 * 40).dp, y = (60 - pulse2 * 30).dp)
                .background(
                    Brush.radialGradient(listOf(CyanAccent.copy(alpha = 0.18f), Color.Transparent)),
                    CircleShape
                )
        )
        // Alert orange accent — center left
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-100).dp, y = (pulse1 * 60 - 30).dp)
                .background(
                    Brush.radialGradient(listOf(AlertOrange.copy(alpha = 0.08f), Color.Transparent)),
                    CircleShape
                )
        )
        content()
    }
}

