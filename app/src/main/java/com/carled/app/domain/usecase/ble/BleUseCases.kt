package com.carled.app.domain.usecase.ble

import com.carled.app.data.ble.TouchPoint
import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.DeviceTelemetryStatus
import com.carled.app.domain.model.LedControlState
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.repository.BleDeviceRepository
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// ──────── Scan / Connect ────────

class ScanDevicesUseCase(private val repository: BleDeviceRepository) {
    operator fun invoke() {
        repository.startScan()
    }
}

class StopScanUseCase(private val repository: BleDeviceRepository) {
    operator fun invoke() {
        repository.stopScan()
    }
}

class ConnectDeviceUseCase(private val repository: BleDeviceRepository) {
    operator fun invoke(macAddress: String) {
        repository.connect(macAddress)
    }
}

class DisconnectDeviceUseCase(private val repository: BleDeviceRepository) {
    operator fun invoke() {
        repository.disconnect()
    }
}

class GetScannedDevicesUseCase(private val repository: BleDeviceRepository) {
    operator fun invoke(): StateFlow<List<BleDeviceModel>> = repository.scannedDevices
}

class GetConnectionStateUseCase(private val repository: BleDeviceRepository) {
    operator fun invoke(): StateFlow<ConnectionState> = repository.connectionState
}

class GetLedStateUseCase(private val repository: BleDeviceRepository) {
    operator fun invoke(): StateFlow<LedControlState> = repository.ledState
}

// ──────── Basic LED Control ────────

class SendColorCommandUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(color: RgbColor, throttled: Boolean = true) {
        repository.setColor(color, throttled)
    }
}

class SendModeCommandUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(mode: LedMode, speed: Int = 50) {
        repository.setMode(mode, speed)
    }
}

class SendPowerCommandUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(on: Boolean) {
        repository.setPower(on)
    }
}

class SendBrightnessCommandUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(level: Int) {
        repository.setBrightness(level)
    }
}

class SendSensitivityCommandUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(sensitivity: Int) {
        repository.setSensitivity(sensitivity)
    }
}

// ──────── Marquee / WiFi / Canvas ────────

class SendMarqueeUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(
        text: String,
        r: Int = 255, g: Int = 215, b: Int = 0,
        speed: Int = 60,
        size: Int = 1,
        rainbow: Boolean = false,
        scrollMode: String = "scroll_left"
    ) {
        repository.sendMarquee(text, r, g, b, speed, size, rainbow, scrollMode)
    }
}

class SendWifiConfigUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(ssid: String, password: String) {
        repository.sendWifiConfig(ssid, password)
    }
}

class ClearCanvasUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke() {
        repository.clearCanvas()
    }
}

class ReloadProjectionUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke() {
        repository.reloadProjection()
    }
}

class UploadProjectionUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(rgb565Data: ByteArray, frameCount: Int = 1, frameDelayMs: Int = 200) {
        require(frameCount in 1..255 && frameDelayMs in 1..65535)
        require(rgb565Data.size == 64 * 32 * 2 * frameCount)
        if (frameCount == 1 && frameDelayMs == 200) repository.uploadProjection(rgb565Data)
        else repository.uploadAnimation(rgb565Data, frameCount, frameDelayMs)
    }
}

// ──────── Telemetry Accessors ────────

class GetTelemetryStatusUseCase(private val repository: BleDeviceRepository) {
    operator fun invoke(): StateFlow<DeviceTelemetryStatus> = repository.telemetryStatus
}

class GetTelemetryEventUseCase(private val repository: BleDeviceRepository) {
    operator fun invoke(): SharedFlow<TelemetryEvent> = repository.telemetryEvent
}

class SendSinglePixelUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(x: Int, y: Int, r: Int, g: Int, b: Int) {
        repository.sendSinglePixel(x, y, r, g, b)
    }
}

class SendBatchDrawUseCase(private val repository: BleDeviceRepository) {
    suspend operator fun invoke(points: List<TouchPoint>) {
        repository.sendBatchDraw(points)
    }
}

// ──────── Aggregate ────────

data class BleUseCases(
    // Scan / Connect
    val scanDevices: ScanDevicesUseCase,
    val stopScan: StopScanUseCase,
    val connectDevice: ConnectDeviceUseCase,
    val disconnectDevice: DisconnectDeviceUseCase,
    val getScannedDevices: GetScannedDevicesUseCase,
    val getConnectionState: GetConnectionStateUseCase,
    val getLedState: GetLedStateUseCase,
    // Basic LED
    val sendColorCommand: SendColorCommandUseCase,
    val sendModeCommand: SendModeCommandUseCase,
    val sendPowerCommand: SendPowerCommandUseCase,
    val sendBrightnessCommand: SendBrightnessCommandUseCase,
    val sendSensitivityCommand: SendSensitivityCommandUseCase,
    // Advanced features
    val sendMarquee: SendMarqueeUseCase,
    val sendWifiConfig: SendWifiConfigUseCase,
    val clearCanvas: ClearCanvasUseCase,
    val reloadProjection: ReloadProjectionUseCase,
    val uploadProjection: UploadProjectionUseCase,
    // Telemetry
    val getTelemetryStatus: GetTelemetryStatusUseCase,
    val getTelemetryEvent: GetTelemetryEventUseCase,
    // Canvas draw
    val sendSinglePixel: SendSinglePixelUseCase,
    val sendBatchDraw: SendBatchDrawUseCase
)
