package com.carled.app.presentation.screens.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.canvas.ColorEditor
import com.carled.app.presentation.screens.canvas.ColorSwatch
import com.carled.app.presentation.theme.*

@Composable
fun MirageEffectScreen(
    vm: ControlViewModel,
    back: () -> Unit,
    scanner: () -> Unit
) {
    val connectionState by vm.connectionState.collectAsState()
    val ledState by vm.ledState.collectAsState()
    val feedbackMessage by vm.feedbackMessage.collectAsState()

    val connected = connectionState is ConnectionState.Connected
    val effectModes = listOf(
        LedMode.STATIC_COLOR,
        LedMode.BREATHING,
        LedMode.RAINBOW_WAVE,
        LedMode.CHASE_FLOW,
        LedMode.MUSIC_REACTIVE
    )

    var picker by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var color by remember { mutableIntStateOf(0xff89c89b.toInt()) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
    ) {
        WindowFrame(
            title = "Màu đơn sắc & hiệu ứng",
            onClose = back,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (connected) "Kết nối LED hoạt động" else "Kết nối LED để áp dụng màu và hiệu ứng",
                        color = if (connected) Ink else MirageMuted,
                        fontWeight = if (connected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .flatSurface(color = if (connected) GreenLight else Cream)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (connected) "Đã kết nối" else "Chưa kết nối",
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (!connected) {
                    HardButton(
                        text = "Kết nối LED",
                        onClick = scanner,
                        shadowOffset = HardStyle.CardOffset,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }

                DeviceSection(title = "Chế độ hiển thị") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        effectModes.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { mode ->
                                    val selected = ledState.currentMode == mode
                                    val label = when (mode) {
                                        LedMode.STATIC_COLOR -> "Màu đơn sắc"
                                        LedMode.BREATHING -> "Nhịp thở"
                                        LedMode.RAINBOW_WAVE -> "Cầu vồng"
                                        LedMode.CHASE_FLOW -> "Đuổi màu"
                                        LedMode.MUSIC_REACTIVE -> "Theo nhạc"
                                        LedMode.CUSTOM_PRESET -> "Character"
                                        else -> "Chạy chữ"
                                    }
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .then(
                                                if (selected) {
                                                    Modifier.hardSurface(
                                                        color = GreenLight,
                                                        offset = 3.dp,
                                                        borderWidth = 2.dp
                                                    )
                                                } else {
                                                    Modifier.flatSurface(color = Cream)
                                                }
                                            )
                                            .clickable(enabled = connected) { vm.setMode(mode) }
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (connected) Ink else MirageMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (ledState.currentMode in listOf(LedMode.STATIC_COLOR, LedMode.BREATHING, LedMode.CHASE_FLOW)) {
                    DeviceSection(title = "Màu LED") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .flatSurface(color = Cream)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val current = android.graphics.Color.rgb(
                                ledState.currentColor.red,
                                ledState.currentColor.green,
                                ledState.currentColor.blue
                            )
                            LedColorWheel(
                                color = current,
                                onColor = { c ->
                                    vm.setColor(RgbColor((c ushr 16) and 255, (c ushr 8) and 255, c and 255))
                                },
                                enabled = connected
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ColorSwatch(current) {}
                                Box(
                                    Modifier
                                        .hardSurface(color = GreenLight, offset = 3.dp, borderWidth = 2.dp)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "#%06X".format(current and 0xffffff),
                                        color = Ink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    0xff89c89b.toInt(),
                                    0xff66bbff.toInt(),
                                    0xffffd966.toInt(),
                                    0xffff6688.toInt()
                                ).forEach { c ->
                                    ColorSwatch(c) {
                                        if (connected) {
                                            vm.setColor(RgbColor((c ushr 16) and 255, (c ushr 8) and 255, c and 255))
                                        }
                                    }
                                }
                            }
                            HardButton(
                                text = "Nhập RGB / HSV",
                                enabled = connected,
                                filled = false,
                                shadowOffset = HardStyle.CardOffset,
                                onClick = {
                                    color = current
                                    tab = 1
                                    picker = true
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )
                        }
                    }
                }

                if (ledState.currentMode in effectModes && ledState.currentMode != LedMode.STATIC_COLOR) {
                    DeviceSection(title = "Tốc độ hiệu ứng") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .flatSurface(color = Cream)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tốc độ", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Box(
                                    Modifier
                                        .hardSurface(color = GreenLight, offset = 3.dp, borderWidth = 2.dp)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("${ledState.speed}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Slider(
                                value = ledState.speed.toFloat(),
                                onValueChange = { vm.setSpeed(it.toInt()) },
                                valueRange = 1f..100f,
                                enabled = connected,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (ledState.currentMode == LedMode.MUSIC_REACTIVE) {
                    DeviceSection(title = "Độ nhạy âm thanh") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .flatSurface(color = Cream)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Độ nhạy", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Box(
                                    Modifier
                                        .hardSurface(color = GreenLight, offset = 3.dp, borderWidth = 2.dp)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("${ledState.sensitivity}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Slider(
                                value = ledState.sensitivity.toFloat(),
                                onValueChange = { vm.setSensitivity(it.toInt()) },
                                valueRange = 1f..100f,
                                enabled = connected,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (ledState.currentMode !in effectModes) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Chọn màu đơn sắc hoặc một hiệu ứng ở trên để áp dụng lên LED.",
                            color = MirageMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                feedbackMessage?.let { msg ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream)
                            .padding(10.dp)
                    ) {
                        Text(text = msg, color = MirageMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (picker) {
        AlertDialog(
            onDismissRequest = { picker = false },
            title = { Text("Chọn màu LED") },
            text = {
                Column {
                    Row {
                        listOf("Bánh xe", "RGB", "HSV").forEachIndexed { i, title ->
                            TextButton(onClick = { tab = i }) { Text(title) }
                        }
                    }
                    ColorEditor(color, { color = it }, tab)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setColor(RgbColor((color ushr 16) and 255, (color ushr 8) and 255, color and 255), false)
                    picker = false
                }) { Text("Áp dụng") }
            },
            dismissButton = {
                TextButton(onClick = { picker = false }) { Text("Hủy") }
            }
        )
    }
}