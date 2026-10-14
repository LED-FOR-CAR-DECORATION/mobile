package com.carled.app.presentation.screens.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: CanvasViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1. Tự động khởi tạo bảng đen trên ESP32 ngay khi mở màn hình (Opcode 0x15)
    LaunchedEffect(Unit) {
        viewModel.initLiveCanvas()
    }

    // 2. Định kỳ Flush batch points sang BLE mỗi 35ms (~28 FPS) theo chuẩn ble_image_importer
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(35)
            viewModel.flushBatch()
        }
    }

    // 3. Tự ẩn thông báo sau 3 giây
    LaunchedEffect(uiState.lastEventFromDevice) {
        if (uiState.lastEventFromDevice != null) {
            delay(3000)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bút Vẽ Live LED (Canvas)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                        Text("Ma trận 64×32 • Nét vẽ tức thì qua BLE", color = DivoomTextMuted, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Nút Hoàn tác (Undo)
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Hoàn tác",
                            tint = if (uiState.canUndo) DivoomGold else DivoomTextMuted.copy(alpha = 0.4f)
                        )
                    }

                    // Nút Chuyển Bảng Đen / Xóa (Opcode 0x15)
                    IconButton(onClick = { viewModel.clearCanvas() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa bảng vẽ", tint = DivoomDanger)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // ── Device Notification Banner ──
            if (uiState.lastEventFromDevice != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("● ", color = NeonGreen, fontWeight = FontWeight.Bold)
                    Text(uiState.lastEventFromDevice!!, color = NeonGreen, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Header Canvas: Tọa độ & Tool Info ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("BẢNG VẼ 64×32", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    val coord = uiState.touchCoord
                    if (coord != null) {
                        Text(
                            "(${coord.first}, ${coord.second})",
                            color = DivoomGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Chế độ công cụ hiện tại
                Text(
                    text = when (uiState.currentTool) {
                        CanvasTool.PENCIL -> "✏️ Bút nét ${uiState.brushSize}px"
                        CanvasTool.ERASER -> "🧹 Cục tẩy (Nền đen)"
                        CanvasTool.BUCKET -> "🪣 Đổ sơn (Flood Fill)"
                    },
                    color = PrimaryCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── LED Pixel Canvas 64×32 (Tương tác chạm / vuốt liên tục) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f) // 64:32 = 2:1
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.5.dp, DivoomBorder, RoundedCornerShape(10.dp))
            ) {
                LedPixelCanvas(
                    pixels = uiState.pixels,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val px = (offset.x / size.width * 64).toInt().coerceIn(0, 63)
                                    val py = (offset.y / size.height * 32).toInt().coerceIn(0, 31)
                                    viewModel.onTouchStart(px, py)
                                    tryAwaitRelease()
                                    viewModel.onTouchEnd()
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val px = (offset.x / size.width * 64).toInt().coerceIn(0, 63)
                                    val py = (offset.y / size.height * 32).toInt().coerceIn(0, 31)
                                    viewModel.onTouchStart(px, py)
                                },
                                onDragEnd = {
                                    viewModel.onTouchEnd()
                                },
                                onDragCancel = {
                                    viewModel.onTouchEnd()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val px = (change.position.x / size.width * 64).toInt().coerceIn(0, 63)
                                    val py = (change.position.y / size.height * 32).toInt().coerceIn(0, 31)
                                    viewModel.onTouchMove(px, py)
                                }
                            )
                        }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Tool Selector Bar (Bút vẽ, Tẩy, Thùng sơn, Cỡ cọ, Xóa) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Nút Bút vẽ
                ToolIconButton(
                    icon = Icons.Default.Edit,
                    label = "Bút",
                    isSelected = uiState.currentTool == CanvasTool.PENCIL,
                    onClick = { viewModel.setTool(CanvasTool.PENCIL) },
                    modifier = Modifier.weight(1f)
                )

                // Nút Cục tẩy
                ToolIconButton(
                    icon = Icons.Default.AutoFixHigh,
                    label = "Tẩy",
                    isSelected = uiState.currentTool == CanvasTool.ERASER,
                    onClick = { viewModel.setTool(CanvasTool.ERASER) },
                    modifier = Modifier.weight(1f)
                )

                // Nút Thùng sơn (Flood fill)
                ToolIconButton(
                    icon = Icons.Default.FormatColorFill,
                    label = "Đổ màu",
                    isSelected = uiState.currentTool == CanvasTool.BUCKET,
                    onClick = { viewModel.setTool(CanvasTool.BUCKET) },
                    modifier = Modifier.weight(1f)
                )

                // Nút Đổi cỡ cọ (1px vs 2px)
                Button(
                    onClick = {
                        val next = if (uiState.brushSize == 1) 2 else 1
                        viewModel.setBrushSize(next)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DivoomCard),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, DivoomBorder)
                ) {
                    Text(
                        "${uiState.brushSize}px",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Nút Bảng đen (Clear)
                Button(
                    onClick = { viewModel.clearCanvas() },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DivoomDanger.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    border = BorderStroke(1.dp, DivoomDanger.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = DivoomDanger, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bảng đen", color = DivoomDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Bảng Màu Nhanh & Nút Chọn Màu RGB Tùy Chỉnh ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MÀU SẮC", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { viewModel.showColorPicker(true) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = DivoomGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tùy Chỉnh RGB", color = DivoomGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val colorPalette = listOf(
                Color(0xFF00D2FF), // Cyan (ble_image_importer)
                Color(0xFFFF0055), // Hot Pink
                Color(0xFF00F076), // Neon Green
                Color(0xFFFFB800), // Gold / Orange
                Color(0xFFFFFFFF), // White
                Color(0xFF9900FF), // Purple
                Color(0xFFFFD700), // Yellow
                Color(0xFFFF3366), // Red
                Color(0xFF007BFF), // Deep Blue
                Color(0xFFFF6600)  // Bright Orange
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colorPalette.forEach { color ->
                    val isSelected = uiState.drawColor == color && uiState.currentTool != CanvasTool.ERASER
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setDrawColor(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Mẫu Hình Pixel Nhanh (Templates / Stamps) ──
            Text("HÌNH VẼ MẪU NHANH", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TemplateCard(
                    title = "❤️ Trái tim",
                    subtitle = "Đỏ Neon",
                    color = Color(0xFFFF0055),
                    onClick = { viewModel.applyTemplate("heart") },
                    modifier = Modifier.weight(1f)
                )

                TemplateCard(
                    title = "🚗 Xe thể thao",
                    subtitle = "Cyan Sport",
                    color = PrimaryCyan,
                    onClick = { viewModel.applyTemplate("car") },
                    modifier = Modifier.weight(1f)
                )

                TemplateCard(
                    title = "😊 Mặt cười",
                    subtitle = "Vàng Gold",
                    color = DivoomGold,
                    onClick = { viewModel.applyTemplate("smile") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal chọn màu RGB tùy chỉnh
    if (uiState.showColorPicker) {
        CustomColorDialog(
            initialColor = uiState.drawColor,
            onDismiss = { viewModel.showColorPicker(false) },
            onColorSelected = {
                viewModel.setDrawColor(it)
                viewModel.showColorPicker(false)
            }
        )
    }
}

@Composable
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryCyan else DivoomCard
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(1.dp, if (isSelected) PrimaryCyan else DivoomBorder)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) DarkBackground else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                color = if (isSelected) DarkBackground else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TemplateCard(
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DivoomCard),
        border = BorderStroke(1.dp, DivoomBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(subtitle, color = DivoomTextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CustomColorDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var r by remember { mutableStateOf((initialColor.red * 255).toInt()) }
    var g by remember { mutableStateOf((initialColor.green * 255).toInt()) }
    var b by remember { mutableStateOf((initialColor.blue * 255).toInt()) }

    val currentColor = Color(r, g, b)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DivoomCard,
        title = {
            Text("Chọn Màu RGB Tùy Chỉnh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                // Color Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val hex = String.format("#%02X%02X%02X", r, g, b)
                    Text(hex, color = if (r + g + b > 380) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Đỏ (R): $r", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = r.toFloat(),
                    onValueChange = { r = it.toInt() },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252))
                )

                Text("Lục (G): $g", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = g.toFloat(),
                    onValueChange = { g = it.toInt() },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00E676), activeTrackColor = Color(0xFF00E676))
                )

                Text("Lam (B): $b", color = Color(0xFF40C4FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = b.toFloat(),
                    onValueChange = { b = it.toInt() },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF40C4FF), activeTrackColor = Color(0xFF40C4FF))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(currentColor) },
                colors = ButtonDefaults.buttonColors(containerColor = DivoomGold)
            ) {
                Text("Chọn màu này", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = DivoomTextMuted)
            }
        }
    )
}

/**
 * Composable vẽ bitmap pixel 64×32 scale lên màn hình.
 * Mỗi pixel được vẽ là 1 ô chữ nhật.
 */
@Composable
private fun LedPixelCanvas(
    pixels: Array<IntArray>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cellW = size.width / 64f
        val cellH = size.height / 32f
        for (row in 0 until 32) {
            for (col in 0 until 64) {
                val argb = pixels[row][col]
                val color = Color(argb)
                if (color != Color.Black) {
                    drawRect(
                        color = color,
                        topLeft = Offset(col * cellW, row * cellH),
                        size = Size(cellW - 0.4f, cellH - 0.4f)
                    )
                }
            }
        }
        // Vẽ grid lines nhẹ để phân biệt pixel
        val gridColor = Color.White.copy(alpha = 0.05f)
        for (col in 0..64) {
            drawLine(gridColor, Offset(col * cellW, 0f), Offset(col * cellW, size.height), strokeWidth = 0.5f)
        }
        for (row in 0..32) {
            drawLine(gridColor, Offset(0f, row * cellH), Offset(size.width, row * cellH), strokeWidth = 0.5f)
        }
    }
}

