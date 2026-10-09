package com.carled.app.domain.repository

import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.LedControlState
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import kotlinx.coroutines.flow.StateFlow

interface BleDeviceRepository {
    val connectionState: StateFlow<ConnectionState>
    val scannedDevices: StateFlow<List<BleDeviceModel>>
    val ledState: StateFlow<LedControlState>

    fun startScan()
    fun stopScan()
    fun connect(macAddress: String)
    fun disconnect()

    suspend fun setPower(on: Boolean)
    suspend fun setBrightness(level: Int)
    suspend fun setMode(mode: LedMode, speed: Int = 50)
    suspend fun setColor(color: RgbColor, throttled: Boolean = true)
    suspend fun setSensitivity(sensitivity: Int)
}
