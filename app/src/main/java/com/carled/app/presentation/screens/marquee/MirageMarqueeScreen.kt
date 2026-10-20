package com.carled.app.presentation.screens.marquee

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.canvas.ColorSwatch

@Composable
fun MirageMarqueeScreen(vm: MarqueeViewModel, back: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val connection by vm.connection.collectAsState()
    val layout = remember(state.text, state.size) { MarqueeLayout.fit(state.text, state.size) }
    val static = state.scrollMode == "static"
    val valid = state.text.isNotBlank() && state.text.all { it.code in 32..126 }
    val preview = remember(state.text, state.size, state.scrollMode, state.r, state.g, state.b) {
        val color = android.graphics.Color.rgb(state.r, state.g, state.b)
        if (static) layout.render(color) else MarqueeLayout.movingPreview(state.text, state.size, color)
    }
    androidx.activity.compose.BackHandler(state.isSending) { }
    Scaffold(topBar = { MirageBar("Chạy chữ", { if (!state.isSending) back() }) }, containerColor = MirageBackground) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Một thông điệp, ngàn sắc màu", style = MaterialTheme.typography.headlineSmall)
            PixelPreview(preview, Modifier.fillMaxWidth().aspectRatio(2f))
            Text(if (static) {
                if (!layout.fits) "Không đủ chỗ ở cỡ 5×7. Rút ngắn còn tối đa 40 ký tự hoặc chọn Cuộn trái."
                else "Tự vừa màn hình · ${layout.glyphLabel} · ${layout.lines.size} dòng · căn giữa"
            } else "Xem trước phần đầu · ${if (state.size == 2) "10×14" else "5×7"} · ${if (state.scrollMode == "blink") "Chữ dài có thể bị cắt khi nhấp nháy." else "Chữ dài sẽ cuộn trên LED."}", color = if (static && !layout.fits) MaterialTheme.colorScheme.error else MirageMuted)
            OutlinedTextField(state.text, vm::updateText, modifier = Modifier.fillMaxWidth(), enabled = !state.isSending, singleLine = true, label = { Text("Nội dung") }, supportingText = { Text("Dùng ký tự tiếng Anh không dấu.") })
            Text(if (static) "Cỡ chữ ưu tiên" else "Cỡ chữ", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(1 to "5×7", 2 to "10×14").forEach { (size, label) ->
                    FilterChip(state.size == size, { vm.updateSize(size) }, enabled = !state.isSending, label = { Text(label) })
                }
            }
            if (static) Text("Tự xuống dòng và giảm 10×14 xuống 5×7 khi cần. Giữ nét pixel, không kéo méo chữ.", color = MirageMuted)
            if (state.scrollMode == "scroll_left") {
                Text("Tốc độ · ${state.speed}")
                Slider(state.speed.toFloat(), { vm.updateSpeed(it.toInt()) }, valueRange = 1f..100f, enabled = !state.isSending)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("scroll_left" to "Cuộn trái", "static" to "Đứng yên", "blink" to "Nhấp nháy").forEach { (mode, label) -> FilterChip(state.scrollMode == mode, { vm.updateScrollMode(mode) }, enabled = !state.isSending, label = { Text(label) }) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                listOf(0xff89c89b.toInt(), 0xffffffff.toInt(), 0xffffd966.toInt(), 0xff66bbff.toInt()).forEach { c -> ColorSwatch(c) { if (!state.isSending) vm.updateColor((c ushr 16) and 255, (c ushr 8) and 255, c and 255) } }
            }
            if (connection !is ConnectionState.Connected) Text("Kết nối LED trước khi gửi.", color = MirageMuted)
            MirageButton(if (state.isSending) "Đang gửi…" else "Hiển thị trên LED", vm::sendMarquee, Modifier.fillMaxWidth(), enabled = connection is ConnectionState.Connected && !state.isSending && valid && (!static || layout.fits))
            if (state.isSending) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.feedbackMessage?.let { Text(it, color = MirageMuted) }
        }
    }
}
