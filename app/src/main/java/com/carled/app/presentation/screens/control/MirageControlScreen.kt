package com.carled.app.presentation.screens.control

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.*

@Composable
fun MirageControlScreen(
    vm: ControlViewModel,
    ownedDevices: OwnedDevicesViewModel,
    back: () -> Unit,
    scanner: () -> Unit,
    openClaimDevice: () -> Unit = {},
    openDeviceInfo: (String) -> Unit = {}
) {
    val feedbackMessage by vm.feedbackMessage.collectAsState()
    val devicesState by ownedDevices.uiState.collectAsState()
    var help by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
            .padding(8.dp)
    ) {
        WindowFrame(
            title = "Thiết bị của tôi",
            onClose = back,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DeviceSection(title = "Owned devices", onAdd = openClaimDevice) {
                    when (val state = devicesState) {
                        is OwnedDevicesUiState.Loading -> EmptyRow("Đang tải danh sách thiết bị…")
                        is OwnedDevicesUiState.Error -> EmptyRow(state.message)
                        is OwnedDevicesUiState.Loaded -> {
                            if (state.devices.isEmpty()) {
                                EmptyRow("Chưa có thiết bị nào. Nhấn + để quét mã QR và thêm thiết bị.")
                            } else {
                                state.devices.forEach { device ->
                                    OwnedDeviceRow(
                                        label = device.deviceName,
                                        serialNumber = device.serialNumber ?: "—",
                                        onOpenInfo = { openDeviceInfo(device.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                GuideRow(text = "Hướng dẫn Bluetooth", onClick = { help = true })

                feedbackMessage?.let { Text(it, color = MirageMuted, fontSize = 13.sp) }
            }
        }

        if (help) AlertDialog(
            onDismissRequest = { help = false },
            title = { Text("Kết nối Bluetooth") },
            text = { Text("Bật nguồn LED và Bluetooth trên điện thoại. Android 12 trở lên cần quyền Thiết bị ở gần; Android cũ hơn cần quyền Vị trí để quét BLE. Đặt điện thoại gần LED rồi chọn Thêm thiết bị. Điều khiển BLE không cần Internet.") },
            confirmButton = { TextButton(onClick = { help = false }) { Text("Đã hiểu") } }
        )
    }
}


@Composable
private fun OwnedDeviceRow(
    label: String,
    serialNumber: String,
    onOpenInfo: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .flatSurface(color = Cream)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Serial: $serialNumber", color = MirageMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))

        // Slimmer height using reduced vertical padding (4.dp)
        Box(
            Modifier
                .hardSurface(color = GreenLight, offset = 4.dp, borderWidth = 2.dp)
                .clickable(onClick = onOpenInfo)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Device info",
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}