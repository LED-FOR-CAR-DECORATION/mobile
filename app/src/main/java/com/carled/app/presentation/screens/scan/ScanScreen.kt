package com.carled.app.presentation.screens.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.presentation.components.StatusBadge
import com.carled.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onNavigateToControl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Khi đã kết nối thành công, có thể chuyển sang màn hình điều khiển
    if (uiState.connectionState is ConnectionState.Connected) {
        // Có thể gọi onNavigateToControl()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tìm Thiết Bị Car LED", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                actions = {
                    IconButton(
                        onClick = {
                            if (uiState.isScanning) viewModel.stopScan() else viewModel.startScan()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan",
                            tint = PrimaryCyan
                        )
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Thanh trạng thái hiện tại
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Trạng thái Bluetooth", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        when (val s = uiState.connectionState) {
                            is ConnectionState.Connected -> "Kết nối: ${s.deviceName}"
                            is ConnectionState.Connecting -> "Đang ghép nối ESP32..."
                            is ConnectionState.Scanning -> "Đang dò tìm CarLED-Ambient..."
                            is ConnectionState.Disconnected -> "Chưa kết nối thiết bị"
                            is ConnectionState.Error -> s.message
                        },
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                StatusBadge(state = uiState.connectionState)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "THIẾT BỊ PHÁT HIỆN (${uiState.devices.size})",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.isScanning) "Đang tìm thiết bị Car LED xung quanh xe..."
                            else "Bấm nút quét để tìm đèn Car LED",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.devices) { device ->
                        DeviceItem(
                            device = device,
                            onClick = {
                                viewModel.connectDevice(device.macAddress)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNavigateToControl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Vào Bảng Điều Khiển Đèn", color = DarkBackground, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DeviceItem(
    device: BleDeviceModel,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(device.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(device.macAddress, color = TextSecondary, fontSize = 12.sp)
        }
        Text("RSSI: ${device.rssi} dBm", color = PrimaryCyan, fontSize = 12.sp)
    }
}
