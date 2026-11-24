package com.carled.app.presentation.screens.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.data.ble.FirmwareBleClient
import com.carled.app.data.remote.dto.FirmwareCheck
import com.carled.app.data.repository.FirmwareRepository
import com.carled.app.data.repository.PendingFirmware
import com.carled.app.domain.model.TelemetryEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.time.Instant
import java.util.UUID

data class FirmwareUpdateState(
    val info: TelemetryEvent.OtaInfo? = null,
    val check: FirmwareCheck? = null,
    val working: Boolean = false,
    val pending: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val progress: Int? = null,
    val completed: Boolean = false
)

class FirmwareUpdateViewModel(
    private val deviceId: Long,
    private val mac: String,
    private val repository: FirmwareRepository,
    private val ble: FirmwareBleClient
) : ViewModel() {
    private val _state = MutableStateFlow(FirmwareUpdateState())
    val state = _state.asStateFlow()
    private var action: Job? = null

    init { checkForUpdate() }

    fun checkForUpdate() = runAction {
        val account = repository.accountId()
        val pending = repository.pending(deviceId, account)
        _state.update { it.copy(pending = pending != null, completed = false) }
        if (pending != null) {
            check(pending.mac.equals(mac, true)) { "Địa chỉ thiết bị đã thay đổi." }
            monitor(pending, account)
        } else {
            val info = if (ble.isConnected(mac)) ble.readInfo(mac) else null
            val result = repository.check(deviceId, info?.version, account)
            check(result.deviceId == deviceId) { "Máy chủ trả về sai thiết bị." }
            _state.update {
                it.copy(info = info, check = result, progress = null, message = null)
            }
        }
    }

    fun configureWifi(ssid: String, password: String) = runAction {
        val currentInfo = ble.readInfo(mac)
        if (currentInfo.wifi) {
            _state.update { it.copy(info = currentInfo, message = "Thiết bị đã kết nối Wi-Fi.") }
            return@runAction
        }
        ble.configureWifi(mac, ssid, password)
        _state.update { it.copy(info = currentInfo, message = "Đang chờ thiết bị kết nối Wi-Fi…") }
        withTimeout(60_000) {
            do {
                delay(3_000)
                val info = ble.readInfo(mac)
                _state.update { it.copy(info = info) }
            } while (!info.wifi)
        }
        _state.update { it.copy(message = "Thiết bị đã kết nối Wi-Fi.") }
    }

    fun update() = runAction {
        val account = repository.accountId()
        check(repository.pending(deviceId, account) == null) { "Hãy xác minh lần cập nhật trước trước khi thử lại." }
        val info = ble.readInfo(mac)
        _state.update { it.copy(info = info, completed = false) }
        check(!info.busy) { "Thiết bị đang cập nhật. Hãy chờ rồi kiểm tra lại." }
        check(info.healthy) { "Thiết bị vừa khởi động. Hãy chờ 15 giây rồi thử lại." }
        check(info.configured) { "Firmware thiết bị chưa được cấu hình máy chủ OTA và chứng chỉ HTTPS." }
        check(info.wifi) { "Hãy cấu hình Wi-Fi có Internet cho thiết bị." }
        val selected = checkNotNull(_state.value.check?.targetFirmware) { "Hãy kiểm tra bản cập nhật trước." }
        // Recheck on every tap. Deprecation may have changed the ACTIVE release.
        val fresh = repository.check(deviceId, info.version, account)
        _state.update { it.copy(check = fresh) }
        val target = checkNotNull(fresh.targetFirmware) { "Thiết bị đã dùng firmware đang được phát hành." }
        check(target.firmwareId == selected.firmwareId) { "Bản phát hành đã thay đổi. Hãy xem lại rồi cập nhật." }
        val session = repository.createSession(deviceId, target.firmwareId, info.nonce, account)
        check(session.firmware.firmwareId == target.firmwareId && session.firmware.version == target.version) {
            "Phiên tải firmware không khớp bản phát hành."
        }
        check(Instant.parse(session.expiresAt).isAfter(Instant.now())) { "Phiên cập nhật đã hết hạn. Hãy thử lại." }
        val pending = PendingFirmware(target.firmwareId, target.version, mac, info.deviceId, UUID.randomUUID().toString())
        repository.savePending(deviceId, account, pending)
        _state.update { it.copy(pending = true, progress = 0) }
        monitor(pending, account) {
            check(repository.accountId() == account) { "Tài khoản đã thay đổi." }
            ble.start(mac, session.token, pending.requestId)
        }
    }

    private suspend fun monitor(
        pending: PendingFirmware,
        account: String,
        start: (() -> Unit)? = null
    ) = coroutineScope {
        var deviceError: String? = null
        var rebooting = false
        val statuses = launch(start = CoroutineStart.UNDISPATCHED) {
            ble.events.collect { event ->
                if (event is TelemetryEvent.OtaStatus && event.requestId == pending.requestId && ble.isConnected(mac)) {
                    when (event.state) {
                        "error" -> deviceError = event.error.ifBlank { "unknown" }
                        "rebooting" -> rebooting = true
                    }
                    _state.update {
                        it.copy(
                            progress = event.progress,
                            message = when (event.state) {
                                "starting" -> "Đang bắt đầu cập nhật…"
                                "downloading" -> "Thiết bị đang tải firmware…"
                                "rebooting" -> "Thiết bị đang khởi động lại…"
                                else -> it.message
                            }
                        )
                    }
                }
            }
        }
        try {
            start?.invoke()
            _state.update { it.copy(message = "Đang xác minh firmware trên thiết bị…") }
            withTimeout(7 * 60_000L) {
                var attempts = 0
                while (true) {
                    check(repository.accountId() == account) { "Tài khoản đã thay đổi." }
                    deviceError?.let {
                        repository.savePending(deviceId, account, null)
                        _state.update { state -> state.copy(pending = false) }
                        error("Thiết bị dừng cập nhật ($it). Hãy kiểm tra lại trước khi thử.")
                    }
                    if (!ble.isConnected(mac)) {
                        _state.update { it.copy(message = "Đang kết nối lại Bluetooth để xác minh…") }
                        try {
                            ble.reconnect(mac)
                        } catch (_: TimeoutCancellationException) {
                            // Retry discovery after a GATT connection attempt stalls during reboot.
                        }
                    } else {
                        val info = try {
                            ble.readInfo(mac)
                        } catch (_: TimeoutCancellationException) {
                            null
                        }
                        if (info != null) {
                            _state.update { it.copy(info = info) }
                            check(info.deviceId == pending.deviceIdentity) { "Thông tin nhận được không thuộc thiết bị đang cập nhật." }
                            if (pending.isConfirmedBy(info)) {
                                _state.update { it.copy(message = "Đang ghi nhận phiên bản mới…") }
                                repository.confirm(deviceId, pending, info, account)
                                _state.update {
                                    it.copy(pending = false, completed = true, check = null, progress = 100,
                                        message = "Đã cập nhật firmware ${pending.version}.")
                                }
                                break
                            }
                            // Allow the start command and reboot to settle before declaring rollback.
                            if (!info.busy && info.healthy && info.version != pending.version &&
                                (start == null || rebooting || attempts >= 6)) {
                                repository.savePending(deviceId, account, null)
                                _state.update { it.copy(pending = false) }
                                error("Thiết bị đang chạy ${info.version}, chưa cài thành công ${pending.version}. Hãy kiểm tra lại.")
                            }
                        }
                    }
                    attempts++
                    delay(3_000)
                }
            }
        } finally {
            statuses.cancel()
        }
    }

    private fun runAction(block: suspend () -> Unit) {
        if (action?.isActive == true) return
        action = viewModelScope.launch {
            _state.update { it.copy(working = true, error = null) }
            try {
                repository.coordinate(block)
            } catch (_: TimeoutCancellationException) {
                _state.update { it.copy(error = "Chưa nhận được xác nhận từ thiết bị. Giữ thiết bị có nguồn và kết nối lại để xác minh.") }
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                _state.update { it.copy(error = "Không kết nối được máy chủ. Kiểm tra Internet và thử lại.") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Không thể cập nhật firmware.") }
            } finally {
                _state.update { it.copy(working = false) }
            }
        }
    }
}