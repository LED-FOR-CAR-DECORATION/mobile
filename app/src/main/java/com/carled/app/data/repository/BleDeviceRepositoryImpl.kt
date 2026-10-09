package com.carled.app.data.repository

import com.carled.app.data.ble.BleProtocolHelper
import com.carled.app.data.ble.CarLedBleManager
import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.LedControlState
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import com.carled.app.domain.repository.BleDeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleDeviceRepositoryImpl(
    private val bleManager: CarLedBleManager
) : BleDeviceRepository {

    override val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    override val scannedDevices: StateFlow<List<BleDeviceModel>> = bleManager.scannedDevices

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
        bleManager.sendCommand(frame, throttled = true)
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
        bleManager.sendCommand(frame, throttled = true)
        _ledState.value = _ledState.value.copy(sensitivity = sensitivity)
    }
}
