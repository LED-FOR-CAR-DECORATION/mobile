package com.carled.app.data.remote.dto

data class ClaimDeviceRequest(
    val v: Int,
    val sn: String,
    val mac: String,
    val sig: String
)

data class ClaimDeviceResponse(
    val id: String,
    val deviceName: String,
    val mac: String,
    val sn: String
)