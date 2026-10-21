package com.carled.app.presentation.screens.marquee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.model.MarqueeLayout
import com.carled.app.domain.model.TextProfile
import com.carled.app.domain.model.TextProfilePlan
import com.carled.app.domain.model.TextProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first

data class TextProfilesState(val entries: List<TextProfile> = emptyList(), val plan: TextProfilePlan = TextProfilePlan.BASIC,
    val selectedId: String? = null, val busy: Boolean = false, val loaded: Boolean = false, val error: String? = null)

class MarqueeViewModel(private val bleUseCases: BleUseCases, private val profileStore: TextProfileRepository? = null) : ViewModel() {

    private val _uiState = MutableStateFlow(MarqueeUiState())
    val uiState: StateFlow<MarqueeUiState> = _uiState.asStateFlow()
    val connection = bleUseCases.getConnectionState()

    val hasProfiles = profileStore != null
    private val _profiles = MutableStateFlow(TextProfilesState())
    val profiles = _profiles.asStateFlow()
    init { reloadProfiles() }
    fun clearProfileError() { _profiles.update { it.copy(error = null) } }
    fun reloadProfiles() {
        val store=profileStore ?: return
        if (_profiles.value.busy) return
        _profiles.update { it.copy(busy=true,error=null) }
        viewModelScope.launch {
            try {
                val (entries,plan)=withContext(Dispatchers.IO) { store.list() to store.plan() }
                _profiles.update { it.copy(entries=entries,plan=plan,loaded=true,error=null) }
            } catch(e: CancellationException) { throw e }
            catch(e:Exception) { _profiles.update { it.copy(error="Không đọc được profile đã lưu. Hãy thử lại.",loaded=false) } }
            finally { _profiles.update { it.copy(busy=false) } }
        }
    }
    fun selectProfile(id:String) {
        if (_uiState.value.isSending || _profiles.value.busy) return
        val p=_profiles.value.entries.firstOrNull { it.id==id } ?: return
        _uiState.update { it.copy(text=p.text,size=p.size,speed=p.speed,scrollMode=p.mode,r=p.r,g=p.g,b=p.b,rainbow=p.rainbow,feedbackMessage="Đã nạp profile ${p.name}. Chọn Hiển thị trên LED để phát.") }
        _profiles.update { it.copy(selectedId=id,error=null) }
    }
    fun saveProfile(name:String, update:Boolean, onSaved:()->Unit = {}) {
        val store=profileStore ?: return
        val ps=_profiles.value
        if (ps.busy || !ps.loaded || _uiState.value.isSending) return
        val id=if(update) ps.selectedId ?: return else UUID.randomUUID().toString()
        val s=_uiState.value
        val p=TextProfile(id,name.trim(),MarqueeLayout.clean(s.text),s.size,s.speed,s.scrollMode,s.r,s.g,s.b,s.rainbow)
        try { p.validate() } catch(e:Exception) { _profiles.update { it.copy(error=e.message) };return }
        _profiles.update { it.copy(busy=true,error=null) }
        viewModelScope.launch {
            try {
                val (entries,plan)=withContext(Dispatchers.IO) { store.save(p);store.list() to store.plan() }
                _profiles.update { it.copy(entries=entries,plan=plan,selectedId=id) }
                _uiState.update { it.copy(feedbackMessage="Đã lưu profile ${p.name} trên điện thoại.") }
                onSaved()
            } catch(e:CancellationException) { throw e }
            catch(e:Exception) { _profiles.update { it.copy(error=e.message ?: "Không lưu được profile.") } }
            finally { _profiles.update { it.copy(busy=false) } }
        }
    }
    fun deleteProfile(id:String,onDeleted:()->Unit = {}) {
        val store=profileStore ?: return
        if (_profiles.value.busy || !_profiles.value.loaded || _uiState.value.isSending) return
        _profiles.update { it.copy(busy=true,error=null) }
        viewModelScope.launch {
            try {
                val entries=withContext(Dispatchers.IO) { store.delete(id);store.list() }
                _profiles.update { it.copy(entries=entries,selectedId=it.selectedId.takeUnless { value -> value==id }) }
                onDeleted()
            } catch(e:CancellationException) { throw e }
            catch(e:Exception) { _profiles.update { it.copy(error=e.message ?: "Không xóa được profile.") } }
            finally { _profiles.update { it.copy(busy=false) } }
        }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun updateSpeed(speed: Int) {
        _uiState.update { it.copy(speed = speed.coerceIn(1, 100)) }
    }

    fun updateSize(size: Int) {
        _uiState.update { it.copy(size = size.coerceIn(1, 2)) }
    }

    fun toggleRainbow() {
        _uiState.update { it.copy(rainbow = !it.rainbow) }
    }

    fun updateScrollMode(mode: String) {
        _uiState.update { it.copy(scrollMode = mode) }
    }

    fun updateColor(r: Int, g: Int, b: Int) {
        _uiState.update { it.copy(r = r, g = g, b = b) }
    }

    fun applyPreset(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun stopMarquee() {
        viewModelScope.launch {
            // Chuyển về Mode 1: Static Color
            bleUseCases.sendModeCommand(com.carled.app.domain.model.LedMode.STATIC_COLOR, 50)
            _uiState.update {
                it.copy(
                    lastEventText = null,
                    feedbackMessage = "Đã dừng chạy chữ (chuyển về Màu tĩnh)"
                )
            }
        }
    }

    fun sendMarquee() {
        val state = _uiState.value
        if (state.text.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Vui lòng nhập nội dung chữ") }
            return
        }
        if (state.isSending) return
        if (bleUseCases.getConnectionState().value !is com.carled.app.domain.model.ConnectionState.Connected) {
            _uiState.update { it.copy(feedbackMessage = "Kết nối LED trước khi gửi.") }; return
        }
        if (!state.text.all { it.code in 32..126 }) {
            _uiState.update { it.copy(feedbackMessage = "Vui lòng dùng ký tự tiếng Anh không dấu (ASCII).") }; return
        }
        val cleanText = MarqueeLayout.clean(state.text)
        val layout = MarqueeLayout.fit(cleanText, state.size)
        if (state.scrollMode != "scroll_left" && !layout.fits) {
            _uiState.update { it.copy(feedbackMessage = "Chữ vượt khung 64×32 ở cỡ 5×7. Rút ngắn còn tối đa 40 ký tự hoặc chọn Cuộn trái.") }; return
        }
        _uiState.update { it.copy(isSending = true, feedbackMessage = null) }
        viewModelScope.launch {
            try {
            coroutineScope {
            val static = state.scrollMode != "scroll_left"
            val blink = state.scrollMode == "blink"
            val ack = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(if (static) 30000L else 10000L) {
                bleUseCases.getTelemetryEvent().first {
                    if (static) it is TelemetryEvent.UploadComplete || it is TelemetryEvent.UploadError
                    else it is TelemetryEvent.MarqueeActive && it.text == cleanText
                }
            } }
            if (static) {
                val color = (0xff shl 24) or (state.r shl 16) or (state.g shl 8) or state.b
                if (blink) bleUseCases.uploadProjection(layout.projection(true, color), frameCount = 2, frameDelayMs = MarqueeLayout.BLINK_FRAME_DELAY_MS)
                else bleUseCases.uploadProjection(layout.projection(false, color))
            } else bleUseCases.sendMarquee(
                text       = cleanText,
                r          = state.r,
                g          = state.g,
                b          = state.b,
                speed      = state.speed,
                size       = state.size,
                rainbow    = state.rainbow,
                scrollMode = state.scrollMode
            )
            val event = ack.await()
            if (event is TelemetryEvent.UploadError) error("LED báo lỗi: ${event.error}")
            _uiState.update { it.copy(lastEventText = cleanText, feedbackMessage = if (static) "LED đã xác nhận ${if (blink) "chữ nhấp nháy" else "chữ tĩnh"} · ${layout.glyphLabel} · ${layout.lines.size} dòng." else "Thiết bị đã xác nhận hiển thị chữ.") }
            }
            } catch (e: TimeoutCancellationException) {
                _uiState.update { it.copy(feedbackMessage = "Chưa nhận xác nhận từ LED. Kiểm tra kết nối.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(feedbackMessage = "Không gửi được: ${e.message}") } }
            finally { _uiState.update { it.copy(isSending = false) } }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    companion object {
        fun removeVietnameseTones(str: String): String {
            var s = str
            s = s.replace("[àáạảãâầấậẩẫăằắặẳẵ]".toRegex(), "a")
            s = s.replace("[èéẹẻẽêềếệểễ]".toRegex(), "e")
            s = s.replace("[ìíịỉĩ]".toRegex(), "i")
            s = s.replace("[òóọỏõôồốộổỗơờớợởỡ]".toRegex(), "o")
            s = s.replace("[ùúụủũưừứựửữ]".toRegex(), "u")
            s = s.replace("[ỳýỵỷỹ]".toRegex(), "y")
            s = s.replace("đ", "d")
            s = s.replace("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]".toRegex(), "A")
            s = s.replace("[ÈÉẸẺẼÊỀẾỆỂỄ]".toRegex(), "E")
            s = s.replace("[ÌÍỊỈĨ]".toRegex(), "I")
            s = s.replace("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]".toRegex(), "O")
            s = s.replace("[ÙÚỤỦŨƯỪỨỰỬỮ]".toRegex(), "U")
            s = s.replace("[ỲÝỴỶỸ]".toRegex(), "Y")
            s = s.replace("Đ", "D")
            return s
        }
    }
}

