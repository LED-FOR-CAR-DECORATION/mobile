package com.carled.app.domain.model

enum class BleScanBlocker(val message: String) {
    PERMISSION("Ứng dụng chưa có quyền quét Bluetooth."),
    UNSUPPORTED("Điện thoại không hỗ trợ Bluetooth."),
    BLUETOOTH_OFF("Bluetooth đang tắt. Bật Bluetooth để tìm thiết bị LED."),
    LOCATION_OFF("Vị trí đang tắt. Android 6–11 cần bật dịch vụ Vị trí để nhận kết quả quét BLE.")
}

fun bleScanBlocker(sdk: Int, permissionGranted: Boolean, bluetoothSupported: Boolean,
    bluetoothEnabled: Boolean, locationEnabled: Boolean): BleScanBlocker? = when {
    !bluetoothSupported -> BleScanBlocker.UNSUPPORTED
    !permissionGranted -> BleScanBlocker.PERMISSION
    !bluetoothEnabled -> BleScanBlocker.BLUETOOTH_OFF
    sdk < 31 && !locationEnabled -> BleScanBlocker.LOCATION_OFF
    else -> null
}
