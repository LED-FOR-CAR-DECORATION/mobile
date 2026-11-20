package com.carled.app.data.repository

import android.util.Log
import com.carled.app.data.ble.BleProtocolHelper
import com.carled.app.data.ble.CarLedBleManager
import com.carled.app.data.ble.ConfirmedCanvasSender
import com.carled.app.data.ble.TouchPoint
import com.carled.app.domain.model.ble.BleDeviceModel
import com.carled.app.domain.model.ble.ConnectionState
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
    private val bleManager: CarLedBleManager,
    /** Composed from DeviceRepository::isMacOwned at the DI root (ServiceLocator)
     *  to avoid this repository depending on DeviceRepository's type directly. */
    private val isMacOwned: suspend (String) -> Boolean
) : BleDeviceRepository {

    companion object {
        private const val TAG = "BleDeviceRepository"
    }

    // Owned internally now (not a passthrough) so a rejected connection can be
    // overridden with an Error state instead of surfacing bleManager's raw Connected.
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override val scannedDevices: StateFlow<List<BleDeviceModel>> = bleManager.scannedDevices
    override val telemetryStatus: StateFlow<DeviceTelemetryStatus> = bleManager.telemetryStatus
    override val telemetryEvent: SharedFlow<TelemetryEvent> = bleManager.telemetryEvent

    private val _ledState = MutableStateFlow(LedControlState())
    override val ledState: StateFlow<LedControlState> = _ledState.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Set right after we force-disconnect an unowned device, so the Disconnected
    // event that bleManager emits a moment later doesn't clobber the Error message
    // we just surfaced. Cleared on the next Disconnected we see.
    private var suppressNextDisconnected = false

    init {
        repositoryScope.launch {
            bleManager.connectionState.collect { state ->
                when {
                    state is ConnectionState.Connected -> {
                        val owned = try {
                            isMacOwned(state.deviceAddress)
                        } catch (e: Exception) {
                            Log.w(TAG, "Không kiểm tra được quyền sở hữu thiết bị: ${e.message}")
                            false
                        }
                        if (owned) {
                            _connectionState.value = state
                        } else {
                            Log.w(TAG, "Từ chối kết nối: MAC ${state.deviceAddress} không thuộc tài khoản hiện tại.")
                            suppressNextDisconnected = true
                            _connectionState.value = ConnectionState.Error(
                                "Thiết bị BLE này không thuộc tài khoản của bạn. Vui lòng quét mã QR để thêm thiết bị trước."
                            )
                            bleManager.disconnect()
                        }
                    }
                    state is ConnectionState.Disconnected && suppressNextDisconnected -> {
                        suppressNextDisconnected = false
                        // Giữ nguyên thông báo lỗi vừa phát ra, không bị Disconnected ghi đè.
                    }
                    else -> _connectionState.value = state
                }
            }
        }
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

    override suspend fun clearCanvas() {
        bleManager.sendCommand(BleProtocolHelper.buildCanvasClearFrame(), throttled = false)
    }

    override suspend fun reloadProjection() {
        bleManager.sendJson(BleProtocolHelper.buildReloadProjectionJson())
    }

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
            delay(25)
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