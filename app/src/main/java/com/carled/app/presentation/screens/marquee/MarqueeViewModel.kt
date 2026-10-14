package com.carled.app.presentation.screens.marquee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MarqueeViewModel(private val bleUseCases: BleUseCases) : ViewModel() {

    private val _uiState = MutableStateFlow(MarqueeUiState())
    val uiState: StateFlow<MarqueeUiState> = _uiState.asStateFlow()

    init {
        // Lắng nghe event "marquee_active" từ ESP32 để xác nhận đang phát
        viewModelScope.launch {
            bleUseCases.getTelemetryEvent().collect { event ->
                when (event) {
                    is TelemetryEvent.MarqueeActive -> {
                        _uiState.update { it.copy(lastEventText = event.text, isSending = false) }
                    }
                    else -> Unit
                }
            }
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
        val cleanText = removeVietnameseTones(state.text.trim())
        _uiState.update { it.copy(isSending = true, feedbackMessage = null) }
        viewModelScope.launch {
            bleUseCases.sendMarquee(
                text       = cleanText,
                r          = state.r,
                g          = state.g,
                b          = state.b,
                speed      = state.speed,
                size       = state.size,
                rainbow    = state.rainbow,
                scrollMode = state.scrollMode
            )
            _uiState.update { it.copy(feedbackMessage = "Đã phát chữ \"$cleanText\" lên bảng LED ✓", isSending = false) }
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

