package com.carled.app.data.repository

import android.content.SharedPreferences
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.dto.*
import com.carled.app.domain.model.TelemetryEvent
import com.google.gson.Gson
import retrofit2.Response
import kotlinx.coroutines.sync.Mutex

data class PendingFirmware(
    val firmwareId: Long,
    val version: String,
    val mac: String,
    val deviceIdentity: String,
    val requestId: String
) {
    fun isConfirmedBy(info: TelemetryEvent.OtaInfo): Boolean =
        info.deviceId == deviceIdentity && info.version == version && info.healthy && !info.busy
}

class FirmwareRepository(
    private val api: ApiService,
    private val preferences: UserPreferences,
    private val pendingStore: SharedPreferences,
    private val gson: Gson
) {
    private val operationLock = Mutex()

    suspend fun coordinate(block: suspend () -> Unit) {
        check(operationLock.tryLock()) { "Một thao tác firmware khác đang chạy. Hãy chờ rồi thử lại." }
        try {
            block()
        } finally {
            operationLock.unlock()
        }
    }

    fun accountId(): String = checkNotNull(preferences.getUserId()?.takeIf { it.isNotBlank() }) {
        "Bạn cần đăng nhập để cập nhật firmware."
    }

    private fun authorization(account: String): String {
        check(accountId() == account) { "Tài khoản đã thay đổi. Hãy mở lại màn hình thiết bị." }
        return "Bearer " + checkNotNull(preferences.getAuthToken()) { "Bạn cần đăng nhập lại." }
    }

    suspend fun check(deviceId: Long, version: String?, account: String): FirmwareCheck =
        body(api.checkFirmware(authorization(account), deviceId, version)).data

    suspend fun createSession(deviceId: Long, target: Long, nonce: String, account: String): FirmwareSession {
        require(nonce.matches(Regex("[0-9a-f]{32}"))) { "Nonce thiết bị không hợp lệ. Hãy kiểm tra lại." }
        return body(api.createFirmwareSession(authorization(account), deviceId, CreateOtaSessionRequest(target, nonce))).data
    }

    suspend fun confirm(deviceId: Long, pending: PendingFirmware, info: TelemetryEvent.OtaInfo, account: String) {
        check(pending.isConfirmedBy(info)) { "Thiết bị chưa xác nhận firmware mới hoạt động ổn định." }
        val response = api.reportFirmware(authorization(account), deviceId, ReportFirmwareRequest(pending.firmwareId))
        check(response.isSuccessful) { errorMessage(response) }
        savePending(deviceId, account, null)
    }

    fun pending(deviceId: Long, account: String): PendingFirmware? {
        val json = pendingStore.getString("$account/$deviceId", null) ?: return null
        return gson.fromJson(json, PendingFirmware::class.java)
    }

    fun savePending(deviceId: Long, account: String, pending: PendingFirmware?) {
        val editor = pendingStore.edit()
        val key = "$account/$deviceId"
        if (pending == null) editor.remove(key) else editor.putString(key, gson.toJson(pending))
        check(editor.commit()) { "Không lưu được trạng thái cập nhật trên điện thoại." }
    }

    private fun <T> body(response: Response<T>): T {
        check(response.isSuccessful) { errorMessage(response) }
        return checkNotNull(response.body()) { "Máy chủ trả về dữ liệu trống." }
    }

    private fun errorMessage(response: Response<*>): String {
        val error = runCatching {
            gson.fromJson(response.errorBody()?.string(), ApiErrorResponse::class.java)
        }.getOrNull()
        return error?.resolvedMessage()?.takeIf { it.isNotBlank() }
            ?: "Không thể cập nhật firmware (HTTP ${response.code()})."
    }
}
