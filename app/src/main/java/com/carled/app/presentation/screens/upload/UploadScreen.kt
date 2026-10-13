package com.carled.app.presentation.screens.upload

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
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
            kotlinx.coroutines.delay(5000)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Upload Ảnh LED", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Nạp ảnh lên Flash SPIFFS", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reloadProjection() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload từ Flash", tint = PrimaryCyan)
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Telemetry Event Status Card ──
            val (eventColor, eventBg) = when (uiState.uploadStatus) {
                UploadStatus.Complete  -> NeonGreen to NeonGreen.copy(alpha = 0.12f)
                UploadStatus.Error     -> ErrorRed to ErrorRed.copy(alpha = 0.12f)
                UploadStatus.Starting  -> PrimaryCyan to PrimaryCyan.copy(alpha = 0.12f)
                UploadStatus.Uploading -> AccentPurple to AccentPurple.copy(alpha = 0.12f)
                UploadStatus.Idle      -> TextSecondary to SurfaceDark
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(eventBg)
                    .border(1.dp, eventColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(eventColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (uiState.uploadStatus) {
                                UploadStatus.Idle      -> "Chờ chọn ảnh để upload"
                                UploadStatus.Starting  -> "ESP32 đang chuẩn bị nhận..."
                                UploadStatus.Uploading -> "Đang truyền dữ liệu BLE..."
                                UploadStatus.Complete  -> "Upload hoàn tất thành công ✓"
                                UploadStatus.Error     -> "Upload thất bại"
                            },
                            color = eventColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    if (uiState.feedbackMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(uiState.feedbackMessage!!, color = eventColor.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Progress Bar (khi đang upload) ──
            if (uiState.isUploading || uiState.uploadStatus == UploadStatus.Starting) {
                Text("TIẾN TRÌNH UPLOAD", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { uiState.uploadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryCyan,
                    trackColor = SurfaceLightDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${(uiState.uploadProgress * 100).toInt()}% • ${uiState.uploadExpectedBytes} bytes",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Image Preview ──
            Text("ẢNH PREVIEW (64×32 LED)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f) // 64:32 = 2:1
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, SurfaceLightDark, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = uiState.previewBitmap as? Bitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Preview ảnh LED",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    // Overlay thông tin
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("64 × 32 px", color = Color.White, fontSize = 10.sp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Chưa có ảnh được chọn", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Specs card ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📐 Yêu cầu ảnh:", color = PrimaryCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("• Kích thước: 64 × 32 pixel (tự động scale)", color = TextSecondary, fontSize = 12.sp)
                    Text("• Định dạng xuất: RGB565 = 4096 bytes", color = TextSecondary, fontSize = 12.sp)
                    Text("• Hỗ trợ: JPG, PNG, WebP", color = TextSecondary, fontSize = 12.sp)
                    Text("• Thời gian upload: ~5 giây qua BLE", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Buttons ──
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan),
                border = BorderStroke(1.dp, PrimaryCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chọn Ảnh từ Thư viện", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.startUpload() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = uiState.selectedImageUri != null && !uiState.isUploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    disabledContainerColor = SurfaceLightDark
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang upload...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload lên ESP32", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
