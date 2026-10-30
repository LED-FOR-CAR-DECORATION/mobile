package com.carled.app.domain.model

/**
 * Sealed class mô tả tất cả các Event JSON nhận từ ESP32 qua BLE Notify.
 *
 * Quy tắc phân biệt:
 *  - Nếu JSON có field "event" → đây là một Event (dùng sealed class bên dưới)
 *  - Nếu JSON KHÔNG có field "event" → đây là Status Telemetry định kỳ (cập nhật DeviceStatus)
 */
sealed class TelemetryEvent {
    data class OtaInfo(val requestId: String, val deviceId: String, val version: String, val hardware: String,
        val nonce: String, val wifi: Boolean, val configured: Boolean, val busy: Boolean, val healthy: Boolean) : TelemetryEvent()
    data class OtaStatus(val requestId: String, val state: String, val progress: Int, val error: String) : TelemetryEvent()
    data class DeviceName(val requestId: String, val name: String, val error: String) : TelemetryEvent()
    data class CanvasBatchApplied(val sequence: Int) : TelemetryEvent()

    /** ESP32 bắt đầu nhận gói ảnh/animation, expected = số bytes dự kiến */
    data class UploadStarted(val expected: Int) : TelemetryEvent()

    /** Upload hoàn tất thành công, mode = chế độ kích hoạt sau upload */
    data class UploadComplete(val status: String, val mode: Int) : TelemetryEvent()

    /** Upload thất bại, error = mã lỗi phần cứng (flash_write_fail, etc.) */
    data class UploadError(val error: String) : TelemetryEvent()

    /** Marquee text đang được phát trên LED */
    data class MarqueeActive(val text: String) : TelemetryEvent()

    /** ESP32 đang kết nối WiFi đến SSID chỉ định */
    data class WifiConnecting(val ssid: String) : TelemetryEvent()

    /** ESP32 đã kết nối WiFi thành công và có địa chỉ IP */
    data class WifiConnected(val ip: String) : TelemetryEvent()

    /** ESP32 bị mất kết nối WiFi */
    object WifiDisconnected : TelemetryEvent()

    /** Cảnh báo nút dừng khẩn cấp (E-Stop) */
    data class EmergencyStop(val active: Boolean) : TelemetryEvent()

    /** Projection đã được reload thành công từ Flash SPIFFS */
    data class ProjectionReloaded(val mode: Int) : TelemetryEvent()

    /** Canvas đã được xóa sạch về màu đen */
    object CanvasCleared : TelemetryEvent()

    /** Event không nhận dạng được (để debug) */
    data class Unknown(val raw: String) : TelemetryEvent()
}

/**
 * Status Telemetry định kỳ — nhận khi JSON KHÔNG có field "event".
 */
data class DeviceTelemetryStatus(
    val power: Boolean = false,
    val brightness: Int = 180,
    val mode: Int = 0,
    val speed: Int = 50,
    val r: Int = 0,
    val g: Int = 150,
    val b: Int = 255,
    val freeHeap: Int = 0,
    val isEmergencyStop: Boolean = false,
    val wifiIp: String = ""
)
