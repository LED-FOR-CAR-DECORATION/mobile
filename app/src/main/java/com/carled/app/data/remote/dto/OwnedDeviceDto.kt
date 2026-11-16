package com.carled.app.data.remote.dto

import com.google.gson.JsonElement

data class ApiResponse<T>(
    val data: T,
    val meta: JsonElement?
)

data class DeviceDto(
    val deviceId: Long,
    val serialNumber: String,
    val macAddress: String,
    val deviceName: String,
    val status: String,
    val modelId: Long?,
    val modelName: String?,
    val widthPx: Int?,
    val heightPx: Int?,
    val currentFirmwareId: Long?,
    val boundAt: String?,
    val warrantyExpiresAt: String?,
    val warrantyStatus: String?
)

data class DeviceDetailDto(
    val deviceId: Long,
    val serialNumber: String,
    val macAddress: String,
    val deviceName: String,
    val status: String,
    val currentFirmwareId: Long?,
    val boundAt: String?,
    val warrantyExpiresAt: String?,
    val warrantyStatus: String?,
    val model: DeviceModelDto?,
    val fulfillment: Any?
)

data class DeviceModelDto(
    val modelId: Long,
    val productId: Long,
    val modelName: String,
    val status: String,
    val spec: com.google.gson.JsonElement? = null
)