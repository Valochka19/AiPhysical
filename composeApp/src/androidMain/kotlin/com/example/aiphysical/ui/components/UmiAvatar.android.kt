package com.example.aiphysical.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.aiphysical.R

@Composable
actual fun UmiAvatar(
    modifier: Modifier,
    contentDescription: String?,
) {
    Image(
        painter = painterResource(id = R.drawable.umi_avatar),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

