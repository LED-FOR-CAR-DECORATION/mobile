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
fun MirageControlScreen(vm: ControlViewModel, back: () -> Unit, scanner: () -> Unit, content: () -> Unit, marquee: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val connected = state.connectionState is ConnectionState.Connected
    val led = state.ledState
    var picker by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var color by remember { mutableIntStateOf(0xff89c89b.toInt()) }
    Scaffold(topBar = { MirageBar("Điều khiển LED", back) }, containerColor = MirageBackground) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            MirageCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text((state.connectionState as? ConnectionState.Connected)?.deviceName ?: "Thiết bị LED"); Text(if (connected) "● Đã kết nối Bluetooth" else "Đã mất kết nối", color = MirageMuted) }
                    Switch(led.isPowerOn, { vm.togglePower() }, enabled = connected)
                }
                PixelPreview(modifier = Modifier.fillMaxWidth().aspectRatio(2f))
                Text("Xem trước minh họa", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
            }
            if (!connected) MirageCard { Text("Kết nối LED để điều khiển. Giá trị bên dưới là trạng thái lần cuối."); MirageButton("Kết nối lại", scanner) }
            MirageCard {
                Text("Độ sáng đèn · ${(led.brightness * 100f / 255).roundToInt()}%", style = MaterialTheme.typography.titleMedium)
                Slider(led.brightness * 100f / 255, { vm.setBrightness((it * 255 / 100).roundToInt()) }, valueRange = 0f..100f, enabled = connected, modifier = Modifier.testTag("ledBrightness"))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 25, 50, 75, 100).forEach { percent ->
                        FilterChip(selected = (led.brightness * 100f / 255).roundToInt() == percent,
                            onClick = { vm.setBrightness((percent * 255f / 100).roundToInt()) }, enabled = connected, label = { Text("$percent%") })
                    }
                }
            }
            Text("Chế độ hiển thị", style = MaterialTheme.typography.titleLarge)
            LedMode.entries.filterNot { it == LedMode.OFF }.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { mode ->
                        FilterChip(selected = led.currentMode == mode, onClick = {
                            when (mode) { LedMode.CUSTOM_PRESET -> content(); LedMode.MARQUEE_TEXT -> marquee(); else -> vm.setMode(mode) }
                        }, enabled = connected, label = { Text(when(mode) {
                            LedMode.STATIC_COLOR -> "Màu tĩnh"; LedMode.BREATHING -> "Nhịp thở"; LedMode.RAINBOW_WAVE -> "Cầu vồng"; LedMode.CHASE_FLOW -> "Đuổi màu"; LedMode.MUSIC_REACTIVE -> "Theo nhạc"; LedMode.CUSTOM_PRESET -> "Character"; else -> "Chạy chữ"
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
            if (led.currentMode !in listOf(LedMode.STATIC_COLOR, LedMode.OFF)) MirageCard {
                Text("Tốc độ · ${led.speed}")
                Slider(led.speed.toFloat(), { vm.setSpeed(it.toInt()) }, valueRange = 1f..100f, enabled = connected)
            }
            if (led.currentMode == LedMode.MUSIC_REACTIVE) MirageCard {
                Text("Độ nhạy âm thanh · ${led.sensitivity}")
                Slider(led.sensitivity.toFloat(), { vm.setSensitivity(it.toInt()) }, valueRange = 1f..100f, enabled = connected)
            }
            state.feedbackMessage?.let { Text(it, color = MirageMuted) }
        }
    }
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text("Chọn màu LED") }, text = {
        Column { Row { listOf("Bánh xe", "RGB", "HSV").forEachIndexed { i, title -> TextButton(onClick = { tab = i }) { Text(title) } } }; ColorEditor(color, { color = it }, tab) }
    }, confirmButton = { TextButton(onClick = { vm.setColor(RgbColor((color ushr 16) and 255, (color ushr 8) and 255, color and 255), false); picker = false }) { Text("Áp dụng") } }, dismissButton = { TextButton(onClick = { picker = false }) { Text("Hủy") } })
}
