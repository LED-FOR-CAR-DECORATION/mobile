package com.carled.app.domain.model

data class OwnedDevice(
    val id: String,
    val deviceName: String,
    val macAddress: String,
    val serialNumber: String,
    val firmwareVersion: String?,
    val lastUpdated: String?
)
