package com.carled.app.presentation.screens.control

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*

@Composable
fun MirageControlScreen(
    vm: ControlViewModel,
    back: () -> Unit,
    scanner: () -> Unit,
    openPanelControl: (String) -> Unit = {},
    userId: String = ""
) {
    val state by vm.uiState.collectAsState()
    val device = state.connectionState as? ConnectionState.Connected
    val firmware = remember { MockDeviceInventory.firmwareReleases() }
    var help by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        WindowFrame(
            title = "Thiết bị của tôi",
            onClose = back,
            modifier = Modifier.fillMaxSize()
        ) {
            DeviceSection(title = "Owned device:", onAdd = scanner) {
                if (device == null) {
                    EmptyRow(
                        if (state.connectionState is ConnectionState.Connecting) "Đang kết nối…"
                        else "Chưa có thiết bị kết nối. Nhấn + để thêm."
                    )
                } else {
                    ConnectedPanelCard(
                        name = device.deviceName,
                        powerOn = state.ledState.isPowerOn,
                        mode = state.ledState.currentMode.displayName,
                        address = device.deviceAddress,
                        onOpenControl = { openPanelControl("current") }
                    )
                }
            }

            DeviceSection(title = "Firmware manager:") {
                firmware.forEach { release -> FirmwareReleaseCard(release) }
            }

            GuideRow(text = "Hướng dẫn Bluetooth", onClick = { help = true })

            state.feedbackMessage?.let { Text(it, color = MirageMuted, fontSize = 13.sp) }
        }

        if (help) AlertDialog(
            onDismissRequest = { help = false },
            title = { Text("Kết nối Bluetooth") },
            text = { Text("Bật nguồn LED và Bluetooth trên điện thoại. Android 12 trở lên cần quyền Thiết bị ở gần; Android cũ hơn cần quyền Vị trí để quét BLE. Đặt điện thoại gần LED rồi chọn Thêm thiết bị. Điều khiển BLE không cần Internet.") },
            confirmButton = { TextButton(onClick = { help = false }) { Text("Đã hiểu") } }
        )
    }
}