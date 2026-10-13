package com.carled.app.presentation.screens.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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

    // Flush batch mỗi 40ms (~25 FPS)
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(40)
            viewModel.flushBatch()
        }
    }

    // Tự ẩn thông báo sau 3 giây
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
                        Text("Vẽ Pixel Art", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Canvas LED 64×32", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearCanvas() }) {
                        if (uiState.isClearing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ErrorRed, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa canvas", tint = ErrorRed)
                        }
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
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Canvas cleared event notification ──
            if (uiState.lastEventFromDevice != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("● ", color = NeonGreen, fontWeight = FontWeight.Bold)
                    Text(uiState.lastEventFromDevice!!, color = NeonGreen, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── LED Pixel Canvas 64×32 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)  // 64:32 = 2:1
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(1.dp, SurfaceLightDark, RoundedCornerShape(8.dp))
            ) {
                LedPixelCanvas(
                    pixels = uiState.pixels,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val px = (offset.x / size.width * 64).toInt().coerceIn(0, 63)
                                val py = (offset.y / size.height * 32).toInt().coerceIn(0, 31)
                                viewModel.drawPixel(px, py)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val px = (change.position.x / size.width * 64).toInt().coerceIn(0, 63)
                                val py = (change.position.y / size.height * 32).toInt().coerceIn(0, 31)
                                viewModel.drawPixel(px, py)
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Color Picker Row ──
            Text("MÀU VẼ", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            val colorPalette = listOf(
                Color.White, Color.Red, Color(0xFFFF8C00), Color.Yellow,
                Color(0xFF00E676), Color.Cyan, Color(0xFF2196F3), Color(0xFFAA00FF),
                Color(0xFFFF4081), Color(0xFFFFD700), Color(0xFF00BCD4), Color.Black
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                colorPalette.forEach { color ->
                    val isSelected = uiState.drawColor == color
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 38.dp else 32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else SurfaceLightDark,
                                shape = CircleShape
                            )
                            .clickable { viewModel.setDrawColor(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Current Color Preview ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(uiState.drawColor)
                        .border(1.dp, SurfaceLightDark, RoundedCornerShape(8.dp))
                )
                Column {
                    Text("Màu đang vẽ", color = TextSecondary, fontSize = 11.sp)
                    val c = uiState.drawColor
                    Text(
                        "R:${(c.red * 255).toInt()}  G:${(c.green * 255).toInt()}  B:${(c.blue * 255).toInt()}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Info footer ──
            Text(
                "Vuốt ngón tay lên canvas để vẽ pixel • Gửi BLE tự động 25 FPS",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
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
                        size = Size(cellW - 0.5f, cellH - 0.5f) // gap 0.5px cho gridline
                    )
                }
            }
        }
        // Vẽ grid lines nhẹ để phân biệt pixel
        val gridColor = Color.White.copy(alpha = 0.04f)
        for (col in 0..64) {
            drawLine(gridColor, Offset(col * cellW, 0f), Offset(col * cellW, size.height), strokeWidth = 0.5f)
        }
        for (row in 0..32) {
            drawLine(gridColor, Offset(0f, row * cellH), Offset(size.width, row * cellH), strokeWidth = 0.5f)
        }
    }
}
