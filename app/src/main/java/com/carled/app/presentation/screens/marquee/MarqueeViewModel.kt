package com.carled.app.presentation.screens.marquee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.model.MarqueeLayout
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

class MarqueeViewModel(private val bleUseCases: BleUseCases) : ViewModel() {

    private val _uiState = MutableStateFlow(MarqueeUiState())
    val uiState: StateFlow<MarqueeUiState> = _uiState.asStateFlow()
    val connection = bleUseCases.getConnectionState()

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
        if (state.scrollMode == "static" && !layout.fits) {
            _uiState.update { it.copy(feedbackMessage = "Chữ vượt khung 64×32 ở cỡ 5×7. Rút ngắn còn tối đa 40 ký tự hoặc chọn Cuộn trái.") }; return
        }
        _uiState.update { it.copy(isSending = true, feedbackMessage = null) }
        viewModelScope.launch {
            try {
            coroutineScope {
            val static = state.scrollMode == "static"
            val ack = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(if (static) 30000L else 10000L) {
                bleUseCases.getTelemetryEvent().first {
                    if (static) it is TelemetryEvent.UploadComplete || it is TelemetryEvent.UploadError
                    else it is TelemetryEvent.MarqueeActive && it.text == cleanText
                }
            } }
            if (static) {
                val color = (0xff shl 24) or (state.r shl 16) or (state.g shl 8) or state.b
                bleUseCases.uploadProjection(layout.render(color).rgb565())
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
            _uiState.update { it.copy(lastEventText = cleanText, feedbackMessage = if (static) "LED đã xác nhận chữ tĩnh · ${layout.glyphLabel} · ${layout.lines.size} dòng." else "Thiết bị đã xác nhận hiển thị chữ.") }
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

