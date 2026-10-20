package com.carled.app.presentation.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun LedColorWheel(color: Int, onColor: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val initial = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) } }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var saturation by remember { mutableFloatStateOf(initial[1]) }
    var value by remember { mutableFloatStateOf(initial[2]) }
    val callback by rememberUpdatedState(onColor)
    LaunchedEffect(color) {
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) }
        if (hsv[2] > 0) { if (hsv[1] > 0) hue = hsv[0]; saturation = hsv[1] }
        value = hsv[2]
    }
    fun emit() { callback(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.widthIn(max = 260.dp).fillMaxWidth().aspectRatio(1f).align(Alignment.CenterHorizontally).testTag("ledColorWheel")
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(); down.consume()
                    fun pick(point: Offset) {
                        val dx = point.x - size.width / 2f; val dy = point.y - size.height / 2f
                        val radius = min(size.width, size.height) / 2f - 10.dp.toPx()
                        hue = ((atan2(dy, dx) * 180 / PI).toFloat() + 360) % 360
                        saturation = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                        emit()
                    }
                    pick(down.position)
                    do {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull()?.let { pick(it.position) }
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }) {
            val radius = min(size.width, size.height) / 2 - 10.dp.toPx()
            drawCircle(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)), radius)
            drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), radius = radius), radius)
            drawCircle(Color.Black.copy(alpha = 1 - value), radius)
            val angle = hue * PI / 180
            val selected = center + Offset((cos(angle) * radius * saturation).toFloat(), (sin(angle) * radius * saturation).toFloat())
            drawCircle(Color.Black, 9.dp.toPx(), selected, style = Stroke(3.dp.toPx()))
            drawCircle(Color.White, 9.dp.toPx(), selected, style = Stroke(1.5.dp.toPx()))
            if (!enabled) drawCircle(Color.White.copy(alpha = .5f), radius)
        }
        Text("Sáng / tối màu · ${(value * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
        Slider(value, { value = it; emit() }, valueRange = 0f..1f, enabled = enabled)
    }
}
