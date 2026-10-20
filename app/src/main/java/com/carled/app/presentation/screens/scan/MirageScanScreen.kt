package com.carled.app.presentation.screens.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.carled.app.data.ble.bleScanPermissions
import com.carled.app.data.ble.readBleScanBlocker
import com.carled.app.domain.model.BleScanBlocker
import com.carled.app.domain.model.ConnectionState
import com.carled.app.presentation.components.*

@Composable
fun MirageScanScreen(vm: ScanViewModel, back: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionError by remember { mutableStateOf(false) }
    val permissions = remember { bleScanPermissions() }
    var blocker by remember { mutableStateOf(readBleScanBlocker(context)) }
    var scanAfterSettings by rememberSaveable { mutableStateOf(false) }
    val currentState by rememberUpdatedState(state)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        blocker = readBleScanBlocker(context)
        permissionError = blocker == BleScanBlocker.PERMISSION
        if (blocker == null) vm.startScan()
    }
    fun scan() {
        blocker = readBleScanBlocker(context)
        when (blocker) {
            null -> vm.startScan()
            BleScanBlocker.PERMISSION -> launcher.launch(permissions)
            else -> Unit
        }
    }
    fun settings(action: String) { scanAfterSettings = true; context.startActivity(Intent(action)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                blocker = readBleScanBlocker(context)
                if (blocker == null && scanAfterSettings && currentState.connectionState !is ConnectionState.Connected && currentState.connectionState !is ConnectionState.Connecting) {
                    scanAfterSettings = false
                    vm.startScan()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); vm.stopScan() }
    }
    LaunchedEffect(Unit) {
        // Preserve the original screen's automatic scan when prerequisites are already satisfied.
        if (readBleScanBlocker(context) == null && state.connectionState !is ConnectionState.Connected && state.connectionState !is ConnectionState.Connecting) vm.startScan()
    }
    Scaffold(topBar = { MirageBar("Thêm thiết bị", back) }, containerColor = MirageBackground) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            MirageCard {
                Icon(Icons.Default.BluetoothSearching, null, Modifier.size(64.dp).align(Alignment.CenterHorizontally), tint = MaterialTheme.colorScheme.primary)
                Text("Kết nối với LED của bạn", style = MaterialTheme.typography.headlineSmall)
                Text("Bật nguồn thiết bị và Bluetooth. Đặt điện thoại gần LED để kết nối ổn định.", color = MirageMuted)
                Text(if (Build.VERSION.SDK_INT >= 31) "Ứng dụng cần quyền Thiết bị ở gần để quét Bluetooth." else "Android phiên bản này cần quyền Vị trí và dịch vụ Vị trí để quét BLE.")
                MirageButton(if (state.isScanning) "Dừng quét" else "Quét thiết bị", {
                    if (state.isScanning) vm.stopScan()
                    else scan()
                }, Modifier.fillMaxWidth(), enabled = state.connectionState !is ConnectionState.Connecting && state.connectionState !is ConnectionState.Connected)
                if (state.isScanning) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Đang tìm thiết bị…") }
            }
            blocker?.let { cause ->
                MirageCard {
                    Text(cause.message, color = MaterialTheme.colorScheme.error)
                    when (cause) {
                        BleScanBlocker.LOCATION_OFF -> {
                            Text("Đã cấp quyền Vị trí chưa đủ: cần bật công tắc Vị trí trong cài đặt điện thoại. Ứng dụng không dùng GPS để điều khiển LED.")
                            MirageButton("Bật Vị trí để quét BLE", { settings(Settings.ACTION_LOCATION_SOURCE_SETTINGS) }, Modifier.fillMaxWidth())
                        }
                        BleScanBlocker.BLUETOOTH_OFF -> MirageButton("Bật Bluetooth", { settings(Settings.ACTION_BLUETOOTH_SETTINGS) }, Modifier.fillMaxWidth())
                        BleScanBlocker.PERMISSION -> MirageButton("Cấp quyền quét", { launcher.launch(permissions) }, Modifier.fillMaxWidth())
                        else -> Unit
                    }
                }
            }
            if (permissionError) {
                Text("Chưa được cấp quyền quét. Cho phép quyền được yêu cầu trong Cài đặt rồi thử lại.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { scanAfterSettings = true; context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) }) { Text("Mở cài đặt ứng dụng") }
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = { settings(Settings.ACTION_BLUETOOTH_SETTINGS) }) { Text("Mở cài đặt Bluetooth") }
            Text("Thiết bị LED ở gần", style = MaterialTheme.typography.titleLarge)
            Text("Chỉ hiển thị CarLED hoặc thiết bị có dịch vụ LED tương thích.", color = MirageMuted)
            if (blocker == null && !state.isScanning && state.devices.isEmpty()) Text("Chưa tìm thấy LED tương thích. Bật nguồn CarLED, đặt gần điện thoại rồi quét lại.", color = MirageMuted)
            state.devices.forEach { device -> MirageCard {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text(if (device.rssi > -65) "Tín hiệu mạnh" else "Đưa điện thoại gần thiết bị hơn", color = MirageMuted)
                val connected = (state.connectionState as? ConnectionState.Connected)?.deviceAddress == device.macAddress
                MirageButton(if (connected) "Đã kết nối" else if (state.connectionState is ConnectionState.Connecting) "Đang kết nối…" else "Kết nối", { vm.connectDevice(device.macAddress) }, Modifier.fillMaxWidth(), enabled = !connected && state.connectionState !is ConnectionState.Connecting)
            } }
            if (state.connectionState is ConnectionState.Connected) MirageButton("Hoàn tất", back, Modifier.fillMaxWidth())
        }
    }
}
