package com.carled.app.presentation.screens.canvas

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.MirrorMode
import com.carled.app.domain.model.PixelTool
import com.carled.app.presentation.components.MirageBackground
import com.carled.app.presentation.components.MirageInk
import com.carled.app.presentation.components.MirageMuted
import com.carled.app.presentation.components.MirageSage
import com.carled.app.presentation.components.miragePanel
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.max

fun rasterizeBitmapToPixels(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): List<Int> {
    val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    val pixels = IntArray(targetWidth * targetHeight)
    scaled.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
    return pixels.map { it }.toList()
}

@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit,
    onProfile: () -> Unit = {},
    importOnOpen: Boolean = false
) {
    val state by viewModel.state.collectAsState()
    val doc = state.document
    var panel by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var imageBusy by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var initialImport by remember { mutableStateOf(importOnOpen) }
    var showExportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) scope.launch {
                imageBusy = true; imageError = null
                try {
                    image = withContext(Dispatchers.IO) {
                        val bounds = android.graphics.BitmapFactory.Options()
                            .apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)
                            .use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
                        require(bounds.outWidth > 0 && bounds.outHeight > 0)
                        var sample = 1
                        while (max(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
                        val options =
                            android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                        context.contentResolver.openInputStream(uri).use {
                            requireNotNull(
                                android.graphics.BitmapFactory.decodeStream(
                                    it,
                                    null,
                                    options
                                )
                            )
                        }
                    }; panel = "import"
                } catch (e: Exception) {
                    imageError = "Không thể đọc ảnh. Hãy chọn ảnh khác."
                } finally {
                    imageBusy = false
                }
            }
        }

    fun selectImage() {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun back() {
        if (state.sending) viewModel.feedback("Đang gửi nội dung. Chờ thiết bị phản hồi trước khi rời màn hình.")
        else if (state.dirty) leave = true else onBack()
    }
    BackHandler { back() }
    LaunchedEffect(doc != null) {
        if (doc != null && initialImport) {
            initialImport = false; selectImage()
        }
    }

    Scaffold(
        containerColor = MirageBackground,
        bottomBar = {
            if (doc != null) Column {
                CanvasBottomBar(
                    state = state,
                    vm = viewModel,
                    onImport = { if (!state.sending && !imageBusy) selectImage() },
                    onBack = ::back
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MirageBackground)
                        .border(width = 1.dp, color = MirageInk.copy(alpha = .15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${doc.width} × ${doc.height}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MirageInk,
                        modifier = Modifier
                            .border(1.dp, MirageInk, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    BasicTextFieldFilename(state.name, viewModel::name)
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Ink)
        ) {
            LiveEditorTitleBar(syncing = state.syncing, onClose = ::back)
            Row(Modifier.fillMaxSize()) {
                CanvasSidebar(
                    state = state, vm = viewModel,
                    onMenu = { showMenu = true },
                    onOptions = { panel = "options" },
                    onColor = { panel = "colors" },
                    onText = { panel = "text" }
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                ) {
                    if (doc != null) {
                        InteractiveGrid(state, viewModel)

                        if (state.tool == PixelTool.PICKER) Text(
                            "Chạm pixel để lấy màu. Chọn Bút để hủy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MirageMuted
                        )
                        if (state.tool == PixelTool.SELECT) Text(
                            "Kéo để chọn vùng, dùng Cắt/Sao chép rồi chuyển sang Di chuyển.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MirageMuted
                        )
                        if (state.tool == PixelTool.MOVE && state.selection == null) Text(
                            "Chọn một vùng trước khi di chuyển.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MirageMuted
                        )
                        if (state.mirror != MirrorMode.NONE) Text(
                            "Đối xứng: ${mirrorLabel(state.mirror)}",
                            color = MirageMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!state.connected) Text(
                            "Bạn vẫn có thể vẽ và lưu vào Gallery. Kết nối LED để bật LIVE và lưu lên thiết bị.",
                            color = MirageMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (doc.width != 64 || doc.height != 32) Text(
                            "Thiết kế này chỉ lưu nháp: giao thức hiện tại chỉ gửi được 64×32.",
                            color = MirageMuted,
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (imageBusy) Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp)); Text("Đang đọc ảnh…")
                        }
                        imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        state.feedback?.let {
                            Text(it, Modifier.miragePanel(MirageSage.copy(alpha = .15f)).padding(12.dp))
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f)
                                .border(2.dp, Cream.copy(alpha = .3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Chọn kích thước canvas để bắt đầu", color = Cream.copy(alpha = .5f))
                        }
                    }
                }
            }
        }
    }

    if (doc == null) CreateCanvasDialog(
        onDismiss = onBack,
        onCreate = { w, h -> viewModel.create(w, h) }
    )

    if (leave) AlertDialog(
        onDismissRequest = { leave = false },
        title = { Text("Rời bản vẽ?") },
        text = { Text("Thay đổi chưa lưu sẽ bị mất. Chọn Lưu vào Gallery để giữ bản vẽ trên điện thoại. LIVE không tự lưu.") },
        confirmButton = {
            TextButton(onClick = {
                leave = false; onBack()
            }) { Text("Bỏ thay đổi") }
        },
        dismissButton = { TextButton(onClick = { leave = false }) { Text("Tiếp tục vẽ") } })

    if (showMenu) NavMenuSheet(
        state = state,
        onSendToLed = viewModel::send,
        onSaveDraft = viewModel::save,
        onExportToGallery = { showExportDialog = true },
        onHome = onBack,
        onProfile = onProfile,
        close = { showMenu = false }
    )

    if (showExportDialog && doc != null) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { format ->
                val success = ImageExportHelper.exportDocument(context, doc, state.name, format)
                if (success) {
                    viewModel.feedback("Đã xuất ảnh ra thư mục Pictures/CarLED thành công!")
                } else {
                    viewModel.feedback("Không thể xuất ảnh.")
                }
            }
        )
    }

    if (doc != null) when (panel) {
        "colors" -> PaletteDialog(state, viewModel) { panel = null }
        "options" -> CanvasOptionsSheet(state, viewModel) { panel = null }
        "text" -> PixelTextDialog(doc, state.color, viewModel) { panel = null }
        "import" -> image?.let {
            ImportImageDialog(
                it,
                doc,
                { bmp ->
                    val rasterizedPixels = rasterizeBitmapToPixels(bmp, doc.width, doc.height)
                    viewModel.importPixels(rasterizedPixels)
                    panel = null
                },
                { panel = null },
                ::selectImage
            )
        }
    }
}

private fun mirrorLabel(m: MirrorMode) = when (m) {
    MirrorMode.NONE -> "Tắt"; MirrorMode.HORIZONTAL -> "Trái–phải"; MirrorMode.VERTICAL -> "Trên–dưới"; MirrorMode.BOTH -> "Cả hai trục"
}

@Composable
private fun LiveEditorTitleBar(syncing: Boolean, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Cream)
            .border(2.dp, Ink),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(start = 10.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Green)
                .border(1.5.dp, Ink, CircleShape)
        )
        Text(
            "Live editor",
            color = Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        Icon(
            Icons.Default.Sync,
            contentDescription = if (syncing) "Đang đồng bộ" else null,
            tint = Green.copy(alpha = if (syncing) 1f else 0.4f),
            modifier = Modifier
                .padding(end = 10.dp)
                .size(18.dp)
        )
        Box(
            Modifier
                .fillMaxHeight()
                .width(48.dp)
                .background(Green)
                .border(1.5.dp, Ink)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text("X", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BasicTextFieldFilename(value: String, onChange: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    if (editing) {
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            modifier = Modifier
                .widthIn(max = 180.dp)
                .testTag("filenameField"),
            textStyle = MaterialTheme.typography.labelLarge,
            trailingIcon = {
                IconButton(onClick = { editing = false }) {
                    Icon(
                        Icons.Default.Check,
                        "Xong"
                    )
                }
            }
        )
    } else {
        Text(
            value.ifBlank { "Filename" },
            style = MaterialTheme.typography.labelLarge,
            color = MirageInk,
            modifier = Modifier
                .clickable { editing = true }
                .testTag("filenameLabel")
        )
    }
}

@Composable
private fun InteractiveGrid(state: StudioState, vm: StudioViewModel) {
    val doc = state.document ?: return
    val pixels = remember(doc) { doc.composite() }
    var zoom by remember(doc.width, doc.height) { mutableFloatStateOf(1f) }
    var pan by remember(doc.width, doc.height) { mutableStateOf(Offset.Zero) }

    val bitmap = remember(doc.width, doc.height) {
        android.graphics.Bitmap.createBitmap(doc.width, doc.height, android.graphics.Bitmap.Config.ARGB_8888)
    }

    remember(pixels, doc.width, doc.height) {
        val argbPixels = IntArray(pixels.size) { i ->
            val p = pixels[i]
            if (p == 0) {
                val x = i % doc.width
                val y = i / doc.width
                if ((x + y) % 2 == 0) 0xff24372b.toInt() else 0xff2d4435.toInt()
            } else {
                p
            }
        }
        bitmap.setPixels(argbPixels, 0, doc.width, 0, 0, doc.width, doc.height)
        true
    }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .testTag("pixelCanvas")
                .fillMaxWidth()
                .aspectRatio(doc.width.toFloat() / doc.height)
                .clipToBounds()
                .border(2.dp, MirageInk)
                .background(Color(0xff18241c))
                .pointerInput(doc.width, doc.height) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false); down.consume()

                        fun position(point: Offset, currentCellSize: Float): Pair<Int, Int> {
                            return floor((point.x - pan.x) / currentCellSize).toInt() to floor((point.y - pan.y) / currentCellSize).toInt()
                        }

                        val initialCellSize = size.width.toFloat() / doc.width * zoom
                        val p = position(down.position, initialCellSize)
                        vm.down(p.first, p.second)
                        var transforming = false
                        try {
                            do {
                                val event = awaitPointerEvent()
                                val activeCellSize = size.width.toFloat() / doc.width * zoom
                                if (event.changes.count { it.pressed } > 1) {
                                    if (!transforming) {
                                        vm.end(cancel = true); transforming = true
                                    }
                                    val center = event.calculateCentroid(useCurrent = false)
                                    val previousZoom = zoom
                                    zoom = (zoom * event.calculateZoom()).coerceIn(0.5f, 16f)

                                    // Allow free panning like Clip Studio without clamping bounds rigidly to edges
                                    pan = center - (center - pan) * (zoom / previousZoom) + event.calculatePan()
                                } else if (!transforming) event.changes.firstOrNull()
                                    ?.let { change ->
                                        val at = position(change.position, activeCellSize)
                                        vm.move(at.first, at.second)
                                    }
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                            if (!transforming) vm.end()
                        } finally {
                            vm.end(cancel = true)
                        }
                    }
                }) {
            val cell = size.width / doc.width * zoom

            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(pan.x.toInt(), pan.y.toInt()),
                dstSize = IntSize((doc.width * cell).toInt(), (doc.height * cell).toInt()),
                filterQuality = FilterQuality.None
            )
            if (state.grid && cell >= 3) {
                val gridColor = Color(0xff56655b)
                for (x in 0..doc.width) {
                    val xPos = pan.x + x * cell
                    drawLine(gridColor, Offset(xPos, pan.y), Offset(xPos, pan.y + doc.height * cell), strokeWidth = 0.5f)
                }
                for (y in 0..doc.height) {
                    val yPos = pan.y + y * cell
                    drawLine(gridColor, Offset(pan.x, yPos), Offset(pan.x + doc.width * cell, yPos), strokeWidth = 0.5f)
                }
            }

            if (state.mirror == MirrorMode.HORIZONTAL || state.mirror == MirrorMode.BOTH) drawLine(
                MirageSage,
                Offset(pan.x + doc.width * cell / 2, 0f),
                Offset(pan.x + doc.width * cell / 2, size.height),
                2f
            )
            if (state.mirror == MirrorMode.VERTICAL || state.mirror == MirrorMode.BOTH) drawLine(
                MirageSage,
                Offset(0f, pan.y + doc.height * cell / 2),
                Offset(size.width, pan.y + doc.height * cell / 2),
                2f
            )
            if (state.tool == PixelTool.TEXT) doc.texts.forEach { b ->
                drawRect(
                    Color.White,
                    Offset(pan.x + b.x * cell, pan.y + b.y * cell),
                    Size((b.text.length * (b.glyphWidth + 1) - 1) * cell, b.glyphHeight * cell),
                    style = Stroke(2f)
                )
            }
            state.selection?.let { sel ->
                val left = pan.x + sel.left * cell
                val top = pan.y + sel.top * cell
                val w = (sel.right - sel.left + 1) * cell
                val h = (sel.bottom - sel.top + 1) * cell
                drawRect(
                    Color.White,
                    Offset(left, top),
                    Size(w, h),
                    style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Thu phóng ${(zoom * 100).toInt()}%",
                Modifier.weight(1f),
                color = MirageMuted,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("Căn giữa") }
        }
    }
}