package com.carled.app.core.common

import java.util.UUID

object Constants {
    // BLE Service & Characteristic UUIDs (khớp chuẩn firmware ESP32)
    val BLE_SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val BLE_WRITE_CHAR_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    val BLE_STATUS_CHAR_UUID: UUID = UUID.fromString("1c95d5e3-d8f7-413a-bf3d-7a2e5d7be87e")
    val BLE_CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val BLE_DEVICE_NAME_PREFIX = "CarLED"
    const val BLE_SCAN_TIMEOUT_MS = 10000L
    const val BLE_THROTTLE_INTERVAL_MS = 40L // ~25 FPS chống tràn buffer GATT
    const val BLE_DEFAULT_MTU = 256

    // REST API Endpoint cấu hình cho Flow 1 Commerce
    const val BASE_API_URL = "https://api.carled-decoration.com/api/v1/"
}
