package com.carled.app.domain.repository

import com.carled.app.data.ble.TouchPoint
import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.DeviceTelemetryStatus
import com.carled.app.domain.model.LedControlState
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import com.carled.app.domain.model.TelemetryEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BleDeviceRepository {
    val connectionState: StateFlow<ConnectionState>
    val scannedDevices: StateFlow<List<BleDeviceModel>>
    val ledState: StateFlow<LedControlState>

    /** Trạng thái thiết bị cập nhật định kỳ từ ESP32 (khi JSON không có field "event") */
    val telemetryStatus: StateFlow<DeviceTelemetryStatus>

    /** Event một lần từ ESP32 (upload, marquee, wifi, canvas events) */
    val telemetryEvent: SharedFlow<TelemetryEvent>

    suspend fun renameDevice(name: String)

    fun startScan()
    fun stopScan()
    fun connect(macAddress: String)
    fun disconnect()

    suspend fun setPower(on: Boolean)
    suspend fun setBrightness(level: Int)
    suspend fun setMode(mode: LedMode, speed: Int = 50)
    suspend fun setColor(color: RgbColor, throttled: Boolean = true)
    suspend fun setSensitivity(sensitivity: Int)

    /** Gửi văn bản chạy chữ qua JSON */
    suspend fun sendMarquee(
        text: String,
        r: Int = 255, g: Int = 215, b: Int = 0,
        speed: Int = 60,
        size: Int = 1,
        rainbow: Boolean = false,
        scrollMode: String = "scroll_left"
    )

    /** Gửi cấu hình WiFi qua BLE JSON */
    suspend fun sendWifiConfig(ssid: String, password: String)

    /** Xóa canvas LED */
    suspend fun clearCanvas()

    /** Reload projection từ Flash SPIFFS */
    suspend fun reloadProjection()

    /** Upload ảnh lên ESP32 (Binary Projection Protocol) */
    suspend fun uploadProjection(rgb565Data: ByteArray)

    /** Consecutive full64×32 RGB565 frames, played by firmware without phone timers. */
    suspend fun uploadAnimation(rgb565Data: ByteArray, frameCount: Int, frameDelayMs: Int)

    /** Gửi một điểm vẽ đơn lẻ qua BLE */
    suspend fun sendSinglePixel(x: Int, y: Int, r: Int, g: Int, b: Int)

    /** Gửi batch điểm vẽ gộp (tối đa 12 điểm/gói, chờ xác nhận ESP32, tần suất 25 FPS) */
    suspend fun sendBatchDraw(points: List<TouchPoint>)
}
