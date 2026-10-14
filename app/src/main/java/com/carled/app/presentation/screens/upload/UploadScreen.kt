package com.carled.app.presentation.screens.upload

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    viewModel: UploadViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Gallery launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(context, it) }
    }

    // Auto-clear feedback
    LaunchedEffect(uiState.feedbackMessage) {
        if (uiState.feedbackMessage != null && !uiState.isUploading) {
            kotlinx.coroutines.delay(6000)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nạp & Chiếu Ảnh Pixel Art", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                        Text("Ma trận 64×32 • Chuyển đổi RGB565 Little-Endian", color = DivoomTextMuted, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reloadProjection() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload từ Flash", tint = DivoomGold)
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

            // ── Chọn ảnh từ thiết bị ──
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan),
                border = BorderStroke(1.dp, PrimaryCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PrimaryCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.selectedImageUri != null) "📁 Đổi Ảnh Khác (JPG / PNG / GIF)" else "📁 Chọn Ảnh Từ Thiết Bị (JPG / PNG / GIF)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 64×32 Live Pixel Preview Canvas ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f) // 64:32 = 2:1
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.5.dp, DivoomBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = uiState.previewBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Preview ảnh LED 64×32",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    // Badge góc dưới
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("64 × 32 px • RGB565", color = DivoomGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = DivoomTextMuted, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Chưa có ảnh được chọn", color = DivoomTextMuted, fontSize = 13.sp)
                        Text("Hỗ trợ JPG, PNG, GIF, WebP", color = DivoomTextMuted.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Kiểu Khung Hình (FitMode: Cover | Contain | Stretch) ──
            Text("KIỂU KHUNG HÌNH (CROP / FIT)", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FitMode.values().forEach { mode ->
                    val isSelected = uiState.fitMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryCyan else DivoomCard)
                            .border(1.dp, if (isSelected) PrimaryCyan else DivoomBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setFitMode(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                FitMode.STRETCH -> "Kéo dãn"
                                FitMode.COVER   -> "Cắt vừa 2:1"
                                FitMode.CONTAIN -> "Giữ tỉ lệ"
                            },
                            color = if (isSelected) DarkBackground else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Thuật toán Dithering (Floyd-Steinberg) ──
            Text("THUẬT TOÁN DITHERING", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DitherMode.values().forEach { mode ->
                    val isSelected = uiState.ditherMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DivoomGold else DivoomCard)
                            .border(1.dp, if (isSelected) DivoomGold else DivoomBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setDitherMode(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                DitherMode.NONE            -> "Không Dither (Mịn)"
                                DitherMode.FLOYD_STEINBERG -> "Floyd-Steinberg (Pixel Art)"
                            },
                            color = if (isSelected) DarkBackground else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Độ Sáng Ảnh (Brightness: 40%..220%) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ĐỘ SÁNG ẢNH", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${uiState.brightnessPercent}%", color = DivoomGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = uiState.brightnessPercent.toFloat(),
                onValueChange = { viewModel.setBrightness(it.toInt()) },
                valueRange = 40f..220f,
                colors = SliderDefaults.colors(thumbColor = DivoomGold, activeTrackColor = DivoomGold)
            )

            // ── Độ Tương Phản (Contrast: 40%..220%) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ĐỘ TƯƠNG PHẢN", color = DivoomTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${uiState.contrastPercent}%", color = PrimaryCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = uiState.contrastPercent.toFloat(),
                onValueChange = { viewModel.setContrast(it.toInt()) },
                valueRange = 40f..220f,
                colors = SliderDefaults.colors(thumbColor = PrimaryCyan, activeTrackColor = PrimaryCyan)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Feedback Toast / Telemetry Status ──
            if (uiState.feedbackMessage != null) {
                val isSuccess = uiState.uploadStatus == UploadStatus.Complete
                val isErr = uiState.uploadStatus == UploadStatus.Error
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isSuccess -> NeonGreen.copy(alpha = 0.15f)
                            isErr     -> DivoomDanger.copy(alpha = 0.15f)
                            else      -> PrimaryCyan.copy(alpha = 0.15f)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = uiState.feedbackMessage!!,
                        color = when {
                            isSuccess -> NeonGreen
                            isErr     -> DivoomDanger
                            else      -> PrimaryCyan
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── Thanh tiến trình nạp (Progress bar) ──
            if (uiState.isUploading) {
                LinearProgressIndicator(
                    progress = { uiState.uploadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = DivoomGold,
                    trackColor = DivoomCard
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Nút Chiếu Lên LED Qua Bluetooth BLE ──
            Button(
                onClick = { viewModel.startUpload() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = uiState.previewBitmap != null && !uiState.isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = DivoomGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang nạp ảnh lên xe...", color = DarkBackground, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🚀 Chiếu Lên LED Qua Bluetooth BLE", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

