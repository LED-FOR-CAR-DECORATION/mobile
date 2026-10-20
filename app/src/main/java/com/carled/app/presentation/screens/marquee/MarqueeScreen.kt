package com.carled.app.presentation.screens.marquee

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarqueeScreen(
    viewModel: MarqueeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto-clear feedback sau 4 giây
    LaunchedEffect(uiState.feedbackMessage) {
        if (uiState.feedbackMessage != null) {
            delay(4000)
            viewModel.clearFeedback()
        }
    }

    // Animation cursor cho Canvas Preview mô phỏng 64x32
    var previewCursorX by remember { mutableFloatStateOf(64f) }
    var blinkVisible by remember { mutableStateOf(true) }
    var rainbowHue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.speed, uiState.scrollMode, uiState.text, uiState.size) {
        val stepDelay = (85 - (uiState.speed * 0.7f)).toLong().coerceAtLeast(15L)
        val cleanText = MarqueeViewModel.removeVietnameseTones(uiState.text.ifBlank { "CAR LED" })
        val fontSize = if (uiState.size == 2) 13f else 8.5f
        val testPaint = android.graphics.Paint().apply {
            textSize = fontSize
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val totalWidth = testPaint.measureText(cleanText)

        while (isActive) {
            delay(stepDelay)
            if (uiState.scrollMode == "scroll_left") {
                previewCursorX -= 1f
                if (previewCursorX < -totalWidth) {
                    previewCursorX = 64f
                }
            } else {
                previewCursorX = ((64f - totalWidth) / 2f).coerceAtLeast(2f)
            }
            rainbowHue = (rainbowHue + 3f) % 360f
        }
    }

    // Nhấp nháy loop nếu mode = blink
    LaunchedEffect(uiState.scrollMode) {
        if (uiState.scrollMode == "blink") {
            while (isActive) {
                delay(350)
                blinkVisible = !blinkVisible
            }
        } else {
            blinkVisible = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bảng Chạy Chữ LED", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                        Text("Thông điệp & Cảnh báo xe hơi qua BLE", color = DivoomTextMuted, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            Spacer(modifier = Modifier.height(8.dp))

            // ── Live Animation Canvas Preview (Mô phỏng LED Matrix 64x32) ──
            Text("MÔ PHỎNG HIỂN THỊ THỰC TẾ (64×32 LED)", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF05070E))
                    .border(1.5.dp, DivoomGold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            ) {
                val cleanText = MarqueeViewModel.removeVietnameseTones(uiState.text.ifBlank { "CAR LED" })
                val fontSizePx = if (uiState.size == 2) 13f else 8.5f
                val fontY = if (uiState.size == 2) 20f else 18f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / 64f
                    val scaleY = size.height / 32f

                    if (blinkVisible) {
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = false
                                textSize = fontSizePx * scaleY
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            }

                            if (uiState.rainbow) {
                                var currX = previewCursorX * scaleX
                                for (i in cleanText.indices) {
                                    val ch = cleanText[i].toString()
                                    val charW = paint.measureText(ch)
                                    if (currX + charW > 0 && currX < size.width) {
                                        val hue = (rainbowHue + i * 25f) % 360f
                                        paint.color = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                                        canvas.nativeCanvas.drawText(
                                            ch,
                                            currX,
                                            fontY * scaleY,
                                            paint
                                        )
                                    }
                                    currX += charW
                                }
                            } else {
                                paint.color = android.graphics.Color.rgb(uiState.r, uiState.g, uiState.b)
                                canvas.nativeCanvas.drawText(
                                    cleanText,
                                    previewCursorX * scaleX,
                                    fontY * scaleY,
                                    paint
                                )
                            }
                        }
                    }

                    // Grid lines nhẹ
                    val gridCol = Color.White.copy(alpha = 0.04f)
                    for (x in 0..64 step 4) {
                        drawLine(gridCol, Offset(x * scaleX, 0f), Offset(x * scaleX, size.height), strokeWidth = 0.5f)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Mẫu câu thông dụng trên xe hơi (Car Presets) ──
            Text("MẪU CÂU THÔNG DỤNG TRÊN XE HƠI", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            val presets = listOf(
                "🚗 Cảm Ơn" to "XIN CAM ON!",
                "⚠️ Tập Lái" to "XE TAP LAI - XIN THONG CAM",
                "🛑 Khoảng Cách" to "GIU KHOANG CACH AN TOAN",
                "👋 Nhường Đường" to "XIN NHUONG DUONG",
                "📞 SĐT Liên Hệ" to "LH: 0988 123 456",
                "🎉 Bình An" to "CHUC VAN DAM BINH AN!"
            )

            // 2 hàng buttons
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { (label, txt) ->
                            Button(
                                onClick = { viewModel.applyPreset(txt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DivoomCard),
                                border = BorderStroke(1.dp, DivoomBorder),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Nội dung hiển thị (Text Input) ──
            Text("NỘI DUNG HIỂN THỊ", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = uiState.text,
                onValueChange = { viewModel.updateText(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập chữ chạy...", color = DivoomTextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DivoomGold,
                    unfocusedBorderColor = DivoomBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DivoomCard,
                    unfocusedContainerColor = DivoomCard
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Cỡ chữ & Kiểu hiệu ứng ──
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Cỡ chữ
                Column(modifier = Modifier.weight(1f)) {
                    Text("CỠ CHỮ", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "Nhỏ (5×7)", 2 to "Lớn (10×14)").forEach { (s, lbl) ->
                            val isSel = uiState.size == s
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DivoomGold else DivoomCard)
                                    .border(1.dp, if (isSel) DivoomGold else DivoomBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateSize(s) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lbl, color = if (isSel) DarkBackground else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Kiểu hiệu ứng
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("KIỂU HIỆU ỨNG", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("scroll_left" to "Cuộn", "static" to "Tĩnh", "blink" to "Chớp").forEach { (m, lbl) ->
                            val isSel = uiState.scrollMode == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) PrimaryCyan else DivoomCard)
                                    .border(1.dp, if (isSel) PrimaryCyan else DivoomBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateScrollMode(m) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lbl, color = if (isSel) DarkBackground else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Màu Sắc Chữ & Cầu Vồng ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MÀU SẮC CHỮ", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.rainbow,
                        onCheckedChange = { viewModel.toggleRainbow() },
                        colors = CheckboxDefaults.colors(checkedColor = DivoomGold)
                    )
                    Text("🌈 Chữ 7 màu (Rainbow)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!uiState.rainbow) {
                Spacer(modifier = Modifier.height(4.dp))
                val marqueePalette = listOf(
                    Triple(255, 215, 0) to "Vàng Gold",
                    Triple(255, 51, 102) to "Đỏ Neon",
                    Triple(0, 210, 255) to "Cyan Ice",
                    Triple(0, 240, 118) to "Xanh Lục",
                    Triple(255, 255, 255) to "Trắng",
                    Triple(153, 0, 255) to "Tím"
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    marqueePalette.forEach { (rgb, _) ->
                        val isSel = uiState.r == rgb.first && uiState.g == rgb.second && uiState.b == rgb.third
                        val col = Color(rgb.first, rgb.second, rgb.third)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(col)
                                .border(
                                    width = if (isSel) 2.5.dp else 1.dp,
                                    color = if (isSel) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.updateColor(rgb.first, rgb.second, rgb.third) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Tốc Độ Cuộn (Slider) ──
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TỐC ĐỘ CUỘN CHỮ", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${uiState.speed}", color = DivoomGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = uiState.speed.toFloat(),
                onValueChange = { viewModel.updateSpeed(it.toInt()) },
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(thumbColor = DivoomGold, activeTrackColor = DivoomGold)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Feedback Banner ──
            if (uiState.feedbackMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = uiState.feedbackMessage!!,
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── Nút Phát & Dừng Chữ (Send & Stop Buttons) ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.sendMarquee() },
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DivoomGold),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isSending
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DarkBackground, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🚀 Bắn Chữ Lên Bảng LED", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = { viewModel.stopMarquee() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DivoomCard),
                    border = BorderStroke(1.dp, DivoomBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = DivoomDanger)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tắt chữ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

