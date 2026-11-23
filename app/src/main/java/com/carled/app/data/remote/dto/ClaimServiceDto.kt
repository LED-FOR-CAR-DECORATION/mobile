package com.carled.app.data.remote.dto

data class ClaimDeviceRequest(
    val qrPayload: String,
    val deviceName: String? = null
)

data class ClaimDeviceResponse(
    val deviceId: Long,
    val serialNumber: String,
    val macAddress: String,
    val deviceName: String,
    val modelId: Long? = null,
    val modelName: String? = null,
    val boundAt: String? = null,
    val warrantyExpiresAt: String? = null
)