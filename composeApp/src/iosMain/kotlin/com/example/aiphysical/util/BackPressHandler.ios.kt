package com.example.aiphysical.util

import androidx.compose.runtime.Composable

@Composable
actual fun BackPressHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS: back navigation handled via swipe gesture — no hardware back button
}

