package com.carled.app.presentation.screens.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.carled.app.presentation.components.MirageInk
import com.carled.app.presentation.components.MirageSage

@Composable
fun ColorSwatch(
    colorInt: Int,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(colorInt))
            .border(
                width = if (selected) 3.dp else 1.5.dp,
                color = if (selected) MirageSage else MirageInk,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (Color(colorInt).luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}