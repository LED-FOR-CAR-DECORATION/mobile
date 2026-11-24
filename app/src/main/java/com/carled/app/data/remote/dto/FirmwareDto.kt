package com.carled.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FirmwareTarget(
    val firmwareId: Long,
    val version: String,
    val changelog: String? = null,
    val fileSizeBytes: Long,
    val sha256: String
)

data class FirmwareCheck(
    val deviceId: Long,
    val currentVersion: String?,
    val currentVersionStatus: String,
    val updateAvailable: Boolean,
    @SerializedName(value = "isRollback", alternate = ["rollback"])
    val isRollback: Boolean,
    val targetFirmware: FirmwareTarget?
)

data class CreateOtaSessionRequest(val firmwareId: Long, val nonce: String)
data class ReportFirmwareRequest(val firmwareId: Long)

class FirmwareSession(
    val sessionId: Long,
    val token: String,
    val expiresAt: String,
    val firmware: FirmwareTarget
) {
    override fun toString() = "FirmwareSession(sessionId=$sessionId)"
}
