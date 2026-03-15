package com.example.aiphysical.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColorScheme = darkColorScheme(
    primary            = VioletPrimary,
    onPrimary          = Color.White,
    primaryContainer   = VioletDark,
    onPrimaryContainer = VioletGlow,
    secondary          = AccentPink,
    onSecondary        = Color.White,
    background         = BackgroundDeep,
    onBackground       = TextPrimary,
    surface            = SurfaceDeep,
    onSurface          = OnSurface,
    surfaceVariant     = BackgroundMid,
    onSurfaceVariant   = TextSecondary,
    error              = ErrorColor,
    onError            = Color.White,
    outline            = GlassBorder,
    outlineVariant     = GlassBorderBright,
    inverseSurface     = GlassBg,
)

@Composable
fun AIPhysicalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        content = content
    )
}

