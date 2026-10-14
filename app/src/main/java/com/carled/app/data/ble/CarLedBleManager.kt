package com.carled.app.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.carled.app.core.common.Constants
import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.DeviceTelemetryStatus
import com.carled.app.domain.model.TelemetryEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

@SuppressLint("MissingPermission")
class CarLedBleManager(private val context: Context) {

    companion object {
        private const val TAG = "CarLedBLE"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDeviceModel>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    /**
     * Phát ra Status Telemetry định kỳ (JSON không có field "event").
     * Dùng để cập nhật UI trạng thái thiết bị.
     */
    private val _telemetryStatus = MutableStateFlow(DeviceTelemetryStatus())
    val telemetryStatus = _telemetryStatus.asStateFlow()

    /**
     * Phát ra Event một lần (JSON có field "event").
     * Dùng SharedFlow để UI nhận được ngay cả khi không collect liên tục.
     *
     * Các event: upload_started, upload_complete, upload_error,
     *            marquee_active, wifi_connecting, projection_reloaded, canvas_cleared
     */
    private val _telemetryEvent = MutableSharedFlow<TelemetryEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val telemetryEvent: SharedFlow<TelemetryEvent> = _telemetryEvent.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    // Rate-limiting cho vuốt thanh màu / độ sáng thời gian thực
    private var lastSendTimestamp = 0L

    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null || bluetoothAdapter?.isEnabled != true) {
            Log.w(TAG, "Bluetooth chưa được kích hoạt trên điện thoại")
            _connectionState.value = ConnectionState.Error("Vui lòng bật Bluetooth trên điện thoại")
            return
        }

        _scannedDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning
        Log.i(TAG, "Bắt đầu quét thiết bị BLE...")

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            // Quét không gò bó filter để không bị drop packet scan response của ESP32
            scanner.startScan(null, scanSettings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Không thể bắt đầu scan: ${e.message}")
            _connectionState.value = ConnectionState.Error("Lỗi khởi chạy quét: ${e.message}")
            return
        }

        // Tự động dừng scan sau 15s để tránh scan throttling của Android
        scanJob?.cancel()
        scanJob = scope.launch {
            delay(15000L)
            stopScan()
            if (_connectionState.value == ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    fun stopScan() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            Log.i(TAG, "Đã dừng quét BLE")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi dừng scan: ${e.message}")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val scanRecord = result.scanRecord

            // Lấy tên từ ScanRecord trước (không cần quyền BLUETOOTH_CONNECT)
            val advertisedName = scanRecord?.deviceName
            val fallbackName = try { device.name } catch (e: SecurityException) { null }

            val rawName = advertisedName ?: fallbackName
            val macAddress = device.address
            val rssi = result.rssi

            // Bỏ qua các thiết bị không có tên và không phát UUID của CarLED để tránh rác danh sách
            val hasServiceUuid = scanRecord?.serviceUuids?.any {
                it.uuid == Constants.BLE_SERVICE_UUID
            } == true

            val isCarLed = (rawName?.contains(Constants.BLE_DEVICE_NAME_PREFIX, ignoreCase = true) == true)
                    || (rawName?.contains("ESP32", ignoreCase = true) == true)
                    || hasServiceUuid

            // Đặt tên hiển thị thân thiện
            val displayName = when {
                !rawName.isNullOrBlank() -> rawName
                hasServiceUuid -> "CarLED-Ambient"
                else -> "BLE Device (${macAddress.takeLast(5)})"
            }

            val isBonded = try {
                device.bondState == BluetoothDevice.BOND_BONDED
            } catch (e: SecurityException) {
                false
            }

            val currentList = _scannedDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.macAddress == macAddress }

            val model = BleDeviceModel(
                name = displayName,
                macAddress = macAddress,
                rssi = rssi,
                isBonded = isBonded
            )

            if (existingIndex >= 0) {
                currentList[existingIndex] = model
            } else {
                currentList.add(model)
            }

            // Đưa các thiết bị CarLED / ESP32 lên đầu danh sách
            currentList.sortByDescending {
                it.name.contains(Constants.BLE_DEVICE_NAME_PREFIX, ignoreCase = true) ||
                it.name.contains("ESP32", ignoreCase = true)
            }

            _scannedDevices.value = currentList
            Log.d(TAG, "Phát hiện thiết bị: $displayName ($macAddress, RSSI: $rssi dBm)")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan thất bại với mã lỗi: $errorCode")
            _connectionState.value = ConnectionState.Error("Quét thiết bị thất bại (Mã lỗi: $errorCode)")
        }
    }

    fun connect(macAddress: String) {
        val adapter = bluetoothAdapter ?: return
        val device = try {
            adapter.getRemoteDevice(macAddress)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Địa chỉ MAC không hợp lệ")
            return
        }

        _connectionState.value = ConnectionState.Connecting

        // Dừng scan trước khi connect để tránh lỗi GATT 133
        stopScan()

        // Đóng session cũ nếu còn tồn tại
        cleanupGatt()

        bluetoothGatt = device.connectGatt(
            context,
            false, // false = kết nối trực tiếp, không đợi autoConnect
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange status=$status, newState=$newState")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT Error status: $status. Đang thu hồi tài nguyên...")
                cleanupGatt()
                _connectionState.value = ConnectionState.Error("Lỗi kết nối GATT: $status")
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BLE đã kết nối. Yêu cầu mở rộng MTU lên ${Constants.BLE_DEFAULT_MTU}...")
                _connectionState.value = ConnectionState.Connecting
                val mtuRequested = gatt.requestMtu(Constants.BLE_DEFAULT_MTU)
                if (!mtuRequested) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        gatt.discoverServices()
                    }, 200)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "Thiết bị ngắt kết nối.")
                cleanupGatt()
                _connectionState.value = ConnectionState.Disconnected
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "MTU đã đổi: $mtu bytes (status: $status)")
            Handler(Looper.getMainLooper()).postDelayed({
                gatt.discoverServices()
            }, 100)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(Constants.BLE_SERVICE_UUID)
                if (service != null) {
                    writeCharacteristic = service.getCharacteristic(Constants.BLE_WRITE_CHAR_UUID)
                    writeCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

                    // Đăng ký nhận thông báo Telemetry (Status Notification)
                    val statusChar = service.getCharacteristic(Constants.BLE_STATUS_CHAR_UUID)
                    if (statusChar != null) {
                        gatt.setCharacteristicNotification(statusChar, true)
                        val descriptor = statusChar.getDescriptor(Constants.BLE_CCCD_UUID)
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                        }
                    }

                    val safeName = try { gatt.device.name } catch (e: SecurityException) { null }
                    _connectionState.value = ConnectionState.Connected(
                        deviceName = if (!safeName.isNullOrBlank()) safeName else "CarLED-Ambient",
                        deviceAddress = gatt.device.address
                    )
                    Log.i(TAG, "Sẵn sàng điều khiển ESP32 LED Car!")
                } else {
                    _connectionState.value = ConnectionState.Error("Không tìm thấy LED Service trên thiết bị")
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val telemetryJson = String(characteristic.value, Charsets.UTF_8)
            Log.d(TAG, "Nhận Telemetry từ LED ESP32: $telemetryJson")
            dispatchTelemetry(telemetryJson)
        }
    }

    /**
     * Phân tích gói tin JSON telemetry từ ESP32.
     *
     * Quy tắc:
     *  - Nếu JSON có field "event" → parse thành [TelemetryEvent] và emit qua [_telemetryEvent]
     *  - Nếu JSON KHÔNG có field "event" → parse thành [DeviceTelemetryStatus] và cập nhật [_telemetryStatus]
     */
    private fun dispatchTelemetry(json: String) {
        runCatching {
            val obj = JSONObject(json)
            if (obj.has("event")) {
                // ── Event packet ──
                val event = when (val eventName = obj.optString("event")) {
                    "upload_started"     -> TelemetryEvent.UploadStarted(
                        expected = obj.optInt("expected", 0)
                    )
                    "upload_complete"    -> TelemetryEvent.UploadComplete(
                        status = obj.optString("status", "ok"),
                        mode   = obj.optInt("mode", 6)
                    )
                    "upload_error"       -> TelemetryEvent.UploadError(
                        error = obj.optString("error", "unknown")
                    )
                    "marquee_active"     -> TelemetryEvent.MarqueeActive(
                        text = obj.optString("text", "")
                    )
                    "wifi_connecting"    -> TelemetryEvent.WifiConnecting(
                        ssid = obj.optString("ssid", "")
                    )
                    "projection_reloaded" -> TelemetryEvent.ProjectionReloaded(
                        mode = obj.optInt("mode", 6)
                    )
                    "canvas_cleared"     -> TelemetryEvent.CanvasCleared
                    else                 -> TelemetryEvent.Unknown(json)
                }
                scope.launch { _telemetryEvent.emit(event) }
                Log.d(TAG, "Event: $event")
            } else {
                // ── Periodic status telemetry ──
                val colorObj = obj.optJSONObject("color")
                _telemetryStatus.value = DeviceTelemetryStatus(
                    power      = obj.optBoolean("power", false),
                    brightness = obj.optInt("brightness", 180),
                    mode       = obj.optInt("mode", 0),
                    speed      = obj.optInt("speed", 50),
                    r          = colorObj?.optInt("r", 0) ?: 0,
                    g          = colorObj?.optInt("g", 150) ?: 150,
                    b          = colorObj?.optInt("b", 255) ?: 255,
                    freeHeap   = obj.optInt("free_heap", 0)
                )
            }
        }.onFailure {
            Log.w(TAG, "Telemetry parse error: ${it.message} — raw: $json")
        }
    }

    fun sendCommand(frame: ByteArray, throttled: Boolean = false) {
        val now = System.currentTimeMillis()
        if (throttled && (now - lastSendTimestamp < Constants.BLE_THROTTLE_INTERVAL_MS)) {
            return // Bỏ qua frame để tránh nghẽn hàng đợi GATT khi lướt ngón tay
        }
        lastSendTimestamp = now
        sendRawBytes(frame)
    }

    fun sendJson(json: String) {
        sendRawBytes(json.toByteArray(Charsets.UTF_8))
    }

    private fun sendRawBytes(bytes: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val char = writeCharacteristic ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                char,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
        } else {
            @Suppress("DEPRECATION")
            char.value = bytes
            @Suppress("DEPRECATION")
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }

    fun disconnect() {
        cleanupGatt()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun cleanupGatt() {
        bluetoothGatt?.let {
            try {
                it.disconnect()
                it.close()
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi cleanup GATT: ${e.message}")
            }
        }
        bluetoothGatt = null
        writeCharacteristic = null
    }
}
