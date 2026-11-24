package com.carled.app.data.ble

import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.model.ble.ConnectionState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

class FirmwareBleClient(private val manager: CarLedBleManager) {
    val connectionState get() = manager.connectionState
    val events get() = manager.telemetryEvent

    fun isConnected(mac: String): Boolean =
        (connectionState.value as? ConnectionState.Connected)?.deviceAddress.equals(mac, ignoreCase = true)

    suspend fun reconnect(mac: String) {
        check(connectionState.value !is ConnectionState.Connected) {
            "Đang kết nối với thiết bị khác. Hãy kết nối lại thiết bị cần cập nhật."
        }
        if (connectionState.value !is ConnectionState.Connecting) manager.connect(mac)
        try {
            withTimeout(20_000) {
                connectionState.first {
                    it is ConnectionState.Connected || it is ConnectionState.Error || it is ConnectionState.Disconnected
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            manager.disconnect()
            throw e
        }
    }

    suspend fun readInfo(mac: String): TelemetryEvent.OtaInfo = coroutineScope {
        check(isConnected(mac)) { "Hãy kết nối Bluetooth với thiết bị cần cập nhật." }
        val requestId = UUID.randomUUID().toString()
        val response = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(8_000) {
                events.first { it is TelemetryEvent.OtaInfo && it.requestId == requestId } as TelemetryEvent.OtaInfo
            }
        }
        try {
            // ESP32 dispatches the "command" field, including for OTA commands.
            manager.sendJson(JSONObject().put("command", "get_ota_info").put("request_id", requestId).toString())
            response.await().also {
                check(isConnected(mac)) { "Kết nối Bluetooth đã thay đổi." }
            }
        } finally {
            response.cancel()
        }
    }

    fun start(mac: String, token: String, requestId: String) {
        check(isConnected(mac)) { "Thiết bị đã mất kết nối Bluetooth." }
        require(token.matches(Regex("[0-9a-f]{64}"))) { "Token cập nhật không hợp lệ." }
        manager.sendJson(
            JSONObject().put("command", "ota_start").put("token", token).put("request_id", requestId).toString()
        )
    }

    fun configureWifi(mac: String, ssid: String, password: String) {
        check(isConnected(mac)) { "Thiết bị đã mất kết nối Bluetooth." }
        require(ssid.isNotBlank()) { "Nhập tên mạng Wi-Fi." }
        manager.sendJson(BleProtocolHelper.buildWifiConfigJson(ssid, password))
    }
}
