package com.carled.app.presentation.screens.canvas

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*
import kotlinx.coroutines.*
import kotlin.math.*

@Composable
fun StudioScreen(viewModel: StudioViewModel, onBack: () -> Unit, importOnOpen: Boolean = false) {
    val state by viewModel.state.collectAsState()
    val doc = state.document
    var size by remember { mutableStateOf(64 to 32) }
    var panel by remember { mutableStateOf<String?>(null) }
    var leave by remember { mutableStateOf(false) }
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var imageBusy by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var initialImport by remember { mutableStateOf(importOnOpen) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            imageBusy = true; imageError = null
            try {
                image = withContext(Dispatchers.IO) {
                    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri).use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
                    require(bounds.outWidth > 0 && bounds.outHeight > 0)
                    var sample = 1
                    while (max(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
                    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(uri).use {
                        requireNotNull(android.graphics.BitmapFactory.decodeStream(it, null, options))
                    }
                }; panel = "import"
            } catch (e: Exception) { imageError = "Không thể đọc ảnh. Hãy chọn ảnh khác." }
            finally { imageBusy = false }
        }
    }
    fun selectImage() { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    fun back() {
        if (state.sending) viewModel.feedback("Đang gửi nội dung. Chờ thiết bị phản hồi trước khi rời màn hình.")
        else if (state.dirty) leave = true else onBack()
    }
    BackHandler { back() }
    LaunchedEffect(doc != null) { if (doc != null && initialImport) { initialImport = false; selectImage() } }
    Scaffold(topBar = {
        MirageBar(if (doc == null) "Tạo thiết kế" else "Trình vẽ pixel", ::back) {
            if (doc != null) {
                IconButton(onClick = viewModel::undo, enabled = state.canUndo && !state.sending) { Icon(Icons.AutoMirrored.Filled.Undo, "Hoàn tác") }
                IconButton(onClick = viewModel::redo, enabled = state.canRedo && !state.sending) { Icon(Icons.AutoMirrored.Filled.Redo, "Làm lại") }
            }
        }
    }, bottomBar = {
        if (doc != null) Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MirageButton(if (state.sending) "Đang lưu lên LED…" else "Lưu lên thiết bị LED", viewModel::send, Modifier.fillMaxWidth().testTag("saveToLed"), enabled = state.connected && !state.sending && doc.width == 64 && doc.height == 32)
            MirageButton("Lưu vào Gallery", viewModel::save, Modifier.fillMaxWidth().testTag("saveToGallery"), enabled = !state.sending, secondary = true)
        }
    }, containerColor = MirageBackground) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (doc == null) {
                Text("Bắt đầu từ từng điểm ảnh", style = MaterialTheme.typography.headlineSmall)
                Text("Chọn độ phân giải cho thiết kế mới. Kích thước được giữ nguyên khi lưu bản nháp.", color = MirageMuted)
                PixelDocument.sizes.forEach { (w, h) ->
                    MirageCard(Modifier.fillMaxWidth().clickable { size = w to h }, if (size == w to h) MirageSage.copy(alpha = .3f) else Color.White) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = size == w to h, onClick = { size = w to h })
                            Column { Text("$w × $h", style = MaterialTheme.typography.titleLarge); Text(if (w == h) "Canvas vuông" else "Canvas ngang · 2:1", color = MirageMuted) }
                        }
                        Box(Modifier.fillMaxWidth(.5f).aspectRatio(w.toFloat() / h).background(MirageSage.copy(alpha = .2f)).border(1.dp, MirageInk))
                    }
                }
                MirageButton("Tạo canvas", { viewModel.create(size.first, size.second) }, Modifier.fillMaxWidth())
                Text("Kiểm tra tương thích với thiết bị khi gửi. Giao thức hiện tại nhận ảnh 64×32.", color = MirageMuted)
            } else {
                if (doc.width == 64 && doc.height == 32) MirageCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (state.sending) "LIVE · Tạm dừng để lưu" else if (!state.connected) "LIVE · Chờ kết nối LED" else if (!state.liveEnabled) "LIVE · Đã tắt" else if (state.syncing) "LIVE · Đang đồng bộ…" else "LIVE · Đang bật", style = MaterialTheme.typography.titleMedium)
                            Text("Nét vẽ gửi trực tiếp. Chọn Lưu lên thiết bị LED để giữ lại sau khi tắt nguồn.", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
                        }
                        Switch(state.liveEnabled, viewModel::live, enabled = !state.sending, modifier = Modifier.testTag("liveToggle"))
                    }
                    state.liveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                OutlinedButton(onClick = viewModel::clear, enabled = !state.sending, modifier = Modifier.fillMaxWidth().testTag("clearCanvas")) { Text("Xóa trắng bảng") }
                OutlinedTextField(state.name, viewModel::name, label = { Text("Tên thiết kế") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${doc.width} × ${doc.height} · ${if (state.connected) "Đã kết nối" else "Vẽ ngoại tuyến"}")
                    IconToggleButton(checked = state.grid, onCheckedChange = { viewModel.grid() }) { Icon(Icons.Default.GridOn, "Lưới pixel") }
                }
                OutlinedButton(onClick = ::selectImage,
                    enabled = !state.sending && !imageBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("importFromGallery"),
                    shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MirageInk)) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nhập ảnh từ thư viện")
                }
                InteractiveGrid(state, viewModel)
                Text("Một ngón để vẽ · Hai ngón di chuyển và thu phóng", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
                MirageCard {
                    Text("Cỡ cọ · ${state.brushSize} px")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..4).forEach { n -> FilterChip(selected = state.brushSize == n, onClick = { viewModel.brush(n) }, label = { Text("$n px") }) }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.palettes.flatMap { it.colors }.distinct().take(16).forEach { c -> ColorSwatch(c, c == state.color) { viewModel.color(c) } }
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Bút" to PixelTool.PEN, "Tẩy" to PixelTool.ERASER, "Hút màu" to PixelTool.PICKER).forEach { (label, tool) ->
                        FilterChip(selected = state.tool == tool, onClick = { viewModel.tool(tool) }, label = { Text(label) })
                    }
                    AssistChip(onClick = { panel = "colors" }, label = { Text("Màu") })
                    AssistChip(onClick = { panel = "tools" }, label = { Text("Thêm") })
                }
                if (state.tool == PixelTool.PICKER) Text("Chạm pixel để lấy màu. Chọn Bút để hủy.")
                if (state.mirror != MirrorMode.NONE) Text("Đối xứng: ${mirrorLabel(state.mirror)}", color = MirageMuted)
                if (state.tool == PixelTool.TEXT) MirageButton("Chỉnh sửa khối chữ", { panel = "text" }, secondary = true)
                if (!state.connected) Text("Bạn vẫn có thể vẽ và lưu vào Gallery. Kết nối LED để bật LIVE và lưu lên thiết bị.", color = MirageMuted)
                if (doc.width != 64 || doc.height != 32) Text("Thiết kế này chỉ lưu nháp: giao thức hiện tại chỉ gửi được 64×32.", color = MirageMuted)
            }
            if (imageBusy) { CircularProgressIndicator(); Text("Đang đọc ảnh…") }
            imageError?.let { Text(it, color = MaterialTheme.colorScheme.error); MirageButton("Chọn ảnh khác", ::selectImage, secondary = true) }
            state.feedback?.let { Text(it, Modifier.miragePanel(MirageSage.copy(alpha = .15f)).padding(12.dp)) }
        }
    }
    if (leave) AlertDialog(onDismissRequest = { leave = false }, title = { Text("Rời bản vẽ?") }, text = { Text("Thay đổi chưa lưu sẽ bị mất. Chọn Lưu vào Gallery để giữ bản vẽ trên điện thoại. LIVE không tự lưu.") },
        confirmButton = { TextButton(onClick = { leave = false; onBack() }) { Text("Bỏ thay đổi") } }, dismissButton = { TextButton(onClick = { leave = false }) { Text("Tiếp tục vẽ") } })
    if (doc != null) when (panel) {
        "colors" -> PaletteDialog(state, viewModel) { panel = null }
        "tools" -> AlertDialog(onDismissRequest = { panel = null }, title = { Text("Công cụ & đối xứng") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hình dạng")
                PixelShape.entries.forEach { shape ->
                    FilterChip(selected = state.shape == shape && state.tool == PixelTool.SHAPE, onClick = { viewModel.shape(shape, state.filled) }, label = { Text(when(shape) { PixelShape.LINE -> "Đường thẳng"; PixelShape.RECTANGLE -> "Chữ nhật"; PixelShape.ELLIPSE -> "Elip" }) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(state.filled, { viewModel.shape(state.shape, it) }, enabled = state.shape != PixelShape.LINE); Text("Tô kín hình") }
                MirageButton("Đổ màu vùng kín", { viewModel.tool(PixelTool.FILL); panel = null }, secondary = true)
                Text("Đối xứng nét vẽ")
                MirrorMode.entries.forEach { m -> FilterChip(selected = state.mirror == m, onClick = { viewModel.mirror(m) }, label = { Text(mirrorLabel(m)) }) }
                MirageButton("Văn bản", { viewModel.tool(PixelTool.TEXT); panel = "text" }, secondary = true)
                MirageButton("Nhập ảnh", { panel = null; selectImage() }, secondary = true)
                Row(verticalAlignment = Alignment.CenterVertically) { Switch(state.grid, { viewModel.grid() }); Text("Lưới pixel") }
            }
        }, confirmButton = { TextButton(onClick = { panel = null }) { Text("Xong") } })
        "text" -> PixelTextDialog(doc, state.color, viewModel) { panel = null }
        "import" -> image?.let { ImportImageDialog(it, doc, { viewModel.importPixels(it); panel = null }, { panel = null }, ::selectImage) }
    }
}

private fun mirrorLabel(m: MirrorMode) = when(m) { MirrorMode.NONE -> "Tắt"; MirrorMode.HORIZONTAL -> "Trái–phải"; MirrorMode.VERTICAL -> "Trên–dưới"; MirrorMode.BOTH -> "Cả hai trục" }

@Composable
private fun InteractiveGrid(state: StudioState, vm: StudioViewModel) {
    val doc = state.document ?: return
    val pixels = remember(doc) { doc.composite() }
    var zoom by remember(doc.width, doc.height) { mutableFloatStateOf(1f) }
    var pan by remember(doc.width, doc.height) { mutableStateOf(Offset.Zero) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.testTag("pixelCanvas").fillMaxWidth().aspectRatio(doc.width.toFloat() / doc.height).clipToBounds().border(2.dp, MirageInk).background(Color(0xff18241c))
            .pointerInput(doc.width, doc.height) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false); down.consume()
                    fun position(point: Offset): Pair<Int, Int> {
                        val cell = size.width.toFloat() / doc.width * zoom
                        return floor((point.x - pan.x) / cell).toInt() to floor((point.y - pan.y) / cell).toInt()
                    }
                    val p = position(down.position); vm.down(p.first, p.second)
                    var transforming = false
                    try {
                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.count { it.pressed } > 1) {
                                if (!transforming) { vm.end(cancel = true); transforming = true }
                                val center = event.calculateCentroid(useCurrent = false)
                                val previousZoom = zoom
                                zoom = (zoom * event.calculateZoom()).coerceIn(1f, 8f)
                                pan = center - (center - pan) * (zoom / previousZoom) + event.calculatePan()
                                pan = Offset(pan.x.coerceIn(size.width * (1 - zoom), 0f), pan.y.coerceIn(size.height * (1 - zoom), 0f))
                            } else if (!transforming) event.changes.firstOrNull()?.let { change ->
                                val at = position(change.position); vm.move(at.first, at.second)
                            }
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        if (!transforming) vm.end()
                    } finally { vm.end(cancel = true) }
                }
            }) {
            val cell = size.width / doc.width * zoom
            for (y in 0 until doc.height) for (x in 0 until doc.width) {
                val p = pixels[y * doc.width + x]
                val pos = Offset(pan.x + x * cell, pan.y + y * cell)
                val emptyColor = if ((x + y) % 2 == 0) Color(0xff24372b) else Color(0xff2d4435)
                drawRect(if (p == 0) emptyColor else Color(p), pos, Size(cell, cell))
                if (state.grid && cell >= 3) drawRect(Color(0xff56655b), pos, Size(cell, cell), style = Stroke(.5f))
            }
            if (state.mirror == MirrorMode.HORIZONTAL || state.mirror == MirrorMode.BOTH) drawLine(MirageSage, Offset(pan.x + doc.width * cell / 2, 0f), Offset(pan.x + doc.width * cell / 2, size.height), 2f)
            if (state.mirror == MirrorMode.VERTICAL || state.mirror == MirrorMode.BOTH) drawLine(MirageSage, Offset(0f, pan.y + doc.height * cell / 2), Offset(size.width, pan.y + doc.height * cell / 2), 2f)
            if (state.tool == PixelTool.TEXT) doc.texts.forEach { b ->
                drawRect(Color.White, Offset(pan.x + b.x * cell, pan.y + b.y * cell), Size((b.text.length * (b.glyphWidth + 1) - 1) * cell, b.glyphHeight * cell), style = Stroke(2f))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Thu phóng ${(zoom * 100).toInt()}%", Modifier.weight(1f), color = MirageMuted)
            TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Căn giữa") }
        }
    }
}
