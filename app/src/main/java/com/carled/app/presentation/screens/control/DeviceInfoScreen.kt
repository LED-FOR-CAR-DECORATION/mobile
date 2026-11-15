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
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.MockDeviceInventory
import com.carled.app.domain.model.isValidDeviceName
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun DeviceInfoScreen(
    vm: ControlViewModel,
    ownedDevices: OwnedDevicesViewModel,
    panelId: String?,
    back: () -> Unit,
    openPanelControl: (String) -> Unit,
    connectDevice: (macAddress: String) -> Unit
) {
    LaunchedEffect(panelId) {
        ownedDevices.loadDeviceDetail(panelId)
    }

    val connectionState by vm.connectionState.collectAsState()
    val devicesState by ownedDevices.uiState.collectAsState()
    val detailState by ownedDevices.detailState.collectAsState()

    val cachedDevice = remember(devicesState, panelId) { ownedDevices.deviceById(panelId) }
    val detailDto = (detailState as? DeviceDetailUiState.Loaded)?.detail

    // Prefer fresh/cached detail DTO fields if available, fallback to cached list item
    val displayName = detailDto?.deviceName ?: cachedDevice?.deviceName
    val displayMac = detailDto?.macAddress ?: cachedDevice?.macAddress
    val displaySerial = detailDto?.serialNumber ?: cachedDevice?.serialNumber
    val displayFirmware = detailDto?.currentFirmwareId?.toString() ?: cachedDevice?.firmwareVersion
    val displayLastUpdated = detailDto?.boundAt ?: cachedDevice?.lastUpdated
    val deviceId = detailDto?.deviceId?.toString() ?: cachedDevice?.id ?: panelId.orEmpty()

    val hasDevice = displayName != null && displayMac != null
    val connectedDevice = connectionState as? ConnectionState.Connected
    val connectedToThis = hasDevice && connectedDevice?.deviceAddress == displayMac

    val firmware = remember { MockDeviceInventory.firmwareReleases() }

    var rename by remember { mutableStateOf(false) }
    var draftName by remember(displayName) { mutableStateOf(displayName.orEmpty()) }
    var renaming by remember { mutableStateOf(false) }
    var renameError by remember { mutableStateOf<String?>(null) }
    val renameScope = rememberCoroutineScope()

    Scaffold(topBar = { MirageBar("Thông tin thiết bị", back) }, containerColor = MirageBackground) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!hasDevice && (devicesState is OwnedDevicesUiState.Loading || detailState is DeviceDetailUiState.Loading)) {
                Text("Đang tải thông tin thiết bị…", color = MirageMuted)
            } else if (!hasDevice) {
                Text("Không tìm thấy thiết bị này.", color = MaterialTheme.colorScheme.error)
            } else {
                DeviceSection(title = "Device info") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream)
                            .clickable { draftName = displayName.orEmpty(); rename = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(displayName.orEmpty(), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Mac Address: $displayMac", color = MirageMuted, fontSize = 12.sp)
                            if (!displaySerial.isNullOrBlank()) {
                                Text("Serial: $displaySerial", color = MirageMuted, fontSize = 12.sp)
                            }
                            Text("Firmware version: ${displayFirmware ?: "—"}", color = MirageMuted, fontSize = 12.sp)
                            Text("Last updated: ${displayLastUpdated ?: "—"}", color = MirageMuted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        ChevronGlyph()
                    }
                }

                DeviceSection(title = "Connection") {
                    if (connectedToThis) {
                        Text("● Đã kết nối Bluetooth", color = MirageMuted)
                        MirageButton("Mở bảng điều khiển", { openPanelControl(deviceId) }, Modifier.fillMaxWidth())
                    } else {
                        Text(
                            if (connectionState is ConnectionState.Connecting) "Đang kết nối…" else "Chưa kết nối thiết bị này",
                            color = MirageMuted
                        )
                        MirageButton(
                            "Connect to device",
                            { connectDevice(displayMac.orEmpty()) },
                            Modifier.fillMaxWidth(),
                            enabled = connectionState !is ConnectionState.Connecting
                        )
                    }
                }
            }

            DeviceSection(title = "Firmware versions") {
                firmware.forEach { release -> FirmwareReleaseCard(release) }
            }
        }
    }

    if (rename && hasDevice) AlertDialog(
        onDismissRequest = { if (!renaming) rename = false },
        title = { Text("Tên Bluetooth của LED") },
        text = {
            Column {
                OutlinedTextField(
                    draftName, { draftName = it; renameError = null }, enabled = !renaming, singleLine = true,
                    label = { Text("Tên thiết bị") },
                    supportingText = { Text("Tối đa 29 byte UTF-8 · Lưu trực tiếp trên LED") }
                )
                renameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !renaming && connectedToThis && isValidDeviceName(draftName),
                onClick = {
                    renaming = true
                    renameScope.launch {
                        try {
                            vm.renameDevice(draftName)
                            rename = false
                        } catch (e: CancellationException) {
                            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                                renameError = "LED chưa xác nhận. Kiểm tra kết nối và cập nhật firmware LED."
                            } else throw e
                        } catch (e: Exception) {
                            renameError = e.message ?: "Không đổi được tên"
                        } finally {
                            renaming = false
                        }
                    }
                }
            ) { Text(if (renaming) "Đang lưu…" else "Lưu trên LED") }
        },
        dismissButton = { TextButton(enabled = !renaming, onClick = { rename = false }) { Text("Hủy") } }
    )
}