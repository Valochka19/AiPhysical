package com.example.aiphysical.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.aiphysical.R

@Composable
actual fun UmiAvatar(
    modifier: Modifier,
    contentDescription: String?,
) {
    val context = LocalContext.current
    val avatarResId = remember(context) {
        context.resources.getIdentifier("umi_avatar", "drawable", context.packageName)
            .takeIf { it != 0 }
            ?: R.mipmap.ic_launcher_round
    }

    Image(
        painter = painterResource(id = avatarResId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

