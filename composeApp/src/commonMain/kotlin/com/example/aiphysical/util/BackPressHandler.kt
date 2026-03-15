package com.example.aiphysical.util

import androidx.compose.runtime.Composable

/** Cross-platform back press interception. On Android intercepts the hardware back button.
 *  On iOS, no-op (swipe gesture is handled natively by the OS). */
@Composable
expect fun BackPressHandler(enabled: Boolean = true, onBack: () -> Unit)

