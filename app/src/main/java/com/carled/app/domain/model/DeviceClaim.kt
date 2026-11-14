package com.carled.app.domain.model

import org.json.JSONException
import org.json.JSONObject

data class DeviceClaimPayload(
    val version: Int,
    val serialNumber: String,
    val macAddress: String,
    val signature: String
)

data class ClaimedDevice(
    val id: String,
    val deviceName: String,
    val macAddress: String,
    val serialNumber: String
)

sealed class QrDecodeResult {
    data class Success(val payload: DeviceClaimPayload) : QrDecodeResult()

    object NotADevicePayload : QrDecodeResult()

    data class Malformed(val reason: String) : QrDecodeResult()
}

fun parseDeviceClaimPayload(raw: String): QrDecodeResult {
    return try {
        val root = JSONObject(raw)
        val obj = if (root.has("qrPayload")) root.getJSONObject("qrPayload") else root
        if (!obj.has("sn") || !obj.has("mac") || !obj.has("sig")) return QrDecodeResult.NotADevicePayload

        val sn = obj.getString("sn").trim()
        val mac = obj.getString("mac").trim()
        val sig = obj.getString("sig").trim()
        if (sn.isEmpty() || mac.isEmpty() || sig.isEmpty()) {
            return QrDecodeResult.Malformed("Mã QR thiếu thông tin (sn/mac/sig).")
        }

        QrDecodeResult.Success(
            DeviceClaimPayload(
                version = obj.optInt("v", 1),
                serialNumber = sn,
                macAddress = mac,
                signature = sig
            )
        )
    } catch (e: JSONException) {
        QrDecodeResult.NotADevicePayload
    }
}