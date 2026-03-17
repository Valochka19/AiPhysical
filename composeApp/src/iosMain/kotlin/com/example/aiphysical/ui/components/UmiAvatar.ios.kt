package com.example.aiphysical.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

@Composable
actual fun UmiAvatar(
    modifier: Modifier,
    contentDescription: String?,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("🤖", fontSize = 18.sp)
    }
}

