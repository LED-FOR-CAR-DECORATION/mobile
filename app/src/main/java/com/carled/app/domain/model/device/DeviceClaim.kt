package com.carled.app.domain.model.device

import android.util.Log
import org.json.JSONException
import org.json.JSONObject

data class DeviceClaimPayload(
    val version: Int,
    val serialNumber: String,
    val macAddress: String,
    val signature: String,
    val qrPayload: String
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
        val obj = when (val wrapped = root.opt("qrPayload")) {
            is JSONObject -> wrapped
            is String -> JSONObject(wrapped)
            else -> root
        }
        if (!obj.has("sn") || !obj.has("mac") || !obj.has("sig")) return QrDecodeResult.NotADevicePayload

        val sn = obj.getString("sn").trim()
        val mac = obj.getString("mac").trim()
        val sig = obj.getString("sig").trim()
        if (sn.isEmpty() || mac.isEmpty() || sig.isEmpty()) {
            return QrDecodeResult.Malformed("Mã QR thiếu thông tin (sn/mac/sig).")
        }

        val version = obj.optInt("v", 1)
        val compactQr = JSONObject().apply {
            put("v", version)
            put("sn", sn)
            put("mac", mac)
            put("sig", sig)
        }.toString()

        Log.d("DeviceClaimParser", "Parsed QR successfully -> SN: $sn | MAC: $mac | SIG: $sig")
        Log.d("DeviceClaimParser", "Serialized compact qrPayload string: $compactQr")

        QrDecodeResult.Success(
            DeviceClaimPayload(
                version = version,
                serialNumber = sn,
                macAddress = mac,
                signature = sig,
                qrPayload = compactQr
            )
        )
    } catch (e: JSONException) {
        QrDecodeResult.NotADevicePayload
    }
}