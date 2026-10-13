package com.carled.app.presentation.screens.canvas

import androidx.compose.ui.graphics.Color

data class CanvasUiState(
    val pixels: Array<IntArray> = Array(32) { IntArray(64) { 0xFF000000.toInt() } }, // 32 rows × 64 cols
    val drawColor: Color = Color.White,
    val isClearing: Boolean = false,
    val feedbackMessage: String? = null,
    val lastEventFromDevice: String? = null  // từ telemetry event "canvas_cleared"
) {
    // Array không hỗ trợ equals/hashCode tự động trong data class
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CanvasUiState) return false
        return drawColor == other.drawColor &&
               isClearing == other.isClearing &&
               feedbackMessage == other.feedbackMessage &&
               lastEventFromDevice == other.lastEventFromDevice
    }
    override fun hashCode(): Int = drawColor.hashCode()
}
