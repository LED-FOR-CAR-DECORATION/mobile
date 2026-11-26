package com.carled.app.presentation.screens.marquee

import android.graphics.Color
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.*
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.*

@Composable
fun MirageMarqueeScreen(vm: MarqueeViewModel, back: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val connection by vm.connection.collectAsState()
    val layout = remember(state.text, state.size) { MarqueeLayout.fit(state.text, state.size) }
    val static = state.scrollMode != "scroll_left"
    val valid = state.text.isNotBlank() && state.text.all { it.code in 32..126 }
    val connected = connection is ConnectionState.Connected
    val canSend = connected && !state.isSending && valid && (!static || layout.fits)
    val preview = remember(state.text, state.size, state.scrollMode, state.r, state.g, state.b) {
        val color = Color.rgb(state.r, state.g, state.b)
        if (static) layout.render(color) else MarqueeLayout.movingPreview(state.text, state.size, color)
    }
    val enabled = !state.isSending

    BackHandler(state.isSending) { }

    Box(
        Modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
            .imePadding()
    ) {
        WindowFrame(
            title = "Chạy chữ",
            onClose = { if (enabled) back() },
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
                        text = "Trình chiếu chữ",
                        color = Ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
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

                DeviceSection(title = "Xem trước") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PixelPreview(preview, Modifier.fillMaxWidth().aspectRatio(2f))
                        Text(
                            text = if (static) {
                                if (!layout.fits) "Không đủ chỗ ở cỡ 5×7. Rút ngắn còn tối đa 40 ký tự hoặc chọn Cuộn trái."
                                else "Tự vừa màn hình · ${layout.glyphLabel} · ${layout.lines.size} dòng · căn giữa${if (state.scrollMode == "blink") " · Nhấp nháy" else ""}"
                            } else "Xem trước phần đầu · ${if (state.size == 2) "10×14" else "5×7"} · Chữ dài sẽ cuộn trên LED.",
                            color = if (static && !layout.fits) ErrorRed else MirageMuted,
                            fontSize = 13.sp
                        )
                        if (!connected) {
                            Text("Kết nối LED trước khi gửi.", color = MirageMuted, fontSize = 12.sp)
                        }
                    }
                }

                if (vm.hasProfiles) {
                    TextProfilesPanel(vm, valid && (!static || layout.fits))
                }

                DeviceSection(title = "Nội dung") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HardTextField(
                            value = state.text,
                            onValueChange = vm::updateText,
                            label = "Nội dung",
                            enabled = enabled,
                            imeAction = ImeAction.Done,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Dùng ký tự tiếng Anh không dấu.", color = MirageMuted, fontSize = 12.sp)
                    }
                }

                DeviceSection(title = if (static) "Cỡ chữ ưu tiên" else "Cỡ chữ") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sizeOptions = listOf("5×7", "10×14")
                        val selectedSizeIdx = if (state.size == 1) 0 else 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (enabled) 1f else 0.5f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sizeOptions.forEachIndexed { idx, label ->
                                val selected = selectedSizeIdx == idx
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .then(
                                            if (selected) {
                                                Modifier.hardSurface(color = GreenLight, offset = 3.dp, borderWidth = 2.dp)
                                            } else {
                                                Modifier.flatSurface(color = Cream)
                                            }
                                        )
                                        .clickable(enabled = enabled) { vm.updateSize(if (idx == 0) 1 else 2) }
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                        if (static) {
                            Text(
                                "Tự xuống dòng và giảm 10×14 xuống 5×7 khi cần. Giữ nét pixel, không kéo méo chữ.",
                                color = MirageMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                DeviceSection(title = "Hiệu ứng hiển thị") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val modes = listOf("scroll_left", "static", "blink")
                        val labels = listOf("Cuộn trái", "Đứng yên", "Nhấp nháy")
                        val selectedModeIdx = modes.indexOf(state.scrollMode).coerceAtLeast(0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (enabled) 1f else 0.5f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            labels.forEachIndexed { idx, label ->
                                val selected = selectedModeIdx == idx
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .then(
                                            if (selected) {
                                                Modifier.hardSurface(color = GreenLight, offset = 3.dp, borderWidth = 2.dp)
                                            } else {
                                                Modifier.flatSurface(color = Cream)
                                            }
                                        )
                                        .clickable(enabled = enabled) { vm.updateScrollMode(modes[idx]) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        if (state.scrollMode == "scroll_left") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tốc độ", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    Modifier
                                        .hardSurface(color = GreenLight, offset = 3.dp, borderWidth = 2.dp)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("${state.speed}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Slider(
                                value = state.speed.toFloat(),
                                onValueChange = { vm.updateSpeed(it.toInt()) },
                                valueRange = 1f..100f,
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                DeviceSection(title = "Màu sắc") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream)
                            .padding(12.dp)
                    ) {
                        ColorPresetRow(
                            selectedColor = RgbColor(state.r, state.g, state.b),
                            onColorSelected = { c -> if (enabled) vm.updateColor(c.red, c.green, c.blue) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HardButton(
                    text = if (state.isSending) "Đang gửi…" else "Hiển thị trên LED",
                    onClick = vm::sendMarquee,
                    enabled = canSend,
                    shadowOffset = HardStyle.CardOffset,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )

                if (state.isSending) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                state.feedbackMessage?.let { msg ->
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
}