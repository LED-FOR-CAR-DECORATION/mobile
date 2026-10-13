package com.carled.app.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.theme.*

data class DivoomFeatureItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit
)

@Composable
fun DivoomDashboardScreen(
    viewModel: ControlViewModel,
    connectionState: ConnectionState,
    onOpenScanner: () -> Unit,
    onOpenCanvas: () -> Unit,
    onOpenMarquee: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenUpload: () -> Unit,
    onOpenStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val led = uiState.ledState

    var showColorPickerModal by remember { mutableStateOf(false) }
    var showSensitivityModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Top Banner Carousel (Quảng cáo phong cách Divoom)
        TopBannerCarousel(onBannerClick = onOpenStore)

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Section "Thiết bị của tôi"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thiết bị của tôi",
                color = DivoomTextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            // Trạng thái kết nối
            val isConnected = connectionState is ConnectionState.Connected
            Text(
                text = if (isConnected) "● Đã kết nối" else "○ Chưa kết nối",
                color = if (isConnected) NeonGreen else DivoomTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Card Thiết bị chính với Thanh trượt độ sáng 2 mặt trời chuẩn Divoom
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DivoomCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenScanner() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DivoomCardSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = if (led.isPowerOn) DivoomGold else DivoomTextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (connectionState is ConnectionState.Connected) "CarLED-Ambient (Kính lái)" else "Thêm thiết bị",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (led.isPowerOn) "Đang hoạt động • ${led.currentMode.displayName}" else "Thiết bị đang tắt",
                                fontSize = 12.sp,
                                color = DivoomTextMuted
                            )
                        }
                    }

                    // Công tắc Nguồn (Power)
                    IconButton(
                        onClick = { viewModel.togglePower() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (led.isPowerOn) NeonGreen.copy(alpha = 0.2f) else DivoomCardSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (led.isPowerOn) NeonGreen else DivoomTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Thanh trượt độ sáng kẹp 2 icon mặt trời
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Min Brightness",
                        tint = DivoomTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = led.brightness.toFloat(),
                        onValueChange = { viewModel.setBrightness(it.toInt()) },
                        valueRange = 0f..255f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = DivoomBorder
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Brightness7,
                        contentDescription = "Max Brightness",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Hai nút phụ: Trung tâm hỗ trợ & Cài đặt thiết bị
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenScanner,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DivoomCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothSearching,
                    contentDescription = null,
                    tint = DivoomTextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tìm & Ghép Bluetooth", color = Color.White, fontSize = 12.sp)
            }

            Button(
                onClick = { showSensitivityModal = true },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DivoomCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = DivoomTextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cài đặt thiết bị", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Lưới 4x3 các chức năng của thiết bị (Đặc trưng của Divoom)
        Text(
            text = "Các chức năng của thiết bị",
            color = DivoomTextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(12.dp))

        val features = listOf(
            DivoomFeatureItem("paint", "Thiết kế", Icons.Default.Palette, Color(0xFFFF5252), onOpenCanvas),
            DivoomFeatureItem("anim", "Hoạt hình", Icons.Default.VideoLibrary, Color(0xFFFFA726), onOpenGallery),
            DivoomFeatureItem("marquee", "Bộ sửa LED", Icons.Default.TextFields, Color(0xFF00E676), onOpenMarquee),
            DivoomFeatureItem("color", "Màu sắc", Icons.Default.ColorLens, Color(0xFFE040FB)) { showColorPickerModal = true },
            DivoomFeatureItem("music", "Âm nhạc", Icons.Default.MusicNote, Color(0xFFFF4081)) { viewModel.setMode(LedMode.MUSIC_REACTIVE) },
            DivoomFeatureItem("rainbow", "Cầu vồng", Icons.Default.GraphicEq, Color(0xFF40C4FF)) { viewModel.setMode(LedMode.RAINBOW_WAVE) },
            DivoomFeatureItem("mic", "Độ nhạy Mic", Icons.Default.Mic, Color(0xFFFFD740)) { showSensitivityModal = true },
            DivoomFeatureItem("upload", "Nạp ảnh SPIFFS", Icons.Default.CloudUpload, Color(0xFF69F0AE), onOpenUpload),
            DivoomFeatureItem("breathe", "Nhịp thở", Icons.Default.Favorite, Color(0xFFFF6E40)) { viewModel.setMode(LedMode.BREATHING) },
            DivoomFeatureItem("chase", "Dòng chảy", Icons.Default.AutoMode, Color(0xFF7C4DFF)) { viewModel.setMode(LedMode.CHASE_FLOW) },
            DivoomFeatureItem("sleep", "Chế độ ngủ", Icons.Default.Bedtime, Color(0xFF00B0FF)) { viewModel.setMode(LedMode.OFF) },
            DivoomFeatureItem("store", "Divoom eStore", Icons.Default.ShoppingBag, Color(0xFFFFA000), onOpenStore)
        )

        // Render 12 features trong grid
        val rows = features.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowItems.forEach { item ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { item.onClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(DivoomCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.iconColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Modal chọn màu nhanh
    if (showColorPickerModal) {
        QuickColorDialog(
            currentColor = led.currentColor,
            onColorSelect = {
                viewModel.setColor(it)
                showColorPickerModal = false
            },
            onDismiss = { showColorPickerModal = false }
        )
    }

    // Modal chỉnh độ nhạy micro bắt nhạc
    if (showSensitivityModal) {
        SensitivityDialog(
            currentSensitivity = led.sensitivity,
            currentSpeed = led.speed,
            onSave = { sens, spd ->
                viewModel.setSpeed(spd)
                // Note: sensitivity can be set via bleUseCases or viewModel
                showSensitivityModal = false
            },
            onDismiss = { showSensitivityModal = false }
        )
    }
}

@Composable
private fun TopBannerCarousel(onBannerClick: () -> Unit) {
    val banners = listOf(
        Pair("Cyberpunk Neon HUD", "Hiệu ứng nhân vật chiếu kính lái cực chất"),
        Pair("Super Deal Days", "Ưu đãi phụ kiện đèn ô tô giảm tới 30%"),
        Pair("Music Reactive Pack", "Bộ hiệu ứng nhảy theo bass loa sub ô tô")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(banners) { (title, sub) ->
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(95.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2A1650), Color(0xFF14244D))
                        )
                    )
                    .clickable { onBannerClick() }
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        color = DivoomGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sub,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickColorDialog(
    currentColor: RgbColor,
    onColorSelect: (RgbColor) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(
        RgbColor(255, 0, 0),      // Red
        RgbColor(0, 255, 0),      // Green
        RgbColor(0, 200, 255),    // Cyan
        RgbColor(255, 165, 0),    // Orange
        RgbColor(168, 85, 247),   // Purple
        RgbColor(255, 0, 128),    // Pink
        RgbColor(255, 255, 0),    // Yellow
        RgbColor(255, 255, 255)   // White
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DivoomCard,
        title = {
            Text("Chọn Màu Ánh Sáng Xe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text("Chọn màu hiển thị trực tiếp lên đèn:", color = DivoomTextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presets.chunked(4).forEach { colPresets ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            colPresets.forEach { color ->
                                val composeColor = Color(color.red, color.green, color.blue)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { onColorSelect(color) }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(composeColor)
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(color.toHex(), color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = DivoomGold)
            }
        }
    )
}

@Composable
private fun SensitivityDialog(
    currentSensitivity: Int,
    currentSpeed: Int,
    onSave: (sensitivity: Int, speed: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sens by remember { mutableStateOf(currentSensitivity.toFloat()) }
    var spd by remember { mutableStateOf(currentSpeed.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DivoomCard,
        title = {
            Text("Cài Đặt Cảm Biến & Tốc Độ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text("Độ nhạy Mic xe (Lọc tạp âm động cơ): ${sens.toInt()}%", color = Color.White, fontSize = 13.sp)
                Slider(
                    value = sens,
                    onValueChange = { sens = it },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(thumbColor = DivoomGold, activeTrackColor = DivoomGold)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Tốc độ chuyển động hiệu ứng: ${spd.toInt()}%", color = Color.White, fontSize = 13.sp)
                Slider(
                    value = spd,
                    onValueChange = { spd = it },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryCyan, activeTrackColor = PrimaryCyan)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(sens.toInt(), spd.toInt()) },
                colors = ButtonDefaults.buttonColors(containerColor = DivoomGold)
            ) {
                Text("Lưu cài đặt", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = DivoomTextMuted)
            }
        }
    )
}
