package com.example.aiphysical.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

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

/**
 * Maximum allowed system font scale.
 * Users who set "Huge" or "Largest" fonts in Android Accessibility can break
 * fixed-size card layouts. Capping at 1.15f gives a modest large-text boost
 * while keeping all UI elements within their designed bounds.
 */
private const val MAX_FONT_SCALE = 1.15f

@Composable
fun AIPhysicalTheme(content: @Composable () -> Unit) {

    // ── Font-scale guard ──────────────────────────────────────────────────────
    // Read the system-provided density once. remember(currentDensity) means this
    // block only re-executes when the system density actually changes (e.g. the
    // user rotates the device or changes the display size in Settings) — NOT on
    // every recomposition, so there is zero extra overhead in the normal render path.
    //
    // If fontScale is already within limits we return the SAME Density object
    // (no allocation). Only when it exceeds MAX_FONT_SCALE do we create a new
    // Density that clamps the scale while keeping the physical pixel density intact.
    val currentDensity = LocalDensity.current
    val safeDensity = remember(currentDensity) {
        if (currentDensity.fontScale <= MAX_FONT_SCALE) {
            currentDensity                          // already fine — no allocation
        } else {
            Density(
                density   = currentDensity.density, // preserve physical screen density
                fontScale = MAX_FONT_SCALE          // cap the text scale
            )
        }
    }

    CompositionLocalProvider(LocalDensity provides safeDensity) {
        MaterialTheme(
            colorScheme = AppDarkColorScheme,
            content     = content
        )
    }
}

