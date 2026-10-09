package com.carled.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.RgbColor
import com.carled.app.presentation.theme.*

@Composable
fun StatusBadge(state: ConnectionState, modifier: Modifier = Modifier) {
    val (label, bgColor, textColor) = when (state) {
        is ConnectionState.Connected -> Triple("ĐÃ KẾT NỐI", NeonGreen.copy(alpha = 0.2f), NeonGreen)
        is ConnectionState.Connecting -> Triple("ĐANG KẾT NỐI...", PrimaryCyan.copy(alpha = 0.2f), PrimaryCyan)
        is ConnectionState.Scanning -> Triple("ĐANG QUÉT...", AccentPurple.copy(alpha = 0.2f), AccentPurple)
        is ConnectionState.Disconnected -> Triple("CHƯA KẾT NỐI", TextSecondary.copy(alpha = 0.2f), TextSecondary)
        is ConnectionState.Error -> Triple("LỖI KẾT NỐI", ErrorRed.copy(alpha = 0.2f), ErrorRed)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ColorPresetRow(
    selectedColor: RgbColor,
    onColorSelected: (RgbColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        RgbColor(0, 200, 255),   // Cyan
        RgbColor(168, 85, 247),  // Purple
        RgbColor(236, 72, 153),  // Pink
        RgbColor(16, 185, 129),  // Emerald Green
        RgbColor(239, 68, 68),   // Red
        RgbColor(245, 158, 11),  // Amber
        RgbColor(255, 255, 255)  // Cool White
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { color ->
            val composeColor = Color(color.red, color.green, color.blue)
            val isSelected = color == selectedColor

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(composeColor)
                    .clickable { onColorSelected(color) }
                    .then(
                        if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                        else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    )
            )
        }
    }
}
