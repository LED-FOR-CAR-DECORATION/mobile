package com.carled.app.presentation.screens.canvas

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioToolsSheet(state: StudioState, vm: StudioViewModel, close: () -> Unit, openText: () -> Unit, importImage: () -> Unit) {
    val doc = state.document ?: return
    var tool by remember { mutableStateOf(state.tool) }
    var shape by remember { mutableStateOf(state.shape) }
    var filled by remember { mutableStateOf(state.filled) }
    var mirror by remember { mutableStateOf(state.mirror) }
    var grid by remember { mutableStateOf(state.grid) }
    ModalBottomSheet(onDismissRequest = close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MirageBackground, modifier = Modifier.testTag("studioToolsSheet")) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, null)
                Text("Công cụ & đối xứng", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = close) { Icon(Icons.Default.Close, "Đóng công cụ") }
            }
            HorizontalDivider(color = MirageInk)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("BỘ CÔNG CỤ VẼ", style = MaterialTheme.typography.labelLarge)
                    Text("Ma trận ${doc.width}×${doc.height}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolTile("Hình dạng", Icons.Default.CropSquare, tool == PixelTool.SHAPE, Modifier.weight(1f)) { tool = PixelTool.SHAPE }
                    ToolTile("Đổ màu", Icons.Default.FormatColorFill, tool == PixelTool.FILL, Modifier.weight(1f)) { tool = PixelTool.FILL }
                    ToolTile("Đối xứng", Icons.Default.Flip, mirror != MirrorMode.NONE, Modifier.weight(1f)) { mirror = if (mirror == MirrorMode.NONE) MirrorMode.HORIZONTAL else MirrorMode.NONE }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolTile("Văn bản", Icons.Default.TextFields, false, Modifier.weight(1f), openText)
                    ToolTile("Nhập ảnh", Icons.Default.AddPhotoAlternate, false, Modifier.weight(1f), importImage)
                    ToolTile(if (grid) "Lưới: Bật" else "Lưới: Tắt", Icons.Default.GridOn, grid, Modifier.weight(1f)) { grid = !grid }
                }
                if (tool == PixelTool.SHAPE) MirageCard {
                    Text("Tùy chọn: Hình dạng", style = MaterialTheme.typography.titleMedium)
                    Text("Màu #${(state.color and 0xffffff).toString(16).padStart(6, '0').uppercase()}", color = MirageMuted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PixelShape.entries.forEach { option ->
                            ChoiceTile(when (option) { PixelShape.LINE -> "Đường thẳng"; PixelShape.RECTANGLE -> "Chữ nhật"; PixelShape.ELLIPSE -> "Hình elip" }, shape == option, Modifier.weight(1f)) { shape = option }
                        }
                    }
                    Text("Kiểu phủ màu", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceTile("Viền ngoài", !filled || shape == PixelShape.LINE, Modifier.weight(1f)) { filled = false }
                        ChoiceTile("Tô kín", filled && shape != PixelShape.LINE, Modifier.weight(1f), shape != PixelShape.LINE) { filled = true }
                    }
                    Text("Đường thẳng tự động bỏ qua chế độ tô kín.", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
                }
                MirageCard {
                    Text("Chế độ đối xứng", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MirrorMode.entries.forEach { mode -> ChoiceTile(when (mode) {
                            MirrorMode.NONE -> "Tắt"; MirrorMode.HORIZONTAL -> "Trái–phải"; MirrorMode.VERTICAL -> "Trên–dưới"; MirrorMode.BOTH -> "Hai trục"
                        }, mirror == mode, Modifier.weight(1f)) { mirror = mode } }
                    }
                    val sample = remember(doc.width, doc.height, mirror, state.color) {
                        PixelDocument(doc.width, doc.height).brush(doc.width / 4, doc.height / 3, doc.width / 4 + 3, doc.height / 3 + 2, state.color, 2, mirror)
                    }
                    Text("MÔ PHỎNG TRỤC ${doc.width}×${doc.height}", style = MaterialTheme.typography.labelMedium)
                    MatrixObjectPreview(sample, null, mirror = mirror)
                    Text("Đối xứng áp dụng cho nét vẽ và hình mới; không thay đổi pixel đã vẽ.", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
                }
            }
            HorizontalDivider(color = MirageInk)
            MirageButton("Áp dụng công cụ", {
                vm.applyTools(tool, shape, filled, mirror, grid); close()
            }, Modifier.fillMaxWidth().padding(16.dp).testTag("applyStudioTools"), enabled = !state.sending)
        }
    }
}

@Composable
private fun ToolTile(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 92.dp), color = if (selected) MirageSage else Color.White,
        shape = RoundedCornerShape(12.dp), border = BorderStroke(2.dp, MirageInk)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            Icon(icon, null, Modifier.size(28.dp)); Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ChoiceTile(label: String, selected: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(8.dp), border = BorderStroke(if (selected) 2.dp else 1.dp, MirageInk),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) MirageSage else Color.White, contentColor = MirageInk),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
        Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun PixelTextDialog(doc: PixelDocument, color: Int, vm: StudioViewModel, close: () -> Unit) {
    var selected by remember { mutableStateOf(doc.texts.firstOrNull()?.id) }
    val initial = doc.texts.firstOrNull()
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var font by remember { mutableStateOf(initial?.let { it.glyphWidth to it.glyphHeight } ?: (3 to 5)) }
    var x by remember { mutableIntStateOf(initial?.x ?: 0) }
    var y by remember { mutableIntStateOf(initial?.y ?: 0) }
    var textColor by remember { mutableIntStateOf(initial?.color ?: color) }
    var customColor by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    val block = PixelText(selected ?: "new", text, x, y, font.first, font.second, textColor)
    val valid = PixelFont.fits(block, doc.width, doc.height)
    val supported = PixelFont.supports(text)
    val textWidth = (text.length * (font.first + 1) - 1).coerceAtLeast(0)
    val maxX = (doc.width - textWidth).coerceAtLeast(0)
    val maxY = (doc.height - font.second).coerceAtLeast(0)
    fun move(dx: Int, dy: Int) { x = (x + dx).coerceIn(0, maxX); y = (y + dy).coerceIn(0, maxY) }
    val preview = doc.copy(texts = doc.texts.filterNot { it.id == selected } + if (text.isNotBlank() && supported) listOf(block) else emptyList())
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(modifier = Modifier.fillMaxSize().testTag("pixelTextScreen"), containerColor = MirageBackground,
            topBar = { MirageBar("Chèn chữ Pixel", close) {
                TextButton(enabled = selected != null, onClick = { delete = true }) { Icon(Icons.Default.DeleteOutline, null); Text("Xóa khối") }
            } }, bottomBar = {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().background(MirageBackground).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MirageButton("Hủy bỏ", close, Modifier.weight(1f), secondary = true)
                    MirageButton("Áp dụng chữ", {
                        if (vm.text(block.copy(id = selected ?: UUID.randomUUID().toString()))) close()
                    }, Modifier.weight(2f).testTag("applyPixelText"), enabled = valid)
                }
            }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Màn hình trực quan (${doc.width}×${doc.height} LED)", style = MaterialTheme.typography.titleSmall)
                MatrixObjectPreview(preview, if (text.isNotBlank() && supported) block else null, onMove = { dx, dy -> move(dx, dy) })
                Text("Khối chữ · X:$x, Y:$y · Kéo khối chữ để di chuyển", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
                MirageCard(color = MirageSage.copy(alpha = .12f)) {
                    Text("Chữ là một khối riêng nằm trên lớp pixel. Bút và Tẩy không làm thay đổi khối chữ.", style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChoiceTile("+ Khối mới", selected == null) { selected = null; text = ""; x = 0; y = 0 }
                    doc.texts.forEach { b -> ChoiceTile(b.text, selected == b.id) {
                        selected = b.id; text = b.text; font = b.glyphWidth to b.glyphHeight; x = b.x; y = b.y; textColor = b.color
                    } }
                }
                MirageCard {
                    Text("Nội dung chữ", style = MaterialTheme.typography.titleMedium)
                    Text("Chỉ hỗ trợ ASCII / Tiếng Việt không dấu", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
                    OutlinedTextField(text, { text = it.take(64) }, modifier = Modifier.fillMaxWidth().testTag("pixelTextInput"),
                        singleLine = true, isError = !supported, label = { Text("Nhập văn bản") },
                        trailingIcon = { IconButton(onClick = { text = "" }) { Icon(Icons.Default.Cancel, "Xóa văn bản") } })
                    if (!supported) Text("Ký tự chưa hỗ trợ. Hãy dùng tiếng Việt không dấu, ví dụ XE HOI.", color = MaterialTheme.colorScheme.error)
                    else if (text.isNotBlank() && !valid) Text("Khối chữ vượt khổ ${doc.width}×${doc.height}. Hãy giảm cỡ font hoặc rút ngắn nội dung.", color = MaterialTheme.colorScheme.error)
                    Text("Kích thước font Bitmap", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3 to 5, 5 to 7, 7 to 9).forEach { size -> ChoiceTile("${size.first} × ${size.second} px", font == size, Modifier.weight(1f)) {
                            font = size
                            x = x.coerceAtMost((doc.width - (text.length * (size.first + 1) - 1)).coerceAtLeast(0))
                            y = y.coerceAtMost(doc.height - size.second)
                        } }
                    }
                    Text("Căn lề & vị trí đối tượng", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceTile("Căn trái", x == 0, Modifier.weight(1f)) { x = 0 }
                        ChoiceTile("Căn giữa", x == maxX / 2, Modifier.weight(1f).testTag("centerPixelText")) { x = maxX / 2 }
                        ChoiceTile("Căn phải", x == maxX, Modifier.weight(1f)) { x = maxX }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { move(-1, 0) }, enabled = x > 0) { Icon(Icons.Default.ArrowLeft, "Dịch trái 1 pixel") }
                        Column {
                            IconButton(onClick = { move(0, -1) }, enabled = y > 0) { Icon(Icons.Default.ArrowDropUp, "Dịch lên 1 pixel") }
                            IconButton(onClick = { move(0, 1) }, enabled = y < maxY) { Icon(Icons.Default.ArrowDropDown, "Dịch xuống 1 pixel") }
                        }
                        IconButton(onClick = { move(1, 0) }, enabled = x < maxX) { Icon(Icons.Default.ArrowRight, "Dịch phải 1 pixel") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(x.toString(), { it.toIntOrNull()?.let { n -> x = n.coerceIn(0, maxX) } }, label = { Text("Vị trí X") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        OutlinedTextField(y.toString(), { it.toIntOrNull()?.let { n -> y = n.coerceIn(0, maxY) } }, label = { Text("Vị trí Y") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                    Text("Màu sắc đèn LED chữ", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        listOf(color, 0xfff4b266.toInt(), 0xff6ed4d8.toInt(), 0xfff2849e.toInt(), 0xffffffff.toInt()).distinct().forEach { c ->
                            ColorSwatch(c, c == textColor) { textColor = c }
                        }
                        IconButton(onClick = { customColor = true }) { Icon(Icons.Default.Colorize, "Màu chữ tùy chỉnh") }
                    }
                }
                Text("Xuất hoặc gửi lên LED sẽ tự động gộp chữ vào ma trận; bản gốc vẫn giữ lớp chữ để chỉnh sửa.", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
            }
        }
        if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Xóa khối chữ?") },
            confirmButton = { TextButton(onClick = { selected?.let(vm::deleteText); close() }) { Text("Xóa khối") } },
            dismissButton = { TextButton(onClick = { delete = false }) { Text("Hủy") } })
        if (customColor) {
            var draft by remember { mutableIntStateOf(textColor) }
            AlertDialog(onDismissRequest = { customColor = false }, title = { Text("Màu chữ") }, text = { ColorEditor(draft, { draft = it }) },
                confirmButton = { TextButton(onClick = { textColor = draft; customColor = false }) { Text("Chọn màu") } }, dismissButton = { TextButton(onClick = { customColor = false }) { Text("Hủy") } })
        }
    }
}

@Composable
private fun MatrixObjectPreview(doc: PixelDocument, block: PixelText?, mirror: MirrorMode = MirrorMode.NONE, onMove: ((Int, Int) -> Unit)? = null) {
    val pixels = remember(doc) { doc.composite() }
    val currentBlock by rememberUpdatedState(block)
    val move by rememberUpdatedState(onMove)
    Canvas(Modifier.fillMaxWidth().aspectRatio(doc.width.toFloat() / doc.height).clip(RoundedCornerShape(12.dp)).background(Color(0xff0b0f12), RoundedCornerShape(12.dp))
        .border(2.dp, MirageInk, RoundedCornerShape(12.dp)).testTag("textObjectPreview")
        .pointerInput(doc.width, doc.height) {
            var origin: PixelText? = null
            var distance = Offset.Zero
            var previousX = 0; var previousY = 0
            detectDragGestures(onDragStart = { point ->
                val cell = size.width.toFloat() / doc.width
                origin = currentBlock?.takeIf { b -> point.x / cell >= b.x && point.x / cell <= b.x + b.text.length * (b.glyphWidth + 1) - 1 && point.y / cell >= b.y && point.y / cell <= b.y + b.glyphHeight }
                distance = Offset.Zero; previousX = 0; previousY = 0
            }, onDrag = { change, amount ->
                if (origin != null && move != null) {
                    change.consume(); distance += amount
                    val cell = size.width.toFloat() / doc.width
                    val dx = (distance.x / cell).roundToInt(); val dy = (distance.y / cell).roundToInt()
                    move?.invoke(dx - previousX, dy - previousY); previousX = dx; previousY = dy
                }
            })
        }) {
        val cell = size.width / doc.width
        pixels.forEachIndexed { index, c ->
            drawRect(if (c == 0) Color(0xff182127) else Color(c), Offset(index % doc.width * cell + .5f, index / doc.width * cell + .5f), Size((cell - 1f).coerceAtLeast(1f), (cell - 1f).coerceAtLeast(1f)))
        }
        if (mirror == MirrorMode.HORIZONTAL || mirror == MirrorMode.BOTH) drawLine(MirageSage, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2f)
        if (mirror == MirrorMode.VERTICAL || mirror == MirrorMode.BOTH) drawLine(MirageSage, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f)
        block?.let { b ->
            val left = b.x * cell; val top = b.y * cell
            val width = (b.text.length * (b.glyphWidth + 1) - 1).coerceAtLeast(1) * cell
            val height = b.glyphHeight * cell
            drawRect(MirageSage, Offset(left, top), Size(width, height), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
            listOf(Offset(left, top), Offset(left + width, top), Offset(left, top + height), Offset(left + width, top + height)).forEach { point ->
                drawRect(Color.White, point - Offset(3f, 3f), Size(6f, 6f))
            }
        }
    }
}
