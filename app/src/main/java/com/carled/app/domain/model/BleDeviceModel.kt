package com.carled.app.domain.model

/**
 * Domain entity đại diện cho thiết bị LED được quét thấy qua BLE.
 */
data class BleDeviceModel(
    val name: String,
    val macAddress: String,
    val rssi: Int = 0,
    val isBonded: Boolean = false
)
