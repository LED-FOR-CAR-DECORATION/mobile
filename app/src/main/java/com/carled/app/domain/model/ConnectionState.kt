package com.carled.app.domain.model

/**
 * Trạng thái kết nối BLE của ứng dụng đối với ESP32 LED.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String, val deviceAddress: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
