package com.carled.app.presentation.screens.canvas

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.data.ble.TouchPoint
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CanvasViewModel(private val bleUseCases: BleUseCases) : ViewModel() {

    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    // Buffer tích lũy điểm chạm cho batch sending (25 FPS flush)
    private val batchBuffer = mutableListOf<TouchPoint>()

    init {
        // Lắng nghe event "canvas_cleared" từ ESP32 để confirm xóa thành công
        viewModelScope.launch {
            bleUseCases.getTelemetryEvent().collect { event ->
                when (event) {
                    is TelemetryEvent.CanvasCleared -> {
                        _uiState.update {
                            it.copy(
                                pixels = Array(32) { IntArray(64) { 0xFF000000.toInt() } },
                                lastEventFromDevice = "Canvas đã được xóa bởi ESP32 ✓",
                                isClearing = false
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun setDrawColor(color: Color) {
        _uiState.update { it.copy(drawColor = color) }
    }

    /**
     * Vẽ pixel tại (x, y) với màu hiện tại.
     * Cập nhật UI ngay lập tức, tích lũy vào batchBuffer.
     */
    fun drawPixel(x: Int, y: Int) {
        if (x !in 0 until 64 || y !in 0 until 32) return
        val color = _uiState.value.drawColor
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        val argb = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b

        val newPixels = _uiState.value.pixels.map { it.clone() }.toTypedArray()
        newPixels[y][x] = argb
        _uiState.update { it.copy(pixels = newPixels) }

        batchBuffer.add(TouchPoint(x, y, r, g, b))
    }

    /**
     * Gửi batch buffer hiện tại qua BLE và xóa buffer.
     * Gọi mỗi ~40ms từ Canvas UI (LaunchedEffect + delay) → ~25 FPS.
     */
    fun flushBatch() {
        if (batchBuffer.isEmpty()) return
        val points = batchBuffer.toList()
        batchBuffer.clear()
        viewModelScope.launch {
            if (points.size == 1) {
                val p = points[0]
                bleUseCases.sendSinglePixel(p.x, p.y, p.r, p.g, p.b)
            } else {
                bleUseCases.sendBatchDraw(points)
            }
        }
    }

    /**
     * Xóa canvas cả local lẫn trên ESP32.
     * ESP32 sẽ xác nhận bằng event "canvas_cleared" → xóa local pixels.
     */
    fun clearCanvas() {
        _uiState.update { it.copy(isClearing = true, feedbackMessage = null, lastEventFromDevice = null) }
        batchBuffer.clear()
        viewModelScope.launch {
            bleUseCases.clearCanvas()
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(feedbackMessage = null, lastEventFromDevice = null) }
    }
}
