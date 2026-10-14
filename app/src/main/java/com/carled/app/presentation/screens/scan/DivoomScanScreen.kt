package com.carled.app.presentation.screens.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.presentation.theme.*

data class DeviceMockModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val iconColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivoomScanScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Tự động quét khi mở màn hình
    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScan()
        }
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Bluetooth, 1: WiFi
    var showManualAddDialog by remember { mutableStateOf(false) }
    var manualCodeInput by remember { mutableStateOf("") }

    val hardwareModels = listOf(
        DeviceMockModel("hud64", "CarLED HUD", "Màn hình kính lái 64x32", DivoomGold),
        DeviceMockModel("ambient", "Ambient Strip", "Dải LED nội thất WS2812B", PrimaryCyan),
        DeviceMockModel("matrix", "Timebox Matrix", "Bảng LED cảnh báo sau xe", AccentPurple)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Segmented Button: Bluetooth | WiFi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DivoomCard)
                                .border(1.dp, DivoomBorder, RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedTab == 0) DivoomGold else Color.Transparent)
                                    .clickable { selectedTab = 0 }
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Bluetooth",
                                    color = if (selectedTab == 0) DarkBackground else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedTab == 1) DivoomGold else Color.Transparent)
                                    .clickable { selectedTab = 1 }
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "WiFi",
                                    color = if (selectedTab == 1) DarkBackground else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showManualAddDialog = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = DivoomGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Hiển thị thông báo lỗi nếu Bluetooth tắt hoặc chưa cấp quyền
                if (uiState.errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = DivoomDanger.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = DivoomDanger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // 1. Carousel Dòng thiết bị phần cứng (Ảnh 2 Divoom)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(hardwareModels) { item ->
                        Card(
                            modifier = Modifier
                                .width(130.dp)
                                .height(110.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DivoomCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DivoomCardSecondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenshotMonitor,
                                        contentDescription = null,
                                        tint = item.iconColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(item.subtitle, color = DivoomTextMuted, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Tiêu đề Thiết bị của tôi & Thiết bị khả dụng
                Text(
                    "Thiết bị của tôi",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Box thiết bị hiện tại đã kết nối
                when (val conn = uiState.connectionState) {
                    is ConnectionState.Connected -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DivoomCard)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(conn.deviceName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(conn.deviceAddress, color = DivoomTextMuted, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onNavigateBack,
                                    colors = ButtonDefaults.buttonColors(containerColor = DivoomGold),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Điều khiển", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.disconnectDevice() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DivoomDanger.copy(alpha = 0.2f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Ngắt", color = DivoomDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    is ConnectionState.Connecting -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DivoomCard)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = DivoomGold,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Đang kết nối ESP32 LED...", color = Color.White, fontSize = 13.sp)
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DivoomCard)
                                .padding(14.dp)
                        ) {
                            Text("Chưa có thiết bị nào được lưu vào danh sách của bạn", color = DivoomTextMuted, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Thiết bị khả dụng (${uiState.devices.size})",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        if (uiState.isScanning) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = DivoomGold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (uiState.isScanning) viewModel.stopScan() else viewModel.startScan()
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                            contentDescription = "Scan",
                            tint = DivoomGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Danh sách thiết bị quét được
                if (uiState.devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                tint = DivoomTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (uiState.isScanning) "Đang tìm thiết bị CarLED trong phạm vi xe..."
                                else "Nhấn nút quét hoặc nổ máy xe để tìm ESP32",
                                color = DivoomTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.devices) { device ->
                            val isCarLedDevice = device.name.contains("CarLED", ignoreCase = true) || device.name.contains("ESP32", ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DivoomCard)
                                    .clickable { viewModel.connectDevice(device.macAddress) }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = if (isCarLedDevice) DivoomGold else PrimaryCyan
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(device.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${device.macAddress} • ${device.rssi} dBm", color = DivoomTextMuted, fontSize = 11.sp)
                                    }
                                }
                                Button(
                                    onClick = { viewModel.connectDevice(device.macAddress) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isCarLedDevice) DivoomGold else DivoomCardSecondary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Ghép nối",
                                        color = if (isCarLedDevice) DarkBackground else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Modal tìm kiếm Bluetooth (Spinner) khi đang quét VÀ chưa tìm thấy thiết bị nào (Ảnh 2 Divoom)
            if (uiState.isScanning && uiState.devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.width(220.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF202438))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Tìm kiếm Bluetooth",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(20.dp))
                            TextButton(onClick = { viewModel.stopScan() }) {
                                Text("Hủy bỏ", color = DivoomGold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }


            // 4. Modal "Thêm thiết bị của bạn" (Ảnh 3 Divoom)
            if (showManualAddDialog) {
                AlertDialog(
                    onDismissRequest = { showManualAddDialog = false },
                    containerColor = Color(0xFFF5F5F7), // Dialog trắng giống ảnh 3 Divoom
                    title = {
                        Text(
                            "Thêm thiết bị của bạn",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Nhập mã số MAC bên dưới hoặc quét mã QR chia sẻ trên vỏ hộp thiết bị.",
                                color = Color(0xFF666666),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = manualCodeInput,
                                onValueChange = { manualCodeInput = it },
                                placeholder = { Text("Nhập MAC (vd: AA:BB:CC:11:22:33)", color = Color.Gray, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DivoomGold,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    // Mở tính năng quét QR
                                    showManualAddDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chạm để quét QR", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (manualCodeInput.isNotBlank()) {
                                    viewModel.connectDevice(manualCodeInput.trim())
                                }
                                showManualAddDialog = false
                            }
                        ) {
                            Text("Xác nhận", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showManualAddDialog = false }) {
                            Text("Hủy bỏ", color = Color(0xFF888888))
                        }
                    }
                )
            }
        }
    }
}
