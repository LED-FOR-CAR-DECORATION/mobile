package com.carled.app.presentation.screens.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carled.app.presentation.components.LedColorWheel
import com.carled.app.presentation.components.MirageInk
import kotlin.math.roundToInt

@Composable
fun ColorEditor(
    value: Int,
    onChange: (Int) -> Unit,
    tab: Int = 0
) {
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(value, it) }
    val red = (value ushr 16) and 255
    val green = (value ushr 8) and 255
    val blue = value and 255

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(value), RoundedCornerShape(8.dp))
                .border(1.5.dp, MirageInk, RoundedCornerShape(8.dp))
        )

        if (tab == 0) {
            LedColorWheel(value, onChange)
        } else {
            val values = if (tab == 1) listOf(red, green, blue) else listOf(hsv[0].roundToInt(), (hsv[1] * 100).roundToInt(), (hsv[2] * 100).roundToInt())
            val labels = if (tab == 1) listOf("R", "G", "B") else listOf("H (°)", "S (%)", "V (%)")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                values.forEachIndexed { i, number ->
                    var text by remember(value, tab) { mutableStateOf(number.toString()) }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { raw ->
                            if (raw.all(Char::isDigit) && raw.length <= 3) {
                                text = raw
                                raw.toIntOrNull()?.let { parsed ->
                                    val max = if (tab == 1) 255 else if (i == 0) 360 else 100
                                    val input = parsed.coerceIn(0, max)
                                    if (tab == 1) {
                                        val v = values.toMutableList(); v[i] = input
                                        onChange(android.graphics.Color.rgb(v[0], v[1], v[2]))
                                    } else {
                                        val v = hsv.clone(); v[i] = if (i == 0) input.toFloat() % 360 else input / 100f
                                        onChange(android.graphics.Color.HSVToColor(v))
                                    }
                                }
                            }
                        },
                        label = { Text(labels[i]) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
    }
}