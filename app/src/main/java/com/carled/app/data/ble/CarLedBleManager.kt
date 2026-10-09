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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    // Rate-limiting cho vuốt thanh màu / độ sáng thời gian thực
    private var lastSendTimestamp = 0L

    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null || bluetoothAdapter?.isEnabled != true) {
            _connectionState.value = ConnectionState.Error("Bluetooth chưa được kích hoạt")
            return
        }

        _scannedDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(Constants.BLE_SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Không thể bắt đầu scan: ${e.message}")
            _connectionState.value = ConnectionState.Error("Lỗi khởi chạy quét: ${e.message}")
            return
        }

        // Tự động dừng scan sau 10s để tránh scan throttling của Android
        scanJob?.cancel()
        scanJob = scope.launch {
            delay(Constants.BLE_SCAN_TIMEOUT_MS)
            stopScan()
            if (_connectionState.value == ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    fun stopScan() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi dừng scan: ${e.message}")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Unknown Car LED"
            val macAddress = device.address
            val rssi = result.rssi

            val currentList = _scannedDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.macAddress == macAddress }

            val model = BleDeviceModel(
                name = deviceName,
                macAddress = macAddress,
                rssi = rssi,
                isBonded = device.bondState == BluetoothDevice.BOND_BONDED
            )

            if (existingIndex >= 0) {
                currentList[existingIndex] = model
            } else {
                currentList.add(model)
            }
            _scannedDevices.value = currentList
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan thất bại với mã lỗi: $errorCode")
            _connectionState.value = ConnectionState.Error("Quét thiết bị thất bại ($errorCode)")
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
                gatt.requestMtu(Constants.BLE_DEFAULT_MTU)
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

                    _connectionState.value = ConnectionState.Connected(
                        deviceName = gatt.device.name ?: "CarLED",
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
