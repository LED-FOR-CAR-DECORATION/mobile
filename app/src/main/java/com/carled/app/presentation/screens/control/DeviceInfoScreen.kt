package com.carled.app.presentation.screens.control

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.MockDeviceInventory
import com.carled.app.domain.model.isValidDeviceName
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeviceInfoScreen(
    vm: ControlViewModel,
    ownedDevices: OwnedDevicesViewModel,
    panelId: String?,
    back: () -> Unit,
    openPanelControl: (String) -> Unit,
    connectDevice: (macAddress: String) -> Unit,
    disconnectDevice: () -> Unit = {}
) {
    LaunchedEffect(panelId) {
        ownedDevices.loadDeviceDetail(panelId)
    }

    val connectionState by vm.connectionState.collectAsState()
    val devicesState by ownedDevices.uiState.collectAsState()
    val detailState by ownedDevices.detailState.collectAsState()

    val cachedDevice = remember(devicesState, panelId) { ownedDevices.deviceById(panelId) }
    val detailDto = (detailState as? DeviceDetailUiState.Loaded)?.detail

    val displayName = detailDto?.deviceName ?: cachedDevice?.deviceName
    val displayMac = detailDto?.macAddress ?: cachedDevice?.macAddress
    val displaySerial = detailDto?.serialNumber ?: cachedDevice?.serialNumber
    val displayFirmware = detailDto?.currentFirmwareId?.toString() ?: cachedDevice?.firmwareVersion
    val displayLastUpdated = detailDto?.boundAt ?: cachedDevice?.lastUpdated
    val deviceId = detailDto?.deviceId?.toString() ?: cachedDevice?.id ?: panelId.orEmpty()

    val hasDevice = displayName != null && displayMac != null
    val connectedDevice = connectionState as? ConnectionState.Connected
    val connectedToThis = hasDevice && connectedDevice?.deviceAddress == displayMac
    val isConnecting = connectionState is ConnectionState.Connecting

    // Timeout tracker for connection attempts (8 seconds threshold)
    var showTimeoutError by remember { mutableStateOf(false) }

    LaunchedEffect(isConnecting) {
        if (isConnecting) {
            showTimeoutError = false
            delay(8000L)
            if (connectionState is ConnectionState.Connecting) {
                showTimeoutError = true
            }
        } else {
            showTimeoutError = false
        }
    }

    val firmware = remember { MockDeviceInventory.firmwareReleases() }

    val requestPermissionsAndConnect = rememberBluetoothPermissionLauncher(
        onGranted = { connectDevice(displayMac.orEmpty()) }
    )

    var rename by remember { mutableStateOf(false) }
    var draftName by remember(displayName) { mutableStateOf(displayName.orEmpty()) }
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
        WindowFrame(
            title = "Thông tin thiết bị",
            onClose = back,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (!hasDevice && (devicesState is OwnedDevicesUiState.Loading || detailState is DeviceDetailUiState.Loading)) {
                    Text("Đang tải thông tin thiết bị…", color = MirageMuted)
                } else if (!hasDevice) {
                    Text("Không tìm thấy thiết bị này.", color = MaterialTheme.colorScheme.error)
                } else {
                    // 1. CONNECTION SECTION (Connection text line removed)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "Connection Status:" label box
                            Box(
                                Modifier
                                    .hardSurface(color = Cream, offset = 4.dp, borderWidth = 2.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("Connection Status:", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            // Dynamic Status Badge (Connected / Connecting / Not connected)
                            val statusText = when {
                                connectedToThis -> "Connected"
                                isConnecting -> "Connecting..."
                                else -> "Not connected"
                            }
                            val badgeColor = if (connectedToThis) GreenLight else Cream

                            Box(
                                Modifier
                                    .hardSurface(color = badgeColor, offset = 4.dp, borderWidth = 2.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(statusText, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        // Timeout error message
                        if (showTimeoutError) {
                            Text("Can't connect to device", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }

                        Spacer(Modifier.height(4.dp))

                        // Connect / Disconnect button state
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .alpha(if (isConnecting) 0.5f else 1f)
                                .hardSurface(color = if (connectedToThis) Cream else GreenLight, offset = 6.dp, borderWidth = 2.dp)
                                .clickable(
                                    enabled = !isConnecting,
                                    onClick = {
                                        if (connectedToThis) {
                                            disconnectDevice()
                                        } else {
                                            requestPermissionsAndConnect()
                                        }
                                    }
                                )
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (connectedToThis) "Disconnect" else "Connect to device",
                                color = Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                // 2. FIRMWARE VERSIONS SECTION
                DeviceSection(title = "Firmware versions") {
                    firmware.forEach { release -> FirmwareReleaseCard(release) }
                }

                // 3. DEVICE INFO SECTION
                if (hasDevice) {
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
                }
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

@Composable
fun rememberBluetoothPermissionLauncher(
    onGranted: () -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onGranted()
        }
    }

    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            val scanGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

            if (connectGranted && scanGranted) {
                onGranted()
            } else {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
        } else {
            onGranted()
        }
    }
}