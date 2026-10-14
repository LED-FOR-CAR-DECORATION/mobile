package com.carled.app.presentation.screens.canvas

import androidx.compose.ui.graphics.Color

enum class CanvasTool {
    PENCIL,
    ERASER,
    BUCKET
}

data class CanvasUiState(
    val pixels: Array<IntArray> = Array(32) { IntArray(64) { 0xFF000000.toInt() } }, // 32 rows × 64 cols
    val drawColor: Color = Color(0xFF00D2FF), // Default Cyan like ble_image_importer
    val currentTool: CanvasTool = CanvasTool.PENCIL,
    val brushSize: Int = 1, // 1px or 2px
    val canUndo: Boolean = false,
    val isClearing: Boolean = false,
    val feedbackMessage: String? = null,
    val lastEventFromDevice: String? = null,
    val showColorPicker: Boolean = false,
    val touchCoord: Pair<Int, Int>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CanvasUiState) return false
        return drawColor == other.drawColor &&
                currentTool == other.currentTool &&
                brushSize == other.brushSize &&
                canUndo == other.canUndo &&
                isClearing == other.isClearing &&
                feedbackMessage == other.feedbackMessage &&
                lastEventFromDevice == other.lastEventFromDevice &&
                showColorPicker == other.showColorPicker &&
                touchCoord == other.touchCoord
    }

    override fun hashCode(): Int {
        var result = drawColor.hashCode()
        result = 31 * result + currentTool.hashCode()
        result = 31 * result + brushSize
        result = 31 * result + canUndo.hashCode()
        result = 31 * result + isClearing.hashCode()
        return result
    }
}

