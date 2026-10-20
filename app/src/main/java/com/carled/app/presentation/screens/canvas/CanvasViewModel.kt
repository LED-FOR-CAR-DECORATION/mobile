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
import java.util.LinkedList
import java.util.Queue

class CanvasViewModel(private val bleUseCases: BleUseCases) : ViewModel() {

    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    // Buffer tích lũy điểm chạm cho batch sending (35ms flush ~ 28 FPS)
    private val batchBuffer = mutableListOf<TouchPoint>()

    // Undo stack lưu tối đa 12 bước
    private val undoStack = mutableListOf<Array<IntArray>>()

    // Điểm vẽ trước đó để nội suy đường thẳng (Bresenham) tránh đứt nét khi vuốt nhanh
    private var lastPoint: Pair<Int, Int>? = null

    init {
        // Lắng nghe event từ ESP32
        viewModelScope.launch {
            bleUseCases.getTelemetryEvent().collect { event ->
                when (event) {
                    is TelemetryEvent.CanvasCleared -> {
                        _uiState.update {
                            it.copy(
                                pixels = Array(32) { IntArray(64) { 0xFF000000.toInt() } },
                                lastEventFromDevice = "Bảng LED đã chuyển về nền đen tuyền ✓",
                                isClearing = false,
                                canvasVersion = it.canvasVersion + 1
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * Kích hoạt chế độ vẽ live khi mở màn hình:
     * Gửi lệnh Opcode 0x15 sang ESP32 để chuyển mode sang CUSTOM_PRESET và xóa matrix về màu đen.
     */
    fun initLiveCanvas() {
        viewModelScope.launch {
            bleUseCases.clearCanvas()
        }
    }

    fun setTool(tool: CanvasTool) {
        _uiState.update { it.copy(currentTool = tool) }
    }

    fun setBrushSize(size: Int) {
        _uiState.update { it.copy(brushSize = size.coerceIn(1, 3)) }
    }

    fun setDrawColor(color: Color) {
        _uiState.update {
            it.copy(
                drawColor = color,
                currentTool = if (it.currentTool == CanvasTool.ERASER) CanvasTool.PENCIL else it.currentTool
            )
        }
    }

    fun showColorPicker(show: Boolean) {
        _uiState.update { it.copy(showColorPicker = show) }
    }

    /**
     * Bắt đầu chạm ngón tay (Tap hoặc Drag Start)
     */
    fun onTouchStart(x: Int, y: Int) {
        if (x !in 0 until 64 || y !in 0 until 32) return
        pushUndoState()
        lastPoint = Pair(x, y)
        _uiState.update { it.copy(touchCoord = Pair(x, y)) }

        when (_uiState.value.currentTool) {
            CanvasTool.BUCKET -> {
                floodFill(x, y)
            }
            CanvasTool.PENCIL, CanvasTool.ERASER -> {
                drawWithBrush(x, y)
            }
        }
    }

    /**
     * Di chuyển ngón tay (Drag Move)
     * Dùng Bresenham nối từ lastPoint đến (x, y) để nét vẽ liền mạch
     */
    fun onTouchMove(x: Int, y: Int) {
        if (x !in 0 until 64 || y !in 0 until 32) return
        _uiState.update { it.copy(touchCoord = Pair(x, y)) }

        if (_uiState.value.currentTool == CanvasTool.BUCKET) {
            return
        }

        val lp = lastPoint
        if (lp != null && (lp.first != x || lp.second != y)) {
            // Nối đường thẳng giữa 2 điểm
            drawLineBresenham(lp.first, lp.second, x, y)
        } else {
            drawWithBrush(x, y)
        }
        lastPoint = Pair(x, y)
    }

    fun onTouchEnd() {
        lastPoint = null
        _uiState.update { it.copy(touchCoord = null) }
    }

    private fun drawWithBrush(cx: Int, cy: Int) {
        val size = _uiState.value.brushSize
        val isEraser = _uiState.value.currentTool == CanvasTool.ERASER
        val targetColor = if (isEraser) Color.Black else _uiState.value.drawColor

        val r = (targetColor.red * 255).toInt()
        val g = (targetColor.green * 255).toInt()
        val b = (targetColor.blue * 255).toInt()
        val argb = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b

        val newPixels = _uiState.value.pixels.map { it.clone() }.toTypedArray()
        val radius = size - 1
        var modified = false

        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val px = cx + dx
                val py = cy + dy
                if (px in 0 until 64 && py in 0 until 32) {
                    if (newPixels[py][px] != argb) {
                        newPixels[py][px] = argb
                        batchBuffer.add(TouchPoint(px, py, r, g, b))
                        modified = true
                    }
                }
            }
        }
        if (modified) {
            _uiState.update { it.copy(pixels = newPixels, canvasVersion = it.canvasVersion + 1) }
        }
    }

    private fun drawLineBresenham(x0: Int, y0: Int, x1: Int, y1: Int) {
        var x = x0
        var y = y0
        val dx = Math.abs(x1 - x0)
        val dy = -Math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy

        while (true) {
            drawWithBrush(x, y)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                x += sx
            }
            if (e2 <= dx) {
                err += dx
                y += sy
            }
        }
    }

    /**
     * Thuật toán đổ màu Flood Fill (Thùng sơn)
     */
    private fun floodFill(startX: Int, startY: Int) {
        val currentPixels = _uiState.value.pixels
        val targetArgb = currentPixels[startY][startX]

        val fillColor = _uiState.value.drawColor
        val r = (fillColor.red * 255).toInt()
        val g = (fillColor.green * 255).toInt()
        val b = (fillColor.blue * 255).toInt()
        val fillArgb = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b

        if (targetArgb == fillArgb) return

        val newPixels = currentPixels.map { it.clone() }.toTypedArray()
        val queue: Queue<Pair<Int, Int>> = LinkedList()
        queue.add(Pair(startX, startY))
        newPixels[startY][startX] = fillArgb
        batchBuffer.add(TouchPoint(startX, startY, r, g, b))

        val dirs = arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
        while (!queue.isEmpty()) {
            val (cx, cy) = queue.poll() ?: break
            for ((dx, dy) in dirs) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx in 0 until 64 && ny in 0 until 32) {
                    if (newPixels[ny][nx] == targetArgb) {
                        newPixels[ny][nx] = fillArgb
                        batchBuffer.add(TouchPoint(nx, ny, r, g, b))
                        queue.add(Pair(nx, ny))
                    }
                }
            }
        }

        _uiState.update { it.copy(pixels = newPixels, canvasVersion = it.canvasVersion + 1) }
    }

    private fun pushUndoState() {
        val snapshot = _uiState.value.pixels.map { it.clone() }.toTypedArray()
        if (undoStack.size >= 12) {
            undoStack.removeAt(0)
        }
        undoStack.add(snapshot)
        _uiState.update { it.copy(canUndo = true) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previousState = undoStack.removeAt(undoStack.lastIndex)
        _uiState.update {
            it.copy(
                pixels = previousState,
                canUndo = undoStack.isNotEmpty(),
                canvasVersion = it.canvasVersion + 1
            )
        }

        // Đồng bộ lại toàn bộ điểm khác biệt lên BLE
        batchBuffer.clear()
        for (y in 0 until 32) {
            for (x in 0 until 64) {
                val argb = previousState[y][x]
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                batchBuffer.add(TouchPoint(x, y, r, g, b))
            }
        }
    }

    /**
     * Mẫu hình Pixel Art có sẵn để dán nhanh lên xe
     */
    fun applyTemplate(type: String) {
        pushUndoState()
        val newPixels = Array(32) { IntArray(64) { 0xFF000000.toInt() } }
        val points = mutableListOf<TouchPoint>()

        when (type) {
            "heart" -> {
                // Vẽ trái tim đỏ ở giữa
                val heartPattern = listOf(
                    "  XXXX   XXXX  ",
                    " XXXXXX XXXXXX ",
                    "XXXXXXXXXXXXXXX",
                    "XXXXXXXXXXXXXXX",
                    " XXXXXXXXXXXXX ",
                    "  XXXXXXXXXXX  ",
                    "   XXXXXXXXX   ",
                    "    XXXXXXX    ",
                    "     XXXXX     ",
                    "      XXX      ",
                    "       X       "
                )
                val startX = (64 - 15) / 2
                val startY = (32 - 11) / 2
                val r = 255; val g = 0; val b = 85
                val argb = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
                heartPattern.forEachIndexed { rowIdx, rowStr ->
                    rowStr.forEachIndexed { colIdx, char ->
                        if (char == 'X') {
                            val px = startX + colIdx
                            val py = startY + rowIdx
                            newPixels[py][px] = argb
                            points.add(TouchPoint(px, py, r, g, b))
                        }
                    }
                }
            }
            "car" -> {
                // Biểu tượng xe hơi thể thao Cyan/Gold
                val carPattern = listOf(
                    "      XXXXXXXXXXXX      ",
                    "    XXXXXXXXXXXXXXXX    ",
                    "   XXXXXX   XXXXXXXXX   ",
                    "  XXXXXXXXXXXXXXXXXXXX  ",
                    " XXXXXXXXXXXXXXXXXXXXXX ",
                    " XXXXXXXXXXXXXXXXXXXXXX ",
                    "  XX   XXXXXXXXXX   XX  ",
                    "  XX   XXXXXXXXXX   XX  "
                )
                val startX = (64 - 24) / 2
                val startY = (32 - 8) / 2
                val r = 0; val g = 210; val b = 255
                val argb = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
                carPattern.forEachIndexed { rowIdx, rowStr ->
                    rowStr.forEachIndexed { colIdx, char ->
                        if (char == 'X') {
                            val px = startX + colIdx
                            val py = startY + rowIdx
                            newPixels[py][px] = argb
                            points.add(TouchPoint(px, py, r, g, b))
                        }
                    }
                }
            }
            "smile" -> {
                // Mặt cười màu vàng Gold
                val r = 255; val g = 215; val b = 0
                val argb = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
                // Mắt
                for (dy in 0..2) {
                    for (dx in 0..2) {
                        newPixels[10 + dy][24 + dx] = argb
                        newPixels[10 + dy][38 + dx] = argb
                        points.add(TouchPoint(24 + dx, 10 + dy, r, g, b))
                        points.add(TouchPoint(38 + dx, 10 + dy, r, g, b))
                    }
                }
                // Nụ cười
                for (col in 22..42) {
                    newPixels[22][col] = argb
                    points.add(TouchPoint(col, 22, r, g, b))
                }
                newPixels[21][21] = argb; newPixels[20][20] = argb
                newPixels[21][43] = argb; newPixels[20][44] = argb
                points.add(TouchPoint(21, 21, r, g, b))
                points.add(TouchPoint(20, 20, r, g, b))
                points.add(TouchPoint(43, 21, r, g, b))
                points.add(TouchPoint(44, 20, r, g, b))
            }
        }

        _uiState.update { it.copy(pixels = newPixels, canvasVersion = it.canvasVersion + 1) }
        batchBuffer.clear()
        viewModelScope.launch {
            bleUseCases.clearCanvas()
            batchBuffer.addAll(points)
        }
    }

    /**
     * Gửi batch buffer qua BLE định kỳ (35ms ~ 28 FPS)
     */
    fun flushBatch() {
        if (batchBuffer.isEmpty()) return
        val chunk = if (batchBuffer.size > 12) {
            val sub = batchBuffer.subList(0, 12).toList()
            batchBuffer.subList(0, 12).clear()
            sub
        } else {
            val sub = batchBuffer.toList()
            batchBuffer.clear()
            sub
        }

        viewModelScope.launch {
            if (chunk.size == 1) {
                val p = chunk[0]
                bleUseCases.sendSinglePixel(p.x, p.y, p.r, p.g, p.b)
            } else {
                bleUseCases.sendBatchDraw(chunk)
            }
        }
    }

    /**
     * Xóa bảng vẽ về màu đen tuyền trên điện thoại và gửi Opcode 0x15 sang ESP32
     */
    fun clearCanvas() {
        pushUndoState()
        _uiState.update {
            it.copy(
                pixels = Array(32) { IntArray(64) { 0xFF000000.toInt() } },
                isClearing = false,
                lastEventFromDevice = "Đã chuyển bảng LED về nền đen (Opcode 0x15)",
                canvasVersion = it.canvasVersion + 1
            )
        }
        batchBuffer.clear()
        viewModelScope.launch {
            bleUseCases.clearCanvas()
        }
    }

    /**
     * Ghi đè toàn bộ Canvas hiện tại vào bộ nhớ Flash (SPIFFS) của ESP32.
     * Tương ứng tính năng "Lưu Vĩnh Viễn Lên Xe" (0x10 -> 0x11 -> 0x12) trong bộ test suite.
     */
    fun burnCanvasToFlash() {
        viewModelScope.launch {
            _uiState.update { it.copy(feedbackMessage = "Đang lưu hình vào bộ nhớ xe (SPIFFS Flash)...") }
            val data = ByteArray(64 * 32 * 2)
            var idx = 0
            val pixels = _uiState.value.pixels
            for (y in 0 until 32) {
                for (x in 0 until 64) {
                    val px = pixels[y][x]
                    val r = ((px shr 16) and 0xFF shr 3) and 0x1F
                    val g = ((px shr 8) and 0xFF shr 2) and 0x3F
                    val b = (px and 0xFF shr 3) and 0x1F
                    val col565 = (r shl 11) or (g shl 5) or b
                    data[idx++] = (col565 and 0xFF).toByte()
                    data[idx++] = ((col565 shr 8) and 0xFF).toByte()
                }
            }
            runCatching {
                bleUseCases.uploadProjection(data)
                _uiState.update {
                    it.copy(
                        feedbackMessage = "✅ Đã lưu hình vĩnh viễn lên Flash của xe!",
                        lastEventFromDevice = "Đã lưu SPIFFS Flash thành công"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(feedbackMessage = "❌ Lỗi lưu Flash: ${e.message}") }
            }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(feedbackMessage = null, lastEventFromDevice = null) }
    }
}

