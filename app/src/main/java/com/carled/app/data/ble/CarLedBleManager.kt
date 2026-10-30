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
import com.carled.app.domain.model.isLedAdvertisement
import com.carled.app.domain.model.isValidDeviceName
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
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
    private val telemetryAssembler=TelemetryAssembler()

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
        readBleScanBlocker(context)?.let { blocker ->
            Log.w(TAG, "Không thể quét BLE: ${blocker.name}")
            _connectionState.value = ConnectionState.Error(blocker.message)
            return
        }
        if (_connectionState.value is ConnectionState.Scanning ||
            _connectionState.value is ConnectionState.Connecting ||
            _connectionState.value is ConnectionState.Connected) return
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
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
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

            // Keep broad radio discovery so split ESP32 advertising/scan responses are not lost.
            // Discard unrelated devices before storing, displaying, or logging them.
            val hasServiceUuid = scanRecord?.serviceUuids?.any {
                it.uuid == Constants.BLE_SERVICE_UUID
            } == true

            if (!isLedAdvertisement(rawName, hasServiceUuid)) return

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

            currentList.sortByDescending { it.rssi }

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

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && descriptor.uuid == Constants.BLE_CCCD_UUID) {
                scope.launch {
                    delay(150)
                    if (bluetoothGatt === gatt && _connectionState.value is ConnectionState.Connected) {
                        runCatching { sendJson(JSONObject().put("command", "get_device_info").toString()) }
                    }
                }
            }
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

                    val safeName = _scannedDevices.value.firstOrNull { it.macAddress == gatt.device.address }?.name
                        ?: try { gatt.device.name } catch (e: SecurityException) { null }
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
            telemetryAssembler.accept(characteristic.value).forEach(::dispatchTelemetry)
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
                    "ota_info" -> TelemetryEvent.OtaInfo(obj.optString("request_id"),obj.optString("device_id"),obj.optString("version"),obj.optString("hardware"),obj.optString("nonce"),obj.optBoolean("wifi"),obj.optBoolean("configured"),obj.optBoolean("busy"),obj.optBoolean("healthy"))
                    "ota_status" -> TelemetryEvent.OtaStatus(obj.optString("request_id"),obj.optString("state"),obj.optInt("progress"),obj.optString("error"))
                    "device_name" -> {
                        val name = obj.optString("name")
                        val connected = _connectionState.value as? ConnectionState.Connected
                        if (name.isNotBlank() && connected != null) {
                            _connectionState.value = connected.copy(deviceName = name)
                            _scannedDevices.value = _scannedDevices.value.map {
                                if (it.macAddress == connected.deviceAddress) it.copy(name = name) else it
                            }
                        }
                        TelemetryEvent.DeviceName(obj.optString("request_id"), name, obj.optString("error"))
                    }
                    "canvas_batch_applied" -> TelemetryEvent.CanvasBatchApplied(obj.optInt("sequence", -1))
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
                    "wifi_connected"     -> TelemetryEvent.WifiConnected(
                        ip = obj.optString("ip", "")
                    )
                    "wifi_disconnected"  -> TelemetryEvent.WifiDisconnected
                    "emergency_stop"     -> TelemetryEvent.EmergencyStop(
                        active = obj.optBoolean("active", true)
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
                val isEstop = obj.optBoolean("emergency_stop", false)
                val ip = obj.optString("ip", "")
                _telemetryStatus.value = DeviceTelemetryStatus(
                    power           = obj.optBoolean("power", false),
                    brightness      = obj.optInt("brightness", 180),
                    mode            = obj.optInt("mode", 0),
                    speed           = obj.optInt("speed", 50),
                    r               = colorObj?.optInt("r", 0) ?: 0,
                    g               = colorObj?.optInt("g", 150) ?: 150,
                    b               = colorObj?.optInt("b", 255) ?: 255,
                    freeHeap        = obj.optInt("free_heap", 0),
                    isEmergencyStop = isEstop,
                    wifiIp          = ip
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

    suspend fun renameDevice(name: String) = coroutineScope {
        val proposed = name.trim()
        require(isValidDeviceName(proposed)) {
            "Tên phải có 1–29 byte UTF-8 và không có ký tự điều khiển"
        }
        val requestId = java.util.UUID.randomUUID().toString()
        val reply = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5000) {
                _telemetryEvent.first { it is TelemetryEvent.DeviceName && it.requestId == requestId } as TelemetryEvent.DeviceName
            }
        }
        try {
            sendJson(JSONObject().put("command", "rename_device").put("name", proposed).put("request_id", requestId).toString())
            val result = reply.await()
            check(result.error.isBlank()) { result.error }
            check(result.name == proposed) { "LED chưa xác nhận tên mới" }
        } finally { reply.cancel() }
    }

    fun sendJson(json: String) {
        sendRawBytes(json.toByteArray(Charsets.UTF_8))
    }

    private fun sendRawBytes(bytes: ByteArray) {
        check(_connectionState.value is ConnectionState.Connected) { "Thiết bị LED chưa kết nối" }
        val gatt = checkNotNull(bluetoothGatt) { "Thiết bị LED đã ngắt kết nối" }
        val char = checkNotNull(writeCharacteristic) { "Thiết bị chưa sẵn sàng nhận lệnh" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(
                char,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
            check(result == android.bluetooth.BluetoothStatusCodes.SUCCESS) { "Bluetooth chưa nhận lệnh ($result). Hãy thử lại." }
        } else {
            @Suppress("DEPRECATION")
            char.value = bytes
            @Suppress("DEPRECATION")
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            check(gatt.writeCharacteristic(char)) { "Bluetooth chưa nhận lệnh. Hãy thử lại." }
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
        telemetryAssembler.clear()
        writeCharacteristic = null
    }
}
