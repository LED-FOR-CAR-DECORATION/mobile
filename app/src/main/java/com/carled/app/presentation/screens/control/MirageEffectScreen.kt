package com.carled.app.presentation.screens.control

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import kotlin.math.roundToInt
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.canvas.ColorEditor
import com.carled.app.presentation.screens.canvas.ColorSwatch

@Composable
fun MirageEffectScreen(vm: ControlViewModel, back: () -> Unit, scanner: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val connected = state.connectionState is ConnectionState.Connected
    val led = state.ledState
    val effectModes = listOf(LedMode.STATIC_COLOR, LedMode.BREATHING, LedMode.RAINBOW_WAVE, LedMode.CHASE_FLOW, LedMode.MUSIC_REACTIVE)
    var picker by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var color by remember { mutableIntStateOf(0xff89c89b.toInt()) }
    Scaffold(topBar = { MirageBar("Effect", back) }, containerColor = MirageBackground) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Màu đơn sắc & hiệu ứng", style = MaterialTheme.typography.headlineSmall)
            Text(if (connected) "● Đã kết nối Bluetooth" else "Kết nối LED để áp dụng màu và hiệu ứng.", color = MirageMuted)
            if (!connected) MirageButton("Kết nối LED", scanner, Modifier.fillMaxWidth())
            Text("Chế độ hiển thị", style = MaterialTheme.typography.titleLarge)
            effectModes.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { mode ->
                        FilterChip(selected = led.currentMode == mode, onClick = {
                            vm.setMode(mode)
                        }, enabled = connected, label = { Text(when(mode) {
                            LedMode.STATIC_COLOR -> "Màu đơn sắc"; LedMode.BREATHING -> "Nhịp thở"; LedMode.RAINBOW_WAVE -> "Cầu vồng"; LedMode.CHASE_FLOW -> "Đuổi màu"; LedMode.MUSIC_REACTIVE -> "Theo nhạc"; LedMode.CUSTOM_PRESET -> "Character"; else -> "Chạy chữ"
                        }) }, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (led.currentMode in listOf(LedMode.STATIC_COLOR, LedMode.BREATHING, LedMode.CHASE_FLOW)) MirageCard {
                Text("Màu LED", style = MaterialTheme.typography.titleMedium)
                val current = android.graphics.Color.rgb(led.currentColor.red, led.currentColor.green, led.currentColor.blue)
                LedColorWheel(current, { c -> vm.setColor(RgbColor((c ushr 16) and 255, (c ushr 8) and 255, c and 255)) }, enabled = connected)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorSwatch(current) {}
                    Text("#%06X".format(current and 0xffffff), style = MaterialTheme.typography.labelLarge)
                }
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(0xff89c89b.toInt(), 0xff66bbff.toInt(), 0xffffd966.toInt(), 0xffff6688.toInt()).forEach { c -> ColorSwatch(c) { if (connected) vm.setColor(RgbColor((c ushr 16) and 255, (c ushr 8) and 255, c and 255)) } }
                }
                MirageButton("Nhập RGB / HSV", { color = current; tab = 1; picker = true }, enabled = connected, secondary = true)
            }
            if (led.currentMode in effectModes && led.currentMode != LedMode.STATIC_COLOR) MirageCard {
                Text("Tốc độ · ${led.speed}")
                Slider(led.speed.toFloat(), { vm.setSpeed(it.toInt()) }, valueRange = 1f..100f, enabled = connected)
            }
            if (led.currentMode == LedMode.MUSIC_REACTIVE) MirageCard {
                Text("Độ nhạy âm thanh · ${led.sensitivity}")
                Slider(led.sensitivity.toFloat(), { vm.setSensitivity(it.toInt()) }, valueRange = 1f..100f, enabled = connected)
            }
            if (led.currentMode !in effectModes) Text("Chọn màu đơn sắc hoặc một hiệu ứng ở trên để áp dụng lên LED.", color = MirageMuted)
            state.feedbackMessage?.let { Text(it, color = MirageMuted) }
        }
    }
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text("Chọn màu LED") }, text = {
        Column { Row { listOf("Bánh xe", "RGB", "HSV").forEachIndexed { i, title -> TextButton(onClick = { tab = i }) { Text(title) } } }; ColorEditor(color, { color = it }, tab) }
    }, confirmButton = { TextButton(onClick = { vm.setColor(RgbColor((color ushr 16) and 255, (color ushr 8) and 255, color and 255), false); picker = false }) { Text("Áp dụng") } }, dismissButton = { TextButton(onClick = { picker = false }) { Text("Hủy") } })
}
