package com.carled.app.data.repository

import com.carled.app.data.ble.BleProtocolHelper
import com.carled.app.data.ble.CarLedBleManager
import com.carled.app.data.ble.TouchPoint
import com.carled.app.data.ble.ConfirmedCanvasSender
import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.DeviceTelemetryStatus
import com.carled.app.domain.model.LedControlState
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.repository.BleDeviceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleDeviceRepositoryImpl(
    private val bleManager: CarLedBleManager
) : BleDeviceRepository {

    override val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    override val scannedDevices: StateFlow<List<BleDeviceModel>> = bleManager.scannedDevices
    override val telemetryStatus: StateFlow<DeviceTelemetryStatus> = bleManager.telemetryStatus
    override val telemetryEvent: SharedFlow<TelemetryEvent> = bleManager.telemetryEvent

    private val _ledState = MutableStateFlow(LedControlState())
    override val ledState: StateFlow<LedControlState> = _ledState.asStateFlow()

    override fun startScan() {
        bleManager.startScan()
    }

    override fun stopScan() {
        bleManager.stopScan()
    }

    override fun connect(macAddress: String) {
        bleManager.connect(macAddress)
    }

    override fun disconnect() {
        bleManager.disconnect()
    }

    override suspend fun setPower(on: Boolean) {
        val frame = BleProtocolHelper.buildPowerFrame(on)
        bleManager.sendCommand(frame, throttled = false)
        _ledState.value = _ledState.value.copy(isPowerOn = on)
    }

    override suspend fun setBrightness(level: Int) {
        val frame = BleProtocolHelper.buildBrightnessFrame(level)
        bleManager.sendCommand(frame, throttled = false)
        _ledState.value = _ledState.value.copy(brightness = level)
    }

    override suspend fun setMode(mode: LedMode, speed: Int) {
        val frame = BleProtocolHelper.buildModeFrame(mode.code)
        bleManager.sendCommand(frame, throttled = false)
        val speedFrame = BleProtocolHelper.buildSpeedFrame(speed)
        bleManager.sendCommand(speedFrame, throttled = false)
        _ledState.value = _ledState.value.copy(currentMode = mode, speed = speed)
    }

    override suspend fun setColor(color: RgbColor, throttled: Boolean) {
        val frame = BleProtocolHelper.buildRgbColorFrame(color.red, color.green, color.blue)
        bleManager.sendCommand(frame, throttled = throttled)
        _ledState.value = _ledState.value.copy(currentColor = color)
    }

    override suspend fun setSensitivity(sensitivity: Int) {
        val frame = BleProtocolHelper.buildSensitivityFrame(sensitivity)
        bleManager.sendCommand(frame, throttled = false)
        _ledState.value = _ledState.value.copy(sensitivity = sensitivity)
    }

    override suspend fun sendMarquee(
        text: String,
        r: Int, g: Int, b: Int,
        speed: Int,
        size: Int,
        rainbow: Boolean,
        scrollMode: String
    ) {
        val json = BleProtocolHelper.buildMarqueeJson(text, r, g, b, speed, size, -1, rainbow, scrollMode)
        bleManager.sendJson(json)
        _ledState.value = _ledState.value.copy(currentMode = LedMode.MARQUEE_TEXT)
    }

    override suspend fun sendWifiConfig(ssid: String, password: String) {
        val json = BleProtocolHelper.buildWifiConfigJson(ssid, password)
        bleManager.sendJson(json)
    }

    override suspend fun clearCanvas() {
        liveSender.clear()
    }

    override suspend fun reloadProjection() {
        bleManager.sendJson(BleProtocolHelper.buildReloadProjectionJson())
    }

    /**
     * Upload ảnh RGB565 lên ESP32 theo Binary Projection Protocol:
     *   1. Gửi PROJECTION_START [0x10]
     *   2. Gửi từng PROJECTION_CHUNK [0x11] (tối đa 240 bytes/chunk)
     *   3. Gửi PROJECTION_FINISH [0x12]
     */
    override suspend fun uploadProjection(rgb565Data: ByteArray) = uploadAnimation(rgb565Data, 1, 200)

    override suspend fun uploadAnimation(rgb565Data: ByteArray, frameCount: Int, frameDelayMs: Int) {
        require(frameCount in 1..255 && frameDelayMs in 1..65535)
        require(rgb565Data.size == 64 * 32 * 2 * frameCount) { "Invalid projection frame length" }
        bleManager.sendCommand(
            BleProtocolHelper.buildProjectionStartFrame(frameCount = frameCount, width = 64, height = 32, frameDelayMs = frameDelayMs),
            throttled = false
        )
        delay(200)

        val chunkSize = 128
        var chunkIndex = 0
        var offset = 0
        while (offset < rgb565Data.size) {
            val end = minOf(offset + chunkSize, rgb565Data.size)
            val chunk = rgb565Data.copyOfRange(offset, end)
            bleManager.sendCommand(
                BleProtocolHelper.buildProjectionChunkFrame(chunkIndex, chunk),
                throttled = false
            )
            delay(25) // Tối ưu BLE throughput đồng bộ với ble_image_importer.html
            offset = end
            chunkIndex++
        }

        delay(100)
        bleManager.sendCommand(
            BleProtocolHelper.buildProjectionFinishFrame(activateNow = true),
            throttled = false
        )
    }

    override suspend fun sendSinglePixel(x: Int, y: Int, r: Int, g: Int, b: Int) {
        val frame = BleProtocolHelper.buildDrawPixelFrame(x, y, r, g, b)
        bleManager.sendCommand(frame, throttled = false)
    }

    private val liveSender = ConfirmedCanvasSender(telemetryEvent, { frame -> bleManager.sendCommand(frame, throttled = false) })
    override suspend fun sendBatchDraw(points: List<TouchPoint>) {
        liveSender.send(points)
    }
}
