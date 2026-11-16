package com.carled.app.data.repository

import com.carled.app.data.ble.BleProtocolHelper
import com.carled.app.data.ble.CarLedBleManager
import com.carled.app.data.ble.ConfirmedCanvasSender
import com.carled.app.data.ble.TouchPoint
import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.DeviceTelemetryStatus
import com.carled.app.domain.model.LedControlState
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.repository.BleDeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BleDeviceRepositoryImpl(
    private val bleManager: CarLedBleManager
) : BleDeviceRepository {

    override val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    override val scannedDevices: StateFlow<List<BleDeviceModel>> = bleManager.scannedDevices
    override val telemetryStatus: StateFlow<DeviceTelemetryStatus> = bleManager.telemetryStatus
    override val telemetryEvent: SharedFlow<TelemetryEvent> = bleManager.telemetryEvent

    private val _ledState = MutableStateFlow(LedControlState())
    override val ledState: StateFlow<LedControlState> = _ledState.asStateFlow()

    // Lifetime matches the singleton repository — never cancelled.
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Mirror periodic telemetry into _ledState so the UI self-corrects when
        // the LED changes state externally: physical button, another device, power
        // cycle with different firmware defaults, etc.
        //
        // ControlViewModel collects ledState via collectLatest + 300 ms delay, so
        // in-flight drag commands always win: a continuous slider drag triggers
        // many rapid command-driven updates, each restarting collectLatest's timer
        // before telemetry can land. The 300 ms settle only fires once dragging stops.
        repositoryScope.launch {
            bleManager.telemetryStatus.collect { status ->
                _ledState.update { current ->
                    current.copy(
                        isPowerOn = status.power,
                        brightness = status.brightness,
                        currentMode = LedMode.fromCode(status.mode),
                        speed = status.speed,
                        currentColor = RgbColor(status.r, status.g, status.b),
                        freeHeap = status.freeHeap
                    )
                }
            }
        }
    }

    override suspend fun renameDevice(name: String) = bleManager.renameDevice(name)

    override fun startScan() { bleManager.startScan() }
    override fun stopScan() { bleManager.stopScan() }
    override fun connect(macAddress: String) { bleManager.connect(macAddress) }
    override fun disconnect() { bleManager.disconnect() }

    override suspend fun setPower(on: Boolean) {
        bleManager.sendCommand(BleProtocolHelper.buildPowerFrame(on), throttled = false)
        _ledState.update { it.copy(isPowerOn = on) }
    }

    override suspend fun setBrightness(level: Int) {
        bleManager.sendCommand(BleProtocolHelper.buildBrightnessFrame(level), throttled = false)
        _ledState.update { it.copy(brightness = level) }
    }

    override suspend fun setMode(mode: LedMode, speed: Int) {
        bleManager.sendCommand(BleProtocolHelper.buildModeFrame(mode.code), throttled = false)
        bleManager.sendCommand(BleProtocolHelper.buildSpeedFrame(speed), throttled = false)
        _ledState.update { it.copy(currentMode = mode, speed = speed) }
    }

    override suspend fun setColor(color: RgbColor, throttled: Boolean) {
        bleManager.sendCommand(
            BleProtocolHelper.buildRgbColorFrame(color.red, color.green, color.blue),
            throttled = throttled
        )
        _ledState.update { it.copy(currentColor = color) }
    }

    override suspend fun setSensitivity(sensitivity: Int) {
        bleManager.sendCommand(BleProtocolHelper.buildSensitivityFrame(sensitivity), throttled = false)
        _ledState.update { it.copy(sensitivity = sensitivity) }
    }

    override suspend fun sendMarquee(
        text: String,
        r: Int, g: Int, b: Int,
        speed: Int,
        size: Int,
        rainbow: Boolean,
        scrollMode: String
    ) {
        bleManager.sendJson(BleProtocolHelper.buildMarqueeJson(text, r, g, b, speed, size, -1, rainbow, scrollMode))
        _ledState.update { it.copy(currentMode = LedMode.MARQUEE_TEXT) }
    }

    override suspend fun sendWifiConfig(ssid: String, password: String) {
        bleManager.sendJson(BleProtocolHelper.buildWifiConfigJson(ssid, password))
    }

    /**
     * Clears the LED canvas by sending CMD_CANVAS_CLEAR (0x15).
     * Previously this called liveSender.clear() which sent an empty CMD_DRAW_CONFIRMED
     * (0x1A) packet — the firmware does not treat that as a clear command and the
     * ack wait would time out and throw.
     */
    override suspend fun clearCanvas() {
        bleManager.sendCommand(BleProtocolHelper.buildCanvasClearFrame(), throttled = false)
    }

    override suspend fun reloadProjection() {
        bleManager.sendJson(BleProtocolHelper.buildReloadProjectionJson())
    }

    /**
     * Upload ảnh RGB565 lên ESP32 theo Binary Projection Protocol:
     *   1. Gửi PROJECTION_START [0x10]
     *   2. Gửi từng PROJECTION_CHUNK [0x11] (tối đa 128 bytes/chunk — well within 240-byte limit)
     *   3. Gửi PROJECTION_FINISH [0x12]
     */
    override suspend fun uploadProjection(rgb565Data: ByteArray) = uploadAnimation(rgb565Data, 1, 200)

    override suspend fun uploadAnimation(rgb565Data: ByteArray, frameCount: Int, frameDelayMs: Int) {
        require(frameCount in 1..255 && frameDelayMs in 1..65535)
        require(rgb565Data.size == 64 * 32 * 2 * frameCount) { "Invalid projection frame length" }

        bleManager.sendCommand(
            BleProtocolHelper.buildProjectionStartFrame(
                frameCount = frameCount, width = 64, height = 32, frameDelayMs = frameDelayMs
            ),
            throttled = false
        )
        delay(200)

        val chunkSize = 128
        var chunkIndex = 0
        var offset = 0
        while (offset < rgb565Data.size) {
            val end = minOf(offset + chunkSize, rgb565Data.size)
            val chunk = rgb565Data.copyOfRange(offset, end)
            bleManager.sendCommand(BleProtocolHelper.buildProjectionChunkFrame(chunkIndex, chunk), throttled = false)
            delay(25) // Tối ưu BLE throughput đồng bộ với ble_image_importer.html
            offset = end
            chunkIndex++
        }

        delay(100)
        bleManager.sendCommand(BleProtocolHelper.buildProjectionFinishFrame(activateNow = true), throttled = false)
    }

    override suspend fun sendSinglePixel(x: Int, y: Int, r: Int, g: Int, b: Int) {
        bleManager.sendCommand(BleProtocolHelper.buildDrawPixelFrame(x, y, r, g, b), throttled = false)
    }

    private val liveSender = ConfirmedCanvasSender(
        events = telemetryEvent,
        write = { frame -> bleManager.sendCommand(frame, throttled = false) }
    )

    override suspend fun sendBatchDraw(points: List<TouchPoint>) {
        liveSender.send(points)
    }
}