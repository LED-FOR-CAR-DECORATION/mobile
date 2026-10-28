package com.carled.app.presentation.components


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green


@Composable
fun PersonGlyph(modifier: Modifier = Modifier, color: Color = Green, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color, radius = w * 0.2f, center = Offset(w / 2f, h * 0.28f))
        drawArc(
            color = color, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(w * 0.12f, h * 0.55f), size = Size(w * 0.76f, h * 0.8f)
        )
    }
}

@Composable
fun KeyGlyph(modifier: Modifier = Modifier, color: Color = Green, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = 2.dp.toPx()
        drawCircle(color, radius = w * 0.2f, center = Offset(w * 0.25f, h * 0.5f), style = Stroke(s))
        drawLine(color, Offset(w * 0.45f, h * 0.5f), Offset(w * 0.95f, h * 0.5f), strokeWidth = s)
        drawLine(color, Offset(w * 0.95f, h * 0.5f), Offset(w * 0.95f, h * 0.75f), strokeWidth = s)
        drawLine(color, Offset(w * 0.75f, h * 0.5f), Offset(w * 0.75f, h * 0.7f), strokeWidth = s)
    }
}

@Composable
fun CloseGlyph(modifier: Modifier = Modifier, color: Color = Cream, size: Dp = 12.dp) {
    Canvas(modifier.size(size)) {
        val s = 1.5.dp.toPx()
        drawLine(color, Offset(0f, 0f), Offset(this.size.width, this.size.height), strokeWidth = s)
        drawLine(color, Offset(this.size.width, 0f), Offset(0f, this.size.height), strokeWidth = s)
    }
}

@Composable
fun BoxGlyph(modifier: Modifier = Modifier, color: Color = Cream, size: Dp = 14.dp) {
    Canvas(modifier.size(size)) {
        drawRect(color, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
fun CheckGlyph(modifier: Modifier = Modifier, color: Color = Color(0xFF1B241E), size: Dp = 10.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = 1.5.dp.toPx()
        drawLine(color, Offset(w * 0.1f, h * 0.55f), Offset(w * 0.4f, h * 0.85f), strokeWidth = s)
        drawLine(color, Offset(w * 0.4f, h * 0.85f), Offset(w * 0.9f, h * 0.15f), strokeWidth = s)
    }
}