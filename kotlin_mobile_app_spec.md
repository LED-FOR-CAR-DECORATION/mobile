# 📱 Kotlin Android App — Car LED Ambient Lighting
## Tài liệu Đặc tả Đầy đủ để Build Mobile App

> Đọc và tổng hợp từ toàn bộ source code IoT-Device firmware, protocols, docs của dự án.

---

## 1. Tổng quan dự án

Ứng dụng Android Kotlin kết nối BLE với thiết bị **ESP32 Car LED Ambient** (`CarLED-Ambient`) để điều khiển hệ thống LED trang trí nội thất ô tô theo thời gian thực.

### Tính năng cốt lõi cần implement:
| Feature | Mô tả |
|---|---|
| BLE Scan & Connect | Quét và kết nối BLE đến thiết bị `CarLED-Ambient` |
| Power Toggle | Bật/tắt đèn |
| Brightness Slider | Điều chỉnh độ sáng (0–255) |
| Mode Picker | Chọn hiệu ứng (OFF, Static, Breathing, Rainbow, Chase, Music, Custom, Marquee) |
| RGB Color Picker | Color Wheel / Slider chọn màu |
| Speed Slider | Tốc độ hiệu ứng (1–100) |
| Mic Sensitivity | Độ nhạy mic khi dùng MUSIC_REACTIVE |
| Marquee Text | Nhập văn bản chạy chữ trên LED |
| Live Canvas | Vẽ trực tiếp lên LED matrix (pixel art) |
| Image Upload | Nạp ảnh/animation lên SPIFFS của ESP32 |
| Status Telemetry | Nhận trạng thái thiết bị từ BLE Notify |
| Wi-Fi Config | Gửi SSID/Password qua BLE để ESP32 kết nối WiFi |
| Auto-Reconnect | Tự động kết nối lại khi thiết bị đã ghép cặp |

---

## 2. Cấu hình Project Android

### 2.1. `build.gradle.kts` (app level)
```kotlin
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 23          // Android 6.0 – hỗ trợ BLE cơ bản
        targetSdk = 34
    }
}

dependencies {
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Color Picker (HSV Wheel)
    implementation("com.github.QuadFlask:colorpicker:0.0.15")

    // Image loading (cho Image Upload preview)
    implementation("io.coil-kt:coil:2.5.0")
}
```

### 2.2. `AndroidManifest.xml` — Quyền BLE (Bắt buộc)
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Bắt buộc phải có BLE hardware -->
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />

    <!-- Android 12+ (API 31+): Quyền BLE tách biệt -->
    <uses-permission
        android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <!-- Android 6.0–11 (API 23–30): Quyền BLE + Location cũ -->
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />

</manifest>
```

> [!IMPORTANT]
> Nếu không khai báo `neverForLocation` trên Android 12+, người dùng sẽ bị yêu cầu cấp quyền Location nhạy cảm không cần thiết.

---

## 3. Hằng số & UUID BLE (Từ firmware `config.h`)

```kotlin
// BleConstants.kt
object BleConstants {
    // Tên thiết bị quảng bá BLE (dùng để filter scan nếu cần)
    const val DEVICE_ADVERTISING_NAME = "CarLED-Ambient"

    // UUIDs — Đồng bộ chính xác từ include/config.h của firmware ESP32
    val SERVICE_UUID         = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val WRITE_CHAR_UUID      = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    val STATUS_CHAR_UUID     = UUID.fromString("1c95d5e3-d8f7-413a-bf3d-7a2e5d7be87e")
    val CCCD_UUID            = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Binary Protocol framing
    const val FRAME_HEADER: Byte = 0xAA.toByte()
    const val FRAME_TAIL:   Byte = 0x55.toByte()

    // Throttle rate cho slider/color wheel (40ms = ~25 FPS)
    const val THROTTLE_INTERVAL_MS = 40L

    // Timeout JSON buffer stale (ms)
    const val JSON_RX_TIMEOUT_MS = 1000L
}
```

---

## 4. Giao thức Nhị phân 7-byte (Binary Protocol)

> [!NOTE]
> Dùng Binary Protocol cho tất cả thao tác **thời gian thực** (slider, color wheel, canvas).  
> Dùng JSON cho lệnh cấu hình (marquee, wifi, chế độ phức tạp).

### 4.1. Cấu trúc khung chuẩn
```
[0xAA] [CMD] [P1] [P2] [P3] [CHECKSUM] [0x55]
  Byte0  Byte1  Byte2 Byte3 Byte4  Byte5    Byte6

CHECKSUM = CMD ^ P1 ^ P2 ^ P3
```

### 4.2. Bảng Opcode đầy đủ (từ `BleManager.h` + `protocols/commands.md`)

| Opcode | Tên lệnh | P1 | P2 | P3 | Ví dụ Hex |
|---|---|---|---|---|---|
| `0x01` | **Power** | 0x01=ON / 0x00=OFF | 0x00 | 0x00 | Bật: `AA 01 01 00 00 00 55` |
| `0x02` | **Brightness** | 0–255 | 0x00 | 0x00 | 180: `AA 02 B4 00 00 B6 55` |
| `0x03` | **Speed** | 1–100 | 0x00 | 0x00 | 60: `AA 03 3C 00 00 3F 55` |
| `0x04` | **Mode** | 0–7 (enum) | 0x00 | 0x00 | Rainbow(3): `AA 04 03 00 00 07 55` |
| `0x05` | **RGB Color** | R (0–255) | G (0–255) | B (0–255) | Ice Blue: `AA 05 00 96 FF 6C 55` |
| `0x06` | **Mic Sensitivity** | 1–100 | 0x00 | 0x00 | 80: `AA 06 50 00 00 56 55` |
| `0x10` | **Projection Start** | frameCount | width(64) | height(32) | Bắt đầu upload |
| `0x11` | **Projection Chunk** | chunkIdxH | chunkIdxL | dataLen | Gửi từng khối data |
| `0x12` | **Projection Finish** | activateNow(1/0) | 0x00 | 0x00 | Hoàn thành upload |
| `0x13` | **Draw Live Pixel** | x (0–63) | y (0–31) | R | + [G, B] bytes thêm |
| `0x14` | **Draw Batch** | pointCount | — | — | Gom nhiều điểm |
| `0x15` | **Canvas Clear** | 0x00 | 0x00 | 0x00 | Xóa màn hình |

### 4.3. Bảng mã Mode (EffectMode enum từ `types.h`)
```kotlin
enum class EffectMode(val code: Int, val displayName: String) {
    OFF(0, "Tắt"),
    STATIC_COLOR(1, "Màu tĩnh"),
    BREATHING(2, "Nhịp thở"),
    RAINBOW_WAVE(3, "Cầu vồng"),
    CHASE_FLOW(4, "Dòng chảy"),
    WELCOME_CAR(5, "Chào xe"), // MUSIC_REACTIVE trong JSON protocol
    CUSTOM_PRESET(6, "Ảnh tùy chỉnh"),
    MARQUEE_TEXT(7, "Chạy chữ")
}
```

> [!WARNING]
> `WELCOME_CAR` trong firmware enum = mode 5. Trong JSON protocol, mode 5 = `MUSIC_REACTIVE`. Cần mapping cẩn thận khi gửi lệnh.

---

## 5. BleProtocolHelper.kt — Bộ tạo khung lệnh

```kotlin
// app/src/main/java/com/carled/app/bluetooth/BleProtocolHelper.kt
package com.carled.app.bluetooth

data class TouchPoint(val x: Int, val y: Int, val r: Int, val g: Int, val b: Int)

object BleProtocolHelper {
    private const val HEADER: Byte = 0xAA.toByte()
    private const val TAIL:   Byte = 0x55.toByte()

    const val CMD_POWER:       Byte = 0x01
    const val CMD_BRIGHTNESS:  Byte = 0x02
    const val CMD_SPEED:       Byte = 0x03
    const val CMD_MODE:        Byte = 0x04
    const val CMD_COLOR_RGB:   Byte = 0x05
    const val CMD_SENSITIVITY: Byte = 0x06

    // Flow 2 — Pixel Art / Animation
    const val CMD_PROJ_START:  Byte = 0x10
    const val CMD_PROJ_CHUNK:  Byte = 0x11
    const val CMD_PROJ_FINISH: Byte = 0x12
    const val CMD_DRAW_PIXEL:  Byte = 0x13
    const val CMD_DRAW_BATCH:  Byte = 0x14
    const val CMD_CANVAS_CLEAR: Byte = 0x15

    /**
     * Khung chuẩn 7 bytes: [0xAA, cmd, p1, p2, p3, checksum, 0x55]
     * checksum = cmd XOR p1 XOR p2 XOR p3
     */
    fun buildFrame(cmd: Byte, p1: Int = 0, p2: Int = 0, p3: Int = 0): ByteArray {
        val b1 = (p1 and 0xFF).toByte()
        val b2 = (p2 and 0xFF).toByte()
        val b3 = (p3 and 0xFF).toByte()
        val checksum = (cmd.toInt() xor b1.toInt() xor b2.toInt() xor b3.toInt()).toByte()
        return byteArrayOf(HEADER, cmd, b1, b2, b3, checksum, TAIL)
    }

    fun buildPowerFrame(on: Boolean) =
        buildFrame(CMD_POWER, if (on) 1 else 0)

    fun buildBrightnessFrame(level: Int) =
        buildFrame(CMD_BRIGHTNESS, level.coerceIn(0, 255))

    fun buildSpeedFrame(speed: Int) =
        buildFrame(CMD_SPEED, speed.coerceIn(1, 100))

    fun buildModeFrame(modeIndex: Int) =
        buildFrame(CMD_MODE, modeIndex.coerceIn(0, 7))

    fun buildRgbColorFrame(r: Int, g: Int, b: Int) =
        buildFrame(CMD_COLOR_RGB, r, g, b)

    fun buildSensitivityFrame(sens: Int) =
        buildFrame(CMD_SENSITIVITY, sens.coerceIn(1, 100))

    fun buildCanvasClearFrame() =
        buildFrame(CMD_CANVAS_CLEAR, 0, 0, 0)

    /**
     * Draw Live Pixel — Frame 9 bytes với Checksum:
     * [0xAA, 0x13, x, y, r, g, b, checksum, 0x55]
     * checksum = 0x13 XOR x XOR y XOR r XOR g XOR b
     */
    fun buildDrawPixelFrame(x: Int, y: Int, r: Int, g: Int, b: Int): ByteArray {
        val bx = (x and 0xFF).toByte()
        val by = (y and 0xFF).toByte()
        val br = (r and 0xFF).toByte()
        val bg = (g and 0xFF).toByte()
        val bb = (b and 0xFF).toByte()
        val checksum = (CMD_DRAW_PIXEL.toInt()
            xor bx.toInt() xor by.toInt()
            xor br.toInt() xor bg.toInt() xor bb.toInt()).toByte()
        return byteArrayOf(HEADER, CMD_DRAW_PIXEL, bx, by, br, bg, bb, checksum, TAIL)
    }

    /**
     * Draw Batch — Gom nhiều điểm chạm trong một gói (chống nghẽn BLE):
     * [0xAA, 0x14, count, x0,y0,r0,g0,b0, ..., xN,yN,rN,gN,bN, checksum, 0x55]
     */
    fun buildBatchDrawFrame(points: List<TouchPoint>): ByteArray {
        val count = points.size.coerceAtMost(15)
        val payload = ByteArray(3 + count * 5 + 2)
        payload[0] = HEADER
        payload[1] = CMD_DRAW_BATCH
        payload[2] = count.toByte()

        var csum = (CMD_DRAW_BATCH.toInt() xor count).toByte()
        for (i in 0 until count) {
            val pt = points[i]
            val offset = 3 + i * 5
            payload[offset]     = (pt.x and 0xFF).toByte()
            payload[offset + 1] = (pt.y and 0xFF).toByte()
            payload[offset + 2] = (pt.r and 0xFF).toByte()
            payload[offset + 3] = (pt.g and 0xFF).toByte()
            payload[offset + 4] = (pt.b and 0xFF).toByte()
            for (k in 0..4) {
                csum = (csum.toInt() xor payload[offset + k].toInt()).toByte()
            }
        }
        payload[3 + count * 5]     = csum
        payload[3 + count * 5 + 1] = TAIL
        return payload
    }

    /**
     * Projection Start — [0xAA, 0x10, frameCount, width, height, delayHigh, delayLow, 0x55]
     */
    fun buildProjectionStartFrame(
        frameCount: Int,
        width: Int = 64,
        height: Int = 32,
        frameDelayMs: Int = 200
    ): ByteArray {
        return byteArrayOf(
            HEADER, CMD_PROJ_START,
            frameCount.toByte(),
            width.toByte(),
            height.toByte(),
            ((frameDelayMs shr 8) and 0xFF).toByte(),
            (frameDelayMs and 0xFF).toByte(),
            TAIL
        )
    }

    /**
     * Projection Chunk — [0xAA, 0x11, chunkIdxH, chunkIdxL, dataLen, <data...>, 0x55]
     */
    fun buildProjectionChunkFrame(chunkIndex: Int, data: ByteArray): ByteArray {
        val dataLen = data.size.coerceAtMost(240)
        val frame = ByteArray(5 + dataLen + 1)
        frame[0] = HEADER
        frame[1] = CMD_PROJ_CHUNK
        frame[2] = ((chunkIndex shr 8) and 0xFF).toByte()
        frame[3] = (chunkIndex and 0xFF).toByte()
        frame[4] = dataLen.toByte()
        data.copyInto(frame, 5, 0, dataLen)
        frame[5 + dataLen] = TAIL
        return frame
    }

    /**
     * Projection Finish — [0xAA, 0x12, activateNow, 0x00, 0x00, checksum, 0x55]
     */
    fun buildProjectionFinishFrame(activateNow: Boolean = true): ByteArray {
        return buildFrame(CMD_PROJ_FINISH, if (activateNow) 1 else 0)
    }
}
```

---

## 6. CarLedBleManager.kt — Lớp quản lý BLE

```kotlin
// app/src/main/java/com/carled/app/bluetooth/CarLedBleManager.kt
package com.carled.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// --------------- State Model ---------------
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning     : ConnectionState()
    object Connecting   : ConnectionState()
    object Connected    : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class DeviceStatus(
    val power: Boolean = false,
    val brightness: Int = 180,
    val mode: Int = 0,
    val speed: Int = 50,
    val r: Int = 0, val g: Int = 150, val b: Int = 255,
    val freeHeap: Int = 0
)

// --------------- Manager ---------------
@SuppressLint("MissingPermission")
class CarLedBleManager(private val context: Context) {

    companion object {
        private const val TAG = "CarLedBLE"
        private const val SCAN_TIMEOUT_MS = 10_000L
        private const val MTU_TARGET = 256
        private const val DISCOVER_DELAY_MS = 100L
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _deviceStatus = MutableStateFlow(DeviceStatus())
    val deviceStatus = _deviceStatus.asStateFlow()

    private val _telemetryEvent = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val telemetryEvent = _telemetryEvent.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    private var lastSendTimestamp = 0L
    private val throttleIntervalMs = BleConstants.THROTTLE_INTERVAL_MS

    // ===================== SCAN =====================

    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null || bluetoothAdapter?.isEnabled != true) {
            _connectionState.value = ConnectionState.Error("Bluetooth chưa được bật")
            return
        }
        _connectionState.value = ConnectionState.Scanning

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)

        scanJob?.cancel()
        scanJob = scope.launch {
            delay(SCAN_TIMEOUT_MS)
            stopScan()
            if (_connectionState.value is ConnectionState.Scanning)
                _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanJob?.cancel()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.i(TAG, "Tìm thấy: ${result.device.name} @ ${result.device.address}")
            stopScan()
            connect(result.device)
        }
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan thất bại: $errorCode")
            _connectionState.value = ConnectionState.Error("Scan failed: $errorCode")
        }
    }

    // ===================== CONNECT =====================

    fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        cleanupGatt()
        bluetoothGatt = device.connectGatt(
            context,
            false,                          // autoConnect = false: kết nối tức thì
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "GATT Error $status — dọn dẹp và reset")
                cleanupGatt()
                _connectionState.value = ConnectionState.Error("GATT Error $status")
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Đã kết nối BLE, đang yêu cầu MTU $MTU_TARGET...")
                    _connectionState.value = ConnectionState.Connecting
                    gatt.requestMtu(MTU_TARGET)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "Thiết bị ngắt kết nối")
                    cleanupGatt()
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "MTU đã được thỏa thuận: $mtu bytes")
            // Delay nhỏ trước discoverServices để BLE stack ổn định
            Handler(Looper.getMainLooper()).postDelayed({
                gatt.discoverServices()
            }, DISCOVER_DELAY_MS)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = gatt.getService(BleConstants.SERVICE_UUID)
            if (service == null) {
                _connectionState.value = ConnectionState.Error("Không tìm thấy LED Service")
                return
            }

            writeCharacteristic = service.getCharacteristic(BleConstants.WRITE_CHAR_UUID)
            writeCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

            // Đăng ký Notify cho Status Characteristic
            val statusChar = service.getCharacteristic(BleConstants.STATUS_CHAR_UUID)
            if (statusChar != null) {
                gatt.setCharacteristicNotification(statusChar, true)
                statusChar.getDescriptor(BleConstants.CCCD_UUID)?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
            }

            _connectionState.value = ConnectionState.Connected
            Log.i(TAG, "✅ Sẵn sàng giao tiếp với Car LED!")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val json = String(characteristic.value, Charsets.UTF_8)
            Log.d(TAG, "Telemetry: $json")
            parseTelemetry(json)
            scope.launch { _telemetryEvent.emit(json) }
        }
    }

    // ===================== SEND COMMANDS =====================

    /**
     * Gửi frame nhị phân với rate-limiting (chống nghẽn GATT khi dùng slider)
     */
    fun sendThrottled(frame: ByteArray) {
        val now = System.currentTimeMillis()
        if (now - lastSendTimestamp < throttleIntervalMs) return
        lastSendTimestamp = now
        sendRaw(frame)
    }

    /**
     * Gửi frame nhị phân tức thì (cho các lệnh không phải slider)
     */
    fun sendCommand(frame: ByteArray) = sendRaw(frame)

    private fun sendRaw(bytes: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val char = writeCharacteristic ?: return
        char.value = bytes
        gatt.writeCharacteristic(char)
    }

    /**
     * Gửi JSON string qua BLE (cho marquee, wifi-config, preset phức tạp)
     */
    fun sendJson(json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        sendRaw(bytes)
    }

    // ===================== DISCONNECT =====================

    fun disconnect() {
        cleanupGatt()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun cleanupGatt() {
        bluetoothGatt?.let {
            it.disconnect()
            it.close()          // Bắt buộc phải gọi close() để tránh lỗi GATT 133
        }
        bluetoothGatt = null
        writeCharacteristic = null
    }

    // ===================== TELEMETRY PARSING =====================

    private fun parseTelemetry(json: String) {
        runCatching {
            val obj = org.json.JSONObject(json)
            _deviceStatus.value = DeviceStatus(
                power      = obj.optBoolean("power", false),
                brightness = obj.optInt("brightness", 180),
                mode       = obj.optInt("mode", 0),
                speed      = obj.optInt("speed", 50),
                r          = obj.optJSONObject("color")?.optInt("r", 0) ?: 0,
                g          = obj.optJSONObject("color")?.optInt("g", 150) ?: 150,
                b          = obj.optJSONObject("color")?.optInt("b", 255) ?: 255,
                freeHeap   = obj.optInt("free_heap", 0)
            )
        }.onFailure { Log.w(TAG, "Telemetry parse error: ${it.message}") }
    }
}
```

---

## 7. Cấu trúc Thư mục Project Android (Clean Architecture)

```
app/
└── src/main/
    ├── java/com/carled/app/
    │   ├── bluetooth/
    │   │   ├── BleConstants.kt              # UUID, hằng số giao thức
    │   │   ├── BleProtocolHelper.kt         # Bộ tạo khung lệnh nhị phân
    │   │   └── CarLedBleManager.kt          # Singleton quản lý kết nối BLE
    │   │
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── EffectMode.kt            # Enum mode đèn
    │   │   │   ├── DeviceStatus.kt          # Data class trạng thái thiết bị
    │   │   │   └── TouchPoint.kt            # Điểm chạm cho canvas
    │   │   └── repository/
    │   │       └── LedRepository.kt         # Interface trừu tượng hóa BLE
    │   │
    │   ├── ui/
    │   │   ├── MainActivity.kt              # Entry point, permission handling
    │   │   ├── scan/
    │   │   │   ├── ScanFragment.kt          # Màn hình quét & kết nối
    │   │   │   └── ScanViewModel.kt
    │   │   ├── control/
    │   │   │   ├── ControlFragment.kt       # Màn hình điều khiển chính
    │   │   │   └── ControlViewModel.kt
    │   │   ├── canvas/
    │   │   │   ├── CanvasFragment.kt        # Live drawing canvas
    │   │   │   ├── CanvasViewModel.kt
    │   │   │   └── LedCanvasView.kt         # Custom View 64x32 pixel
    │   │   ├── marquee/
    │   │   │   ├── MarqueeFragment.kt       # Nhập & gửi văn bản chạy chữ
    │   │   │   └── MarqueeViewModel.kt
    │   │   └── upload/
    │   │       ├── UploadFragment.kt        # Upload ảnh/animation lên ESP32
    │   │       └── UploadViewModel.kt
    │   │
    │   └── utils/
    │       ├── PermissionHelper.kt          # Xử lý quyền Android 6–14
    │       └── ColorConverter.kt            # RGB <-> HSV <-> RGB565
    │
    └── res/
        ├── layout/                          # XML layouts
        ├── navigation/nav_graph.xml         # Navigation Component
        └── values/
            ├── strings.xml
            └── colors.xml
```

---

## 8. Xử lý Quyền (Permission) — PermissionHelper.kt

```kotlin
// utils/PermissionHelper.kt
object PermissionHelper {

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Android 6.0–11
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun hasAllPermissions(context: Context): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
```

---

## 9. Luồng JSON Commands (Dùng cho cấu hình & Marquee)

### 9.1. Các JSON command phổ biến

```kotlin
// Bật/tắt
fun buildPowerJson(on: Boolean) = """{"power": $on}"""

// Chỉnh độ sáng
fun buildBrightnessJson(brightness: Int) = """{"brightness": $brightness}"""

// Chọn mode + speed
fun buildModeJson(mode: Int, speed: Int = 60) = """{"mode": $mode, "speed": $speed}"""

// Set màu RGB
fun buildColorJson(r: Int, g: Int, b: Int) = """
    {"mode": 1, "color": {"r": $r, "g": $g, "b": $b}}
""".trimIndent()

// Cấu hình Marquee Text
fun buildMarqueeJson(
    text: String,
    r: Int = 255, g: Int = 215, b: Int = 0,
    speed: Int = 60,
    size: Int = 1,          // 1 = nhỏ (5x7px), 2 = lớn (10x14px)
    yPos: Int = -1,         // -1 = auto center
    rainbow: Boolean = false,
    scrollMode: String = "scroll_left"  // "scroll_left" | "static" | "blink"
) = """
    {
      "type": "marquee",
      "text": "$text",
      "color": {"r": $r, "g": $g, "b": $b},
      "speed": $speed,
      "size": $size,
      "y_pos": $yPos,
      "mode": "$scrollMode",
      "rainbow": $rainbow
    }
""".trimIndent()

// Wi-Fi Config qua BLE
fun buildWifiConfigJson(ssid: String, password: String) = """
    {"command": "wifi_config", "ssid": "$ssid", "password": "$password"}
""".trimIndent()

// Canvas clear
fun buildCanvasClearJson() = """{"command": "canvas_clear"}"""

// Reload projection từ Flash
fun buildReloadProjectionJson() = """{"command": "reload_projection"}"""
```

### 9.2. Telemetry Status từ ESP32 (Notify)
```json
{
  "power": true,
  "brightness": 180,
  "mode": 6,
  "speed": 60,
  "color": {"r": 0, "g": 150, "b": 255},
  "free_heap": 182400
}
```

---

## 10. Live Canvas — LedCanvasView.kt

```kotlin
// ui/canvas/LedCanvasView.kt
class LedCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val COLS = 64
    private val ROWS = 32
    private val pixelBitmap = Bitmap.createBitmap(COLS, ROWS, Bitmap.Config.ARGB_8888)
    private val pixelCanvas = Canvas(pixelBitmap)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var onTouchBatch: ((List<TouchPoint>) -> Unit)? = null
    private var currentColor = Color.WHITE
    private val batchBuffer = mutableListOf<TouchPoint>()
    private var lastFlushTime = 0L
    private val FLUSH_INTERVAL_MS = 40L // 25 FPS batch

    fun setDrawColor(color: Int) { currentColor = color }

    fun setPixel(x: Int, y: Int, color: Int) {
        if (x in 0 until COLS && y in 0 until ROWS) {
            pixelBitmap.setPixel(x, y, color)
            invalidate()
        }
    }

    fun clearCanvas() {
        pixelCanvas.drawColor(Color.BLACK)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cellW = width.toFloat() / COLS
        val cellH = height.toFloat() / ROWS
        // Scale up pixel bitmap
        val destRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(pixelBitmap, null, destRect, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cellW = width.toFloat() / COLS
        val cellH = height.toFloat() / ROWS
        val px = (event.x / cellW).toInt().coerceIn(0, COLS - 1)
        val py = (event.y / cellH).toInt().coerceIn(0, ROWS - 1)

        val r = Color.red(currentColor)
        val g = Color.green(currentColor)
        val b = Color.blue(currentColor)

        setPixel(px, py, currentColor)
        batchBuffer.add(TouchPoint(px, py, r, g, b))

        // Flush batch mỗi 40ms
        val now = System.currentTimeMillis()
        if (now - lastFlushTime >= FLUSH_INTERVAL_MS) {
            val toFlush = batchBuffer.toList()
            batchBuffer.clear()
            lastFlushTime = now
            onTouchBatch?.invoke(toFlush)
        }

        return true
    }
}
```

---

## 11. Image Upload Flow (Projection — Flow 2)

### Quy trình gửi ảnh lên ESP32 SPIFFS:

```
1. Người dùng chọn ảnh từ Gallery
2. App resize ảnh về 64x32 pixel
3. Convert sang định dạng RGB565 (2 bytes/pixel)
4. Tính tổng kích thước: 64 × 32 × 2 = 4096 bytes
5. Gửi frame PROJECTION_START [0x10]
6. Chia data thành chunks ≤ 240 bytes
7. Gửi từng chunk với frame PROJECTION_CHUNK [0x11]
8. Gửi frame PROJECTION_FINISH [0x12]
9. Nhận Telemetry xác nhận: {"event":"upload_complete","status":"ok","mode":6}
```

```kotlin
// Trong UploadViewModel.kt
suspend fun uploadImage(bitmap: Bitmap) {
    // 1. Scale bitmap về 64x32
    val scaled = Bitmap.createScaledBitmap(bitmap, 64, 32, true)

    // 2. Convert to RGB565 ByteArray
    val rgb565Data = ByteArray(64 * 32 * 2)
    var idx = 0
    for (y in 0 until 32) {
        for (x in 0 until 64) {
            val px = scaled.getPixel(x, y)
            val r = (Color.red(px) shr 3) and 0x1F
            val g = (Color.green(px) shr 2) and 0x3F
            val b = (Color.blue(px) shr 3) and 0x1F
            val rgb565 = (r shl 11) or (g shl 5) or b
            rgb565Data[idx++] = ((rgb565 shr 8) and 0xFF).toByte()
            rgb565Data[idx++] = (rgb565 and 0xFF).toByte()
        }
    }

    // 3. Gửi Projection Start
    bleManager.sendCommand(
        BleProtocolHelper.buildProjectionStartFrame(
            frameCount = 1,
            width = 64,
            height = 32,
            frameDelayMs = 200
        )
    )
    delay(200)

    // 4. Gửi từng chunk
    val chunkSize = 240
    var chunkIndex = 0
    var offset = 0
    while (offset < rgb565Data.size) {
        val end = minOf(offset + chunkSize, rgb565Data.size)
        val chunk = rgb565Data.copyOfRange(offset, end)
        bleManager.sendCommand(
            BleProtocolHelper.buildProjectionChunkFrame(chunkIndex, chunk)
        )
        delay(30) // Đủ thời gian cho BLE stack xử lý
        offset = end
        chunkIndex++
    }

    // 5. Gửi Projection Finish
    delay(100)
    bleManager.sendCommand(BleProtocolHelper.buildProjectionFinishFrame(activateNow = true))
}
```

---

## 12. Danh sách Màn hình UI & Navigation

### Nav Graph:
```
ScanFragment
    └── [Kết nối thành công] ──► ControlFragment (Bottom Nav)
                                        ├── ControlTab (Home)
                                        ├── CanvasTab (Live Draw)
                                        ├── MarqueeTab (Chạy chữ)
                                        └── UploadTab (Upload ảnh)
```

### Màn hình 1 — ScanFragment
- Nút "Bắt đầu quét" → gọi `startScan()`
- Hiển thị trạng thái: Scanning / Connecting / Error
- Animation pulsing khi đang scan

### Màn hình 2 — ControlFragment (Màn hình chính)
| UI Element | Hành động | Protocol |
|---|---|---|
| Power Switch | Toggle ON/OFF | Binary `0x01` |
| Brightness SeekBar | Vuốt 0–255 | Binary `0x02` (Throttled) |
| Speed SeekBar | Vuốt 1–100 | Binary `0x03` (Throttled) |
| Mode Selector | Chọn trong 8 mode | Binary `0x04` |
| Color Wheel | Chọn màu HSV | Binary `0x05` (Throttled) |
| Sensitivity SeekBar | Chỉnh mic (Music mode) | Binary `0x06` (Throttled) |
| Status Card | Hiển thị telemetry | Nhận từ Notify |

### Màn hình 3 — CanvasFragment
- Custom `LedCanvasView` 64×32 pixel
- Color Picker nhỏ góc dưới
- Nút "Xóa canvas" → `0x15` + clear local bitmap
- Batch Drawing tự động mỗi 40ms

### Màn hình 4 — MarqueeFragment
- TextField nhập văn bản
- Lựa chọn màu, size, tốc độ, chế độ scroll
- Toggle Rainbow mode
- Nút "Phát" → gửi JSON marquee

### Màn hình 5 — UploadFragment
- Nút chọn ảnh từ Gallery
- Preview ảnh đã scale về 64×32
- Progress bar khi upload
- Nhận event `upload_complete` từ Telemetry

---

## 13. Các lỗi BLE Android phổ biến & Giải pháp

| Lỗi | Nguyên nhân | Giải pháp Đã Implement |
|---|---|---|
| **GATT Error 133** | BLE stack bị nghẽn, không gọi `close()` | Luôn gọi `disconnect()` + `close()` trong `cleanupGatt()` |
| **Scan throttling** | Quét quá 5 lần/30s | Dùng `ScanFilter` theo `SERVICE_UUID`, timeout 10s |
| **Slider lag 1–2s** | Flood 60–120 lệnh/giây vào GATT queue | Throttle 40ms (~25 FPS) |
| **Permission crash** (Android 12+) | Thiếu `BLUETOOTH_SCAN/CONNECT` | Check & request runtime permission theo API level |
| **MTU fragment JSON** | JSON lớn bị cắt nhỏ thành nhiều GATT write | `requestMtu(256)` trước `discoverServices()` |
| **GATT callback on Binder thread** | Crash khi update UI trong callback | Dùng `StateFlow` / `SharedFlow` → collect trên `Main` |

---

## 14. Luồng Kết nối End-to-End (từ `docs/android_ble_guide.md`)

```
User mở App
    → checkPermissions()
    → startScan(filter: SERVICE_UUID, SCAN_MODE_LOW_LATENCY)
    → [10s timeout] hoặc onScanResult()
    → stopScan()                    ← QUAN TRỌNG: Dừng trước khi connect
    → connect(device, autoConnect=false)
    → onConnectionStateChange(CONNECTED)
    → requestMtu(256)
    → onMtuChanged()
    → delay(100ms) + discoverServices()
    → onServicesDiscovered()
    → getCharacteristic(WRITE_UUID) → setWriteType(NO_RESPONSE)
    → enableNotification(STATUS_UUID)
    → State = CONNECTED ✅

User vuốt Color Wheel:
    → sendThrottled(buildRgbColorFrame(r, g, b))   [Rate 40ms]
    → writeCharacteristic(WRITE_TYPE_NO_RESPONSE)
    → ESP32 render LED real-time (< 30ms)
```

---

## 15. Kiểm tra Tích hợp (Integration Checklist)

- [ ] Firmware ESP32 đang quảng bá tên `CarLED-Ambient` với Service UUID `4fafc201-...`
- [ ] `ScanFilter` dùng đúng `SERVICE_UUID`
- [ ] `stopScan()` được gọi trước `connectGatt()`
- [ ] `gatt.requestMtu(256)` được gọi trong `onConnectionStateChange(CONNECTED)`
- [ ] `discoverServices()` được gọi trong `onMtuChanged()`
- [ ] `writeCharacteristic.writeType = WRITE_TYPE_NO_RESPONSE`
- [ ] Throttle 40ms áp dụng cho slider & color wheel
- [ ] `cleanupGatt()` gọi cả `disconnect()` và `close()`
- [ ] JSON marquee encoding: không dùng ký tự tiếng Việt có dấu (ESP32 auto-strip)
- [ ] Projection chunk delay ≥ 30ms giữa các gói
- [ ] `CCCD_UUID = 00002902-0000-1000-8000-00805f9b34fb` đúng cho Notify

---

## 16. Mock Data Tham khảo (từ `protocols/mock-data/`)

```json
// power_toggle.json
{"power": true}

// static_color.json
{"mode": 1, "color": {"r": 0, "g": 200, "b": 255}, "brightness": 200}

// rainbow_wave.json
{"mode": 3, "speed": 65, "brightness": 180}

// music_reactive.json
{"mode": 5, "sensitivity": 75, "brightness": 200}

// custom_projection.json
{"command": "reload_projection"}
```

---

## 17. Tóm tắt Dependencies Khuyến nghị

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "1.9.22"
coroutines = "1.7.3"
lifecycle = "2.7.0"
navigation = "2.7.6"
coil = "2.5.0"

[libraries]
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
lifecycle-runtime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
navigation-fragment = { module = "androidx.navigation:navigation-fragment-ktx", version.ref = "navigation" }
navigation-ui = { module = "androidx.navigation:navigation-ui-ktx", version.ref = "navigation" }
gson = { module = "com.google.code.gson:gson", version = "2.10.1" }
coil = { module = "io.coil-kt:coil", version.ref = "coil" }
```

---

*Tài liệu này được tổng hợp từ:*
- `docs/android_ble_guide.md` — Android BLE quirks & production code
- `docs/api_protocol.md` — BLE UUIDs & JSON command format
- `protocols/commands.md` — Binary protocol specification
- `include/config.h` — Firmware constants & UUIDs
- `include/types.h` — EffectMode enum & data structures
- `src/network/BleManager.h/.cpp` — ESP32-side GATT server implementation
