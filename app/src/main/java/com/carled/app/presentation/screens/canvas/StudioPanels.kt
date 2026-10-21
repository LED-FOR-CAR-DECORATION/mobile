package com.carled.app.presentation.screens.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.carled.app.data.local.PixelPalette
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*
import java.util.UUID
import kotlin.math.*

@Composable
fun ColorSwatch(color: Int, selected: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).padding(4.dp).background(Color(color), CircleShape)
        .border(if (selected) 3.dp else 1.dp, MirageInk, CircleShape).clickable(onClick = onClick))
}

@Composable
fun PaletteDialog(state: StudioState, vm: StudioViewModel, close: () -> Unit) {
    var palettes by remember { mutableStateOf(state.palettes) }
    var paletteId by remember { mutableStateOf(palettes.firstOrNull()?.id) }
    var swatch by remember { mutableStateOf<Int?>(null) }
    var value by remember { mutableIntStateOf(state.color) }
    var tab by remember { mutableIntStateOf(0) }
    var rename by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var delete by remember { mutableStateOf(false) }
    val palette = palettes.firstOrNull { it.id == paletteId }
    fun updatePalette(colors: List<Int>) { palettes = palettes.map { if (it.id == paletteId) it.copy(colors = colors) else it } }
    AlertDialog(onDismissRequest = close, title = { Text("Màu & bảng màu") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bảng màu của tôi", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palettes.forEach { p -> FilterChip(selected = p.id == paletteId, onClick = { paletteId = p.id; swatch = null }, label = { Text(p.name) }) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { name = ""; rename = false; paletteId = null }) { Text("+ Bảng màu") }
                if (palette != null) {
                    TextButton(onClick = { name = palette.name; rename = true }) { Text("Đổi tên") }
                    TextButton(onClick = { delete = true }) { Text("Xóa bảng") }
                }
            }
            if (paletteId == null || rename) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("Tên bảng màu") }, singleLine = true)
                TextButton(enabled = name.isNotBlank(), onClick = {
                    if (rename) palettes = palettes.map { if (it.id == paletteId) it.copy(name = name.trim()) else it }
                    else { val p = PixelPalette(UUID.randomUUID().toString(), name.trim(), emptyList()); palettes = palettes + p; paletteId = p.id }
                    rename = false
                }) { Text("Lưu tên") }
            }
            palette?.let { p ->
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    p.colors.forEachIndexed { index, c -> ColorSwatch(c, swatch == index) { swatch = index; value = c } }
                }
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    TextButton(onClick = { updatePalette(p.colors + value); swatch = p.colors.size }) { Text("Thêm màu") }
                    TextButton(enabled = swatch != null, onClick = { updatePalette(p.colors.mapIndexed { i, c -> if (i == swatch) value else c }) }) { Text("Sửa màu") }
                    TextButton(enabled = swatch != null, onClick = { updatePalette(p.colors.filterIndexed { i, _ -> i != swatch }); swatch = null }) { Text("Xóa màu") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Bánh xe", "RGB", "HSV").forEachIndexed { i, title -> FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(title) }) }
            }
            ColorEditor(value, { value = it }, tab)
            Row(verticalAlignment = Alignment.CenterVertically) { ColorSwatch(value) {}; Text("Màu đang chọn") }
            Text("Đổi bảng màu không thay đổi các pixel đã vẽ.", color = MirageMuted)
        }
    }, confirmButton = { TextButton(onClick = { vm.color(value); vm.palettes(palettes); close() }) { Text("Áp dụng") } },
        dismissButton = { TextButton(onClick = close) { Text("Hủy") } })
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Xóa bảng màu?") }, text = { Text("Bản vẽ và màu trên canvas được giữ nguyên.") },
        confirmButton = { TextButton(onClick = { palettes = palettes.filterNot { it.id == paletteId }; paletteId = palettes.firstOrNull()?.id; swatch = null; delete = false }) { Text("Xóa") } },
        dismissButton = { TextButton(onClick = { delete = false }) { Text("Hủy") } })
}

@Composable
fun ColorEditor(value: Int, onChange: (Int) -> Unit, tab: Int = 0) {
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(value, it) }
    if (tab == 0) {
        LedColorWheel(value, onChange)
    } else {
        val values = if (tab == 1) listOf((value ushr 16) and 255, (value ushr 8) and 255, value and 255) else listOf(hsv[0].roundToInt(), (hsv[1] * 100).roundToInt(), (hsv[2] * 100).roundToInt())
        val labels = if (tab == 1) listOf("R", "G", "B") else listOf("H (°)", "S (%)", "V (%)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            values.forEachIndexed { i, number ->
                var text by remember(value, tab) { mutableStateOf(number.toString()) }
                OutlinedTextField(text, { raw ->
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
                }, label = { Text(labels[i]) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
            }
        }
    }
}

@Composable
fun ImportImageDialog(image: Bitmap, doc: PixelDocument, apply: (List<Int>) -> Unit, close: () -> Unit, choose: () -> Unit) {
    var crop by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var alignX by remember { mutableFloatStateOf(.5f) }; var alignY by remember { mutableFloatStateOf(.5f) }
    val pixels = remember(image, doc.width, doc.height, crop, alignX, alignY) {
        val scale = if (crop) max(doc.width.toFloat() / image.width, doc.height.toFloat() / image.height) else min(doc.width.toFloat() / image.width, doc.height.toFloat() / image.height)
        val dw = image.width * scale; val dh = image.height * scale
        val ox = (doc.width - dw) * alignX; val oy = (doc.height - dh) * alignY
        List(doc.width * doc.height) { i ->
            val sx = floor((i % doc.width + .5f - ox) / scale).toInt(); val sy = floor((i / doc.width + .5f - oy) / scale).toInt()
            if (sx in 0 until image.width && sy in 0 until image.height) image.getPixel(sx, sy).let { if (it ushr 24 == 0) 0 else it or 0xff000000.toInt() } else 0
        }
    }
    AlertDialog(onDismissRequest = close, title = { Text("Nhập ảnh") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(image.asImageBitmap(), "Ảnh gốc", Modifier.fillMaxWidth().height(100.dp))
            Text("Xem trước ${doc.width}×${doc.height}")
            PixelPreview(doc.copy(pixels = pixels), Modifier.fillMaxWidth().aspectRatio(doc.width.toFloat() / doc.height))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!crop, { crop = false }, label = { Text("Thu vừa") }); FilterChip(crop, { crop = true }, label = { Text("Cắt đầy") })
            }
            Text("Giữ tỷ lệ ảnh; canvas không đổi kích thước. Lớp chữ được giữ lại.")
            Text("Căn ngang"); Slider(alignX, { alignX = it })
            Text("Căn dọc"); Slider(alignY, { alignY = it })
            TextButton(onClick = choose) { Text("Chọn ảnh khác") }
        }
    }, confirmButton = { TextButton(onClick = { confirm = true }) { Text("Nhập vào canvas") } }, dismissButton = { TextButton(onClick = close) { Text("Hủy") } })
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Thay lớp pixel?") }, text = { Text("Ảnh thay thế lớp pixel hiện tại. Lớp chữ giữ nguyên; bạn có thể hoàn tác.") },
        confirmButton = { TextButton(onClick = { confirm = false; apply(pixels) }) { Text("Thay thế") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Hủy") } })
}
