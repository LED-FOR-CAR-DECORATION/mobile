package com.carled.app.presentation.screens.firmware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.di.ServiceLocator
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.TelemetryEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.UUID

data class FirmwareState(
    val info: TelemetryEvent.OtaInfo?=null, val releaseVersion: String?=null,
    val busy: Boolean=false, val updating: Boolean=false, val message: String="", val error: String?=null, val progress: Int=0
)

class FirmwareViewModel : ViewModel() {
    private val ble=ServiceLocator.bleManager
    private val api=ServiceLocator.backendClient
    private val preferences=ServiceLocator.userPreferences
    private val mutable=MutableStateFlow(FirmwareState())
    val state=mutable.asStateFlow()
    private var updateId=""
    private var terminal: CompletableDeferred<TelemetryEvent.OtaStatus>?=null
    init {
        viewModelScope.launch {
            ble.telemetryEvent.collect { event ->
                if (event is TelemetryEvent.OtaStatus && event.requestId==updateId) {
                    mutable.update { it.copy(progress=event.progress,message=when(event.state) {
                        "starting" -> "LED đang xác thực bản cập nhật…"
                        "downloading" -> "LED đang tải firmware: ${event.progress}%"
                        "rebooting" -> "LED đang khởi động lại…"
                        else -> it.message
                    }) }
                    if(event.state=="error" || event.state=="rebooting") terminal?.complete(event)
                }
            }
        }
        checkUpdate()
    }
    private fun id()=UUID.randomUUID().toString().take(8)
    private suspend fun info(): TelemetryEvent.OtaInfo = coroutineScope {
        val requestId=id()
        val reply=async(start=CoroutineStart.UNDISPATCHED) {
            withTimeout(7000) { ble.telemetryEvent.first { it is TelemetryEvent.OtaInfo && it.requestId==requestId } as TelemetryEvent.OtaInfo }
        }
        try { ble.sendJson(JSONObject().put("command","get_ota_info").put("request_id",requestId).toString()); reply.await() }
        finally { reply.cancel() }
    }
    private fun action(block: suspend () -> Unit) {
        if(mutable.value.busy) return
        mutable.update { it.copy(busy=true,error=null) }
        viewModelScope.launch {
            try { block() }
            catch (e: TimeoutCancellationException) { mutable.update { it.copy(error="Chưa xác nhận được kết quả từ LED. Kết nối lại rồi kiểm tra phiên bản; không ngắt nguồn khi LED đang cập nhật.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error=e.message ?: "Không thể cập nhật firmware") } }
            finally { mutable.update { it.copy(busy=false,updating=false) } }
        }
    }
    fun checkUpdate()=action {
        val device=info()
        mutable.update { it.copy(info=device,releaseVersion=null,message="Firmware hiện tại: ${device.version}") }
        val pending=preferences.pendingFirmware(device.deviceId)
        if(pending!=null) {
            if(device.version==pending && device.healthy) {
                preferences.savePendingFirmware(device.deviceId,null)
                mutable.update { it.copy(message="Đã xác nhận LED chạy ổn định phiên bản $pending.") }
            } else if(device.busy || !device.healthy) {
                mutable.update { it.copy(message="LED đang cập nhật hoặc kiểm tra sau khởi động. Hãy kiểm tra lại sau.") }; return@action
            } else {
                mutable.update { it.copy(message="LED vẫn chạy ${device.version}; chưa xác nhận bản $pending. Có thể cập nhật chưa hoàn tất hoặc đã khôi phục bản cũ.") }
            }
        }
        check(device.configured) { "LED chưa được cấu hình máy chủ OTA/CA. Cần nạp bản hỗ trợ OTA qua USB lần đầu." }
        val data=api.request("/api/devices/${device.deviceId}/ota/check?currentVersion=${java.net.URLEncoder.encode(device.version,"UTF-8")}")
        val release=if(data.optBoolean("updateAvailable")) data.getJSONObject("release").getString("version") else null
        mutable.update { it.copy(releaseVersion=release,message=if(pending!=null) it.message else if(release==null) "LED đang dùng bản phát hành hiện tại." else "Có bản cập nhật $release.") }
    }
    fun connectWifi(ssid: String,password: String)=action {
        require(ssid.toByteArray().size in 1..32 && password.toByteArray().size<=63) { "SSID tối đa 32 byte; mật khẩu tối đa 63 byte." }
        ble.sendJson(JSONObject().put("command","wifi_config").put("ssid",ssid).put("password",password).toString())
        mutable.update { it.copy(message="Đang kết nối LED với Wi-Fi/hotspot…") }
        val device=withTimeout(40000) {
            var value: TelemetryEvent.OtaInfo
            do { delay(2000); value=info() } while(!value.wifi)
            value
        }
        mutable.update { it.copy(info=device,message="LED đã kết nối Wi-Fi.") }
    }
    fun install()=action {
        val connection=ble.connectionState.value as? ConnectionState.Connected ?: error("Hãy kết nối LED trước.")
        val device=info()
        check(device.wifi && device.configured && !device.busy) { "LED cần kết nối Wi-Fi và sẵn sàng cập nhật." }
        val session=api.request("/api/devices/${device.deviceId}/ota/sessions","POST",JSONObject().put("currentVersion",device.version).put("nonce",device.nonce))
        val target=session.getString("version")
        preferences.savePendingFirmware(device.deviceId,target)
        updateId=id(); val completion=CompletableDeferred<TelemetryEvent.OtaStatus>(); terminal=completion
        mutable.update { it.copy(updating=true,message="Đang gửi yêu cầu cập nhật…",progress=0) }
        try {
            ble.sendJson(JSONObject().put("command","ota_start").put("request_id",updateId).put("token",session.getString("token")).toString())
            // Connection loss is expected around reboot. Poll the same MAC to
            // recover even if the final reboot notification was not delivered.
            val result=withTimeoutOrNull(310000) {
                while(!completion.isCompleted) {
                    delay(2000)
                    if(ble.connectionState.value !is ConnectionState.Connected) break
                }
                if(completion.isCompleted) completion.await() else null
            }
            if(result?.state=="error") error("LED từ chối cập nhật: ${result.error}. Kiểm tra Wi-Fi và thử lại.")
            mutable.update { it.copy(message="Đang kết nối lại và xác minh firmware…") }
            val verified=withTimeout(90000) {
                delay(2500)
                var current: TelemetryEvent.OtaInfo?=null
                while(current==null || current.busy || !current.healthy || current.version!=target) {
                    if(ble.connectionState.value !is ConnectionState.Connected && ble.connectionState.value !is ConnectionState.Connecting)
                        ble.connect(connection.deviceAddress)
                    delay(2500)
                    if(ble.connectionState.value is ConnectionState.Connected) {
                        current=try { info() } catch (_: TimeoutCancellationException) { null }
                        if(current!=null) check(current.deviceId==device.deviceId) { "Thiết bị kết nối lại không khớp." }
                    }
                }
                current
            }
            preferences.savePendingFirmware(device.deviceId,null)
            mutable.update { it.copy(info=verified,releaseVersion=null,progress=100,message="Cập nhật thành công. LED đã chạy ổn định phiên bản $target.") }
        } finally { terminal=null }
    }
}
