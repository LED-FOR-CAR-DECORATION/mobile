package com.carled.app.core.network

/**
 * Cấu hình kết nối mạng và API Gateway của hệ thống.
 */
object NetworkConfig {
    /**
     * Địa chỉ mặc định cho Android Emulator kết nối với localhost máy tính host:
     * - Android Emulator: 10.0.2.2:8080
     * - Máy thật: cần trỏ tới IP LAN của máy tính chạy backend Spring Boot (ví dụ: 192.168.1.x:8080)
     */
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8080/"
    const val LOCALHOST_BASE_URL = "http://127.0.0.1:8080/"

    // Mặc định sử dụng EMULATOR_BASE_URL cho Android development
    var activeBaseUrl: String = EMULATOR_BASE_URL

    const val TIMEOUT_CONNECT_SECONDS = 15L
    const val TIMEOUT_READ_SECONDS = 20L
    const val TIMEOUT_WRITE_SECONDS = 20L
}
