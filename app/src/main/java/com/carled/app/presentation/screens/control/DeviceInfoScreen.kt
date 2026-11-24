package com.carled.app.presentation.screens.control

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.carled.app.data.ble.bleScanPermissions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.core.di.ServiceLocator
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.carled.app.domain.model.device.isValidDeviceName
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
    connectDevice: (macAddress: String) -> Unit,
    disconnectDevice: () -> Unit
) {
    val connectionState by vm.connectionState.collectAsState()
    val devicesState by ownedDevices.uiState.collectAsState()
    val listDevice = remember(devicesState, panelId) { ownedDevices.deviceById(panelId) }

    val deviceIdLong = remember(panelId) { panelId?.toLongOrNull() }
    LaunchedEffect(deviceIdLong) {
        if (deviceIdLong != null) ownedDevices.loadDeviceDetail(deviceIdLong)
    }
    val detailStates by ownedDevices.detailState.collectAsState()
    val detailState = deviceIdLong?.let { detailStates[it] }
    val detail = (detailState as? DeviceDetailUiState.Loaded)?.detail

    var confirmedName by remember(panelId, detail?.deviceName, listDevice?.deviceName) {
        mutableStateOf<String?>(null)
    }
    val deviceName = confirmedName ?: detail?.deviceName ?: listDevice?.deviceName
    val macAddress = detail?.macAddress ?: listDevice?.macAddress
    val serialNumber = detail?.serialNumber ?: listDevice?.serialNumber
    val boundAt = detail?.boundAt ?: listDevice?.lastUpdated
    val modelName = detail?.model?.modelName
    val firmwareId = detail?.currentFirmwareId ?: listDevice?.firmwareVersion?.toLongOrNull()
    val warrantyStatus = detail?.warrantyStatus
    val warrantyExpiresAt = detail?.warrantyExpiresAt

    val connectedDevice = connectionState as? ConnectionState.Connected
    val connectedToThis = macAddress != null && connectedDevice?.deviceAddress == macAddress

    val firmwareVm = if (deviceIdLong != null && macAddress != null) {
        viewModel<FirmwareUpdateViewModel>(
            key = "firmware-$deviceIdLong-$macAddress",
            factory = viewModelFactory {
                initializer {
                    FirmwareUpdateViewModel(deviceIdLong, macAddress, ServiceLocator.firmwareRepository, ServiceLocator.firmwareBleClient)
                }
            }
        )
    } else null
    val firmwareState = firmwareVm?.state?.collectAsState()?.value
    val firmwareActive = firmwareState?.working == true
    LaunchedEffect(firmwareState?.completed) {
        if (firmwareState?.completed == true && deviceIdLong != null) {
            ownedDevices.loadDeviceDetail(deviceIdLong)
            ownedDevices.refresh()
        }
    }

    var attemptedMac by remember { mutableStateOf<String?>(null) }
    val connectingToThis = connectionState is ConnectionState.Connecting && attemptedMac == macAddress
    LaunchedEffect(connectionState) {
        if (connectionState !is ConnectionState.Connecting) attemptedMac = null
    }

    // Android 12+ needs BLUETOOTH_CONNECT/SCAN granted at runtime; without them
    // connect() bails out with an Error state, so ask before dialing the MAC.
    val context = LocalContext.current
    val blePermissions = remember { bleScanPermissions() }
    var connectError by remember { mutableStateOf<String?>(null) }
    fun startConnect() {
        connectError = null
        attemptedMac = macAddress
        macAddress?.let(connectDevice)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) startConnect()
        else connectError = "Cần cấp quyền Thiết bị ở gần (Bluetooth) để kết nối với LED."
    }
    LaunchedEffect(connectionState) {
        (connectionState as? ConnectionState.Error)?.let { connectError = it.message }
    }

    var rename by remember { mutableStateOf(false) }
    var draftName by remember(deviceName) { mutableStateOf(deviceName.orEmpty()) }
    var renaming by remember { mutableStateOf(false) }
    var renameError by remember { mutableStateOf<String?>(null) }
    val renameScope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
            .padding(8.dp)
    ) {
        WindowFrame(title = "Thông tin thiết bị", onClose = back, modifier = Modifier.fillMaxSize()) {
            if (deviceName == null) {
                Text(
                    if (detailState is DeviceDetailUiState.Error) detailState.message else "Đang tải thông tin thiết bị…",
                    color = if (detailState is DeviceDetailUiState.Error) MaterialTheme.colorScheme.error else MirageMuted
                )
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connection", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.flatSurface(color = Cream).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Trạng thái kết nối:", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Box(
                            Modifier
                                .flatSurface(color = if (connectedToThis) GreenLight else Cream)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                when {
                                    connectedToThis -> "Đã kết nối"
                                    connectingToThis -> "Đang kết nối…"
                                    else -> "Chưa kết nối"
                                },
                                color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    when {
                        connectedToThis -> HardButton(
                            text = "Ngắt kết nối",
                            enabled = !firmwareActive,
                            onClick = disconnectDevice,
                            filled = false,
                            shadowOffset = HardStyle.CardOffset,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                        connectingToThis -> HardButton(
                            text = "Đang kết nối…",
                            onClick = {},
                            enabled = false,
                            shadowOffset = HardStyle.CardOffset,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                        else -> HardButton(
                            text = "Connect to device",
                            onClick = {
                                val granted = blePermissions.all {
                                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                }
                                if (granted) startConnect() else permissionLauncher.launch(blePermissions)
                            },
                            enabled = macAddress != null,
                            shadowOffset = HardStyle.CardOffset,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                    }

                    if (!connectedToThis && !connectingToThis) {
                        connectError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    }

                    if (connectedToThis) {
                        HardButton(
                            text = "Mở bảng điều khiển",
                            onClick = { openPanelControl(panelId.orEmpty()) },
                            filled = false,
                            shadowOffset = HardStyle.CardOffset,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }

                DeviceSection(title = "Firmware") {
                    firmwareVm?.let { FirmwareUpdateSection(it, connectedToThis) }
                }

                DeviceSection(title = "Device info") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream)
                            .clickable { draftName = deviceName; rename = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(deviceName, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            modelName?.let { Text("Model: $it", color = MirageMuted, fontSize = 12.sp) }
                            Text("Mac Address: ${macAddress ?: "—"}", color = MirageMuted, fontSize = 12.sp)
                            Text("Serial number: ${serialNumber ?: "—"}", color = MirageMuted, fontSize = 12.sp)
                            Text("Firmware ID: ${firmwareId ?: "—"}", color = MirageMuted, fontSize = 12.sp)
                            Text("Bound at: ${boundAt ?: "—"}", color = MirageMuted, fontSize = 12.sp)
                            if (warrantyStatus != null) {
                                Text(
                                    "Bảo hành: $warrantyStatus" + (warrantyExpiresAt?.let { " · hết hạn $it" } ?: ""),
                                    color = MirageMuted, fontSize = 12.sp
                                )
                            }
                            if (detailState is DeviceDetailUiState.Error) {
                                Text(detailState.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        ChevronGlyph()
                    }
                }
            }
        }
    }

    if (rename && deviceName != null) AlertDialog(
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
                            confirmedName = draftName
                            rename = false
                            if (deviceIdLong != null) {
                                ownedDevices.loadDeviceDetail(deviceIdLong)
                            }
                            ownedDevices.refresh()
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