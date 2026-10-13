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

    fun sendMarquee() {
        val state = _uiState.value
        if (state.text.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Vui lòng nhập nội dung chữ") }
            return
        }
        _uiState.update { it.copy(isSending = true, feedbackMessage = null) }
        viewModelScope.launch {
            bleUseCases.sendMarquee(
                text       = state.text,
                r          = state.r,
                g          = state.g,
                b          = state.b,
                speed      = state.speed,
                size       = state.size,
                rainbow    = state.rainbow,
                scrollMode = state.scrollMode
            )
            // isSending sẽ được tắt khi nhận được event "marquee_active" từ telemetry
            // hoặc sau timeout nhỏ nếu không có event
            _uiState.update { it.copy(feedbackMessage = "Đã gửi lệnh chạy chữ ✓") }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
