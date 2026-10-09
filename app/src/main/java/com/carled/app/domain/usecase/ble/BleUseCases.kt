package com.carled.app.domain.usecase.ble

import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.LedControlState
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import com.carled.app.domain.repository.BleDeviceRepository
import kotlinx.coroutines.flow.StateFlow

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

data class BleUseCases(
    val scanDevices: ScanDevicesUseCase,
    val stopScan: StopScanUseCase,
    val connectDevice: ConnectDeviceUseCase,
    val disconnectDevice: DisconnectDeviceUseCase,
    val sendColorCommand: SendColorCommandUseCase,
    val sendModeCommand: SendModeCommandUseCase,
    val sendPowerCommand: SendPowerCommandUseCase,
    val sendBrightnessCommand: SendBrightnessCommandUseCase
)
