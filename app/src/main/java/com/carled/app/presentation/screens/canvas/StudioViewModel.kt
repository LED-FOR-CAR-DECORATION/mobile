package com.carled.app.presentation.screens.canvas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.data.ble.BleProtocolHelper
import com.carled.app.data.ble.TouchPoint
import com.carled.app.data.local.PixelDraft
import com.carled.app.data.local.PixelDraftStore
import com.carled.app.data.local.PixelPalette
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.MirrorMode
import com.carled.app.domain.model.PixelDocument
import com.carled.app.domain.model.PixelFont
import com.carled.app.domain.model.PixelShape
import com.carled.app.domain.model.PixelText
import com.carled.app.domain.model.PixelTool
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.model.clearRegion
import com.carled.app.domain.model.regionPixels
import com.carled.app.domain.model.stampRegion
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

/** A marquee selection in document pixel coordinates. Not persisted — lives only in UI state. */
data class SelectionRect(val x0: Int, val y0: Int, val x1: Int, val y1: Int) {
    val left get() = minOf(x0, x1)
    val right get() = maxOf(x0, x1)
    val top get() = minOf(y0, y1)
    val bottom get() = maxOf(y0, y1)
}

data class StudioState(
    val document: PixelDocument? = null,
    val name: String = "Thiết kế của tôi",
    val tool: PixelTool = PixelTool.PEN,
    val color: Int = 0xff89c89b.toInt(),
    val brushSize: Int = 1,
    val mirror: MirrorMode = MirrorMode.NONE,
    val shape: PixelShape = PixelShape.RECTANGLE,
    val filled: Boolean = false,
    val grid: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val dirty: Boolean = false,
    val palettes: List<PixelPalette> = emptyList(),
    val feedback: String? = null,
    val sending: Boolean = false,
    val connected: Boolean = false,
    val liveEnabled: Boolean = true,
    val syncing: Boolean = false,
    val liveError: String? = null,
    // New: marquee selection + clipboard (Select/Move/Cut/Copy/Paste) and a brightness-style "opacity" for drawing.
    val selection: SelectionRect? = null,
    val hasClipboard: Boolean = false,
    val opacity: Int = 100
)

class StudioViewModel(private val ble: BleUseCases, context: Context, draftId: String? = null) :
    ViewModel() {
    private val store = PixelDraftStore(context)
    private val _state = MutableStateFlow(StudioState())
    val state = _state.asStateFlow()
    private val id = draftId ?: UUID.randomUUID().toString()
    private val writes = Mutex()
    private val changed = Channel<Unit>(Channel.CONFLATED)
    private var resumeAfterSave = false
    private var wire: IntArray? = null
    private var generation = 0
    private val undo = ArrayDeque<PixelDocument>()
    private val redo = ArrayDeque<PixelDocument>()
    private var strokeStart: PixelDocument? = null
    private var start: Pair<Int, Int>? = null
    private var previous: Pair<Int, Int>? = null
    private var saved: PixelDocument? = null
    private var savedName = "Thiết kế của tôi"

    private var clipboard: Pair<Int, List<Int>>? = null
    private var floating: Triple<Int, Int, List<Int>>? = null
    private var floatingAt: Pair<Int, Int>? = null

    init {
        runCatching {
            val draft = draftId?.let { value -> store.drafts().firstOrNull { it.id == value } }
            saved = draft?.document
            savedName = draft?.name ?: "Thiết kế của tôi"
            _state.value = StudioState(
                document = saved,
                name = draft?.name ?: "Thiết kế của tôi",
                palettes = store.palettes()
            )
        }.onFailure { feedback("Không thể đọc bản nháp hoặc bảng màu đã lưu.") }
        viewModelScope.launch {
            ble.getConnectionState().distinctUntilChanged { old, new -> old == new }
                .collect { connection ->
                    generation++; wire = null
                    _state.update {
                        it.copy(
                            connected = connection is ConnectionState.Connected,
                            syncing = false
                        )
                    }
                    changed.trySend(Unit)
                }
        }
        viewModelScope.launch {
            for (signal in changed) {
                try {
                    while (true) {
                        val more = writes.withLock {
                            val state = _state.value
                            val document = state.document
                            if (!state.liveEnabled || !state.connected || state.sending || document == null || document.width != 64 || document.height != 32) {
                                _state.update { it.copy(syncing = false) }; return@withLock false
                            }
                            val epoch = generation
                            if (wire == null) {
                                _state.update { it.copy(syncing = true) }
                                ble.clearCanvas()
                                delay(40)
                                if (epoch != generation) return@withLock false
                                wire = IntArray(2048)
                                resumeAfterSave = false
                            }
                            val current = requireNotNull(wire)
                            val target = livePixels(document)
                            val indices = target.indices.filter { target[it] != current[it] }
                                .take(BleProtocolHelper.MAX_DRAW_POINTS)
                            if (indices.isEmpty()) {
                                _state.update { it.copy(syncing = false) }; return@withLock false
                            }
                            if (resumeAfterSave) {
                                // Firmware resets an uploaded image on the first live pixel.
                                // Re-enter live mode explicitly, then replay the entire latest canvas.
                                ble.clearCanvas()
                                delay(40)
                                if (epoch != generation) return@withLock false
                                wire = IntArray(2048); resumeAfterSave = false
                                return@withLock true
                            }
                            _state.update { it.copy(syncing = true) }
                            ble.sendBatchDraw(indices.map { index ->
                                val pixel = target[index]
                                TouchPoint(
                                    index % 64,
                                    index / 64,
                                    (pixel ushr 16) and 255,
                                    (pixel ushr 8) and 255,
                                    pixel and 255
                                )
                            })
                            if (epoch != generation) return@withLock false
                            indices.forEach { current[it] = target[it] }
                            true
                        }
                        if (!more) break
                        delay(40)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    wire = null
                    _state.update {
                        it.copy(
                            liveEnabled = false,
                            syncing = false,
                            liveError = "LIVE bị gián đoạn: ${e.message}. Bật lại LIVE để đồng bộ canvas."
                        )
                    }
                }
            }
        }
    }

    fun create(w: Int, h: Int) {
        if (_state.value.sending) return; undo.clear(); redo.clear(); saved = null; clipboard =
            null; show(PixelDocument(w, h))
    }

    fun name(value: String) {
        _state.update {
            it.copy(
                name = value.take(80),
                dirty = value.take(80) != savedName || it.document != saved
            )
        }
    }

    fun tool(value: PixelTool) {
        _state.update { it.copy(tool = value) }
    }

    fun color(value: Int) {
        _state.update { it.copy(color = value) }
    }

    fun brush(value: Int) {
        _state.update { it.copy(brushSize = value.coerceIn(1, 12)) }
    }

    fun opacity(value: Int) {
        _state.update { it.copy(opacity = value.coerceIn(10, 100)) }
    }

    fun mirror(value: MirrorMode) {
        _state.update { it.copy(mirror = value) }
    }

    fun shape(value: PixelShape, filled: Boolean) {
        _state.update { it.copy(shape = value, filled = filled, tool = PixelTool.SHAPE) }
    }

    fun togglePenEraser() {
        _state.update { it.copy(tool = if (it.tool == PixelTool.ERASER) PixelTool.PEN else PixelTool.ERASER) }
    }

    fun cycleRectShape() {
        _state.update {
            it.copy(
                shape = if (it.shape == PixelShape.RECTANGLE) PixelShape.ELLIPSE else PixelShape.RECTANGLE,
                tool = PixelTool.SHAPE
            )
        }
    }

    fun applyTools(
        tool: PixelTool,
        shape: PixelShape,
        filled: Boolean,
        mirror: MirrorMode,
        grid: Boolean
    ) {
        if (_state.value.sending) return
        _state.update {
            it.copy(
                tool = tool,
                shape = shape,
                filled = filled && shape != PixelShape.LINE,
                mirror = mirror,
                grid = grid
            )
        }
    }

    fun grid() {
        _state.update { it.copy(grid = !it.grid) }
    }

    fun feedback(value: String?) {
        _state.update { it.copy(feedback = value) }
    }

    private fun history(document: PixelDocument) {
        undo.addLast(document); if (undo.size > 50) undo.removeFirst(); redo.clear()
    }

    private fun show(document: PixelDocument) {
        _state.update {
            it.copy(
                document = document,
                canUndo = undo.isNotEmpty(),
                canRedo = redo.isNotEmpty(),
                dirty = document != saved || it.name != savedName
            )
        }
        changed.trySend(Unit)
    }

    private fun livePixels(document: PixelDocument): IntArray = document.composite().map { pixel ->
        if (pixel == 0) 0 else pixel and 0x00ffffff
    }.toIntArray()

    private fun applyOpacity(color: Int): Int {
        val o = _state.value.opacity
        if (o >= 100) return color
        val r = (((color ushr 16) and 0xff) * o / 100).coerceIn(0, 255)
        val g = (((color ushr 8) and 0xff) * o / 100).coerceIn(0, 255)
        val b = ((color and 0xff) * o / 100).coerceIn(0, 255)
        return 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
    }

    fun live(enabled: Boolean) {
        if (_state.value.sending) return
        _state.update { it.copy(liveEnabled = enabled, liveError = null, syncing = false) }
        if (enabled) {
            wire = null; changed.trySend(Unit)
        }
    }

    fun clear() {
        if (!_state.value.sending) _state.value.document?.let {
            commit(
                PixelDocument(
                    it.width,
                    it.height
                )
            )
        }
    }

    private fun commit(document: PixelDocument) {
        if (_state.value.sending) return
        val before = _state.value.document ?: return; if (before != document) {
            history(before); show(document)
        }
    }

    fun down(x: Int, y: Int) {
        val s = _state.value; if (s.sending) return
        val d = s.document ?: return
        if (s.tool == PixelTool.PICKER) {
            if (x !in 0 until d.width || y !in 0 until d.height) return
            val picked = d.composite()[y * d.width + x]
            if (picked != 0) _state.update { it.copy(color = picked, tool = PixelTool.PEN) }
            else feedback("Pixel này chưa có màu. Chọn một pixel đã vẽ.")
            return
        }
        if (s.tool == PixelTool.TEXT) return
        if (s.tool == PixelTool.SELECT) {
            val cx = x.coerceIn(0, d.width - 1)
            val cy = y.coerceIn(0, d.height - 1)
            start = cx to cy; previous = cx to cy
            _state.update { it.copy(selection = SelectionRect(cx, cy, cx, cy)) }
            return
        }
        if (s.tool == PixelTool.MOVE) {
            val sel = s.selection ?: return
            if (x in sel.left..sel.right && y in sel.top..sel.bottom) {
                strokeStart = d
                val (w, data) = d.regionPixels(sel.left, sel.top, sel.right, sel.bottom)
                val h = if (w > 0) data.size / w else 0
                floating = Triple(w, h, data)
                floatingAt = sel.left to sel.top
                start = x to y; previous = x to y
                show(d.clearRegion(sel.left, sel.top, sel.right, sel.bottom))
            }
            return
        }
        if (x !in 0 until d.width || y !in 0 until d.height) return
        strokeStart = d; start = x to y; previous = x to y
        if (s.tool == PixelTool.FILL) show(d.fill(x, y, applyOpacity(s.color))) else move(x, y)
    }

    fun move(x: Int, y: Int) {
        val s = _state.value; if (s.sending) return
        val d = s.document ?: return
        if (s.tool == PixelTool.SELECT) {
            val anchor = start ?: return
            val cx = x.coerceIn(0, d.width - 1)
            val cy = y.coerceIn(0, d.height - 1)
            _state.update {
                it.copy(
                    selection = SelectionRect(
                        anchor.first,
                        anchor.second,
                        cx,
                        cy
                    )
                )
            }
            return
        }
        if (s.tool == PixelTool.MOVE) {
            val base = strokeStart ?: return
            val (w, h, data) = floating ?: return
            val anchor = start ?: return
            val origin = floatingAt ?: return
            val nx = origin.first + (x - anchor.first)
            val ny = origin.second + (y - anchor.second)
            show(base.stampRegion(nx, ny, w, data))
            _state.update { it.copy(selection = SelectionRect(nx, ny, nx + w - 1, ny + h - 1)) }
            return
        }
        val base = strokeStart ?: return
        if (x !in 0 until d.width || y !in 0 until d.height) return
        val from = previous ?: return
        when (s.tool) {
            PixelTool.PEN, PixelTool.ERASER -> show(
                d.brush(
                    from.first,
                    from.second,
                    x,
                    y,
                    if (s.tool == PixelTool.ERASER) 0 else applyOpacity(s.color),
                    s.brushSize,
                    s.mirror
                )
            )

            PixelTool.SHAPE -> start?.let {
                show(
                    base.shape(
                        it.first,
                        it.second,
                        x,
                        y,
                        applyOpacity(s.color),
                        s.shape,
                        s.filled,
                        s.mirror
                    )
                )
            }

            else -> Unit
        }
        previous = x to y
    }

    fun end(cancel: Boolean = false) {
        val s = _state.value
        if (s.sending) {
            strokeStart = null; start = null; previous = null; floating = null; floatingAt =
                null; return
        }
        if (s.tool == PixelTool.SELECT) {
            val sel = s.selection
            if (sel != null && sel.left == sel.right && sel.top == sel.bottom) _state.update {
                it.copy(
                    selection = null
                )
            }
            start = null; previous = null; return
        }
        if (s.tool == PixelTool.MOVE) {
            strokeStart?.let { before ->
                if (cancel) show(before)
                else if (_state.value.document != before) {
                    history(before); show(_state.value.document!!)
                }
            }
            strokeStart = null; start = null; previous = null; floating = null; floatingAt = null
            return
        }
        strokeStart?.let { before ->
            if (cancel) show(before)
            else if (_state.value.document != before) {
                history(before); show(_state.value.document!!)
            }
        }
        strokeStart = null; start = null; previous = null
    }

    fun undo() {
        if (!_state.value.sending && undo.isNotEmpty()) {
            redo.addLast(_state.value.document!!); show(undo.removeLast())
        }
    }

    fun redo() {
        if (!_state.value.sending && redo.isNotEmpty()) {
            undo.addLast(_state.value.document!!); show(redo.removeLast())
        }
    }

    fun selectAll() {
        val d = _state.value.document ?: return
        _state.update {
            it.copy(
                selection = SelectionRect(0, 0, d.width - 1, d.height - 1),
                tool = PixelTool.SELECT
            )
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selection = null) }
    }

    fun copySelection() {
        val s = _state.value
        val d = s.document ?: return
        val sel = s.selection ?: run { feedback("Chọn một vùng trước khi sao chép."); return }
        clipboard = d.regionPixels(sel.left, sel.top, sel.right, sel.bottom)
        _state.update { it.copy(hasClipboard = true) }
    }

    fun cutSelection() {
        val s = _state.value; if (s.sending) return
        val d = s.document ?: return
        val sel = s.selection ?: run { feedback("Chọn một vùng trước khi cắt."); return }
        clipboard = d.regionPixels(sel.left, sel.top, sel.right, sel.bottom)
        _state.update { it.copy(hasClipboard = true) }
        commit(d.clearRegion(sel.left, sel.top, sel.right, sel.bottom))
    }

    fun pasteClipboard() {
        val s = _state.value; if (s.sending) return
        val d = s.document ?: return
        val (w, data) = clipboard ?: run { feedback("Chưa có nội dung đã sao chép."); return }
        val h = if (w > 0) data.size / w else 0
        val x = ((d.width - w) / 2).coerceAtLeast(0)
        val y = ((d.height - h) / 2).coerceAtLeast(0)
        commit(d.stampRegion(x, y, w, data))
        _state.update {
            it.copy(
                selection = SelectionRect(x, y, x + w - 1, y + h - 1),
                tool = PixelTool.MOVE
            )
        }
    }

    fun text(block: PixelText): Boolean {
        if (_state.value.sending) return false
        val d = _state.value.document ?: return false
        if (!PixelFont.fits(block, d.width, d.height)) {
            feedback("Chỉ dùng ký tự tiếng Anh; khối chữ phải nằm trong canvas."); return false
        }
        commit(d.copy(texts = d.texts.filterNot { it.id == block.id } + block)); return true
    }

    fun deleteText(id: String) {
        _state.value.document?.let { commit(it.copy(texts = it.texts.filterNot { b -> b.id == id })) }
    }

    fun importPixels(pixels: List<Int>) {
        _state.value.document?.let { commit(it.copy(pixels = pixels)) }
    }

    fun palettes(value: List<PixelPalette>) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { store.savePalettes(value) } }
                .onSuccess { _state.update { it.copy(palettes = value) } }
                .onFailure { feedback("Không thể lưu bảng màu: ${it.message}") }
        }
    }

    fun save() {
        val s = _state.value; if (s.sending) return
        val d = s.document ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    store.save(
                        PixelDraft(
                            id,
                            s.name.ifBlank { "Thiết kế của tôi" },
                            d
                        )
                    )
                }
            }
                .onSuccess {
                    saved = d; savedName = s.name; _state.update {
                    it.copy(
                        dirty = it.document != d || it.name != savedName,
                        feedback = "Đã lưu bản nháp trên điện thoại"
                    )
                }
                }
                .onFailure { feedback("Không thể lưu bản nháp: ${it.message}") }
        }
    }

    fun send() {
        val s = _state.value; if (s.sending) return
        val d = s.document ?: return
        if (!s.connected) {
            feedback("Kết nối thiết bị trước khi gửi."); return
        }
        if (d.width != 64 || d.height != 32) {
            feedback("Giao thức LED hiện tại chỉ nhận 64×32. Hãy lưu bản nháp để giữ nguyên kích thước."); return
        }
        _state.update { it.copy(sending = true, feedback = "Đang lưu vào bộ nhớ LED…") }
        viewModelScope.launch {
            try {
                writes.withLock {
                    val epoch = generation
                    coroutineScope {
                        val ack = async(start = CoroutineStart.UNDISPATCHED) {
                            withTimeout(30_000) {
                                ble.getTelemetryEvent()
                                    .first { it is TelemetryEvent.UploadComplete || it is TelemetryEvent.UploadError }
                            }
                        }
                        ble.uploadProjection(d.rgb565())
                        when (val event = ack.await()) {
                            is TelemetryEvent.UploadComplete -> {
                                if (epoch != generation) error("Kết nối đã thay đổi trong khi lưu. Hãy kiểm tra và lưu lại.")
                                wire = livePixels(d)
                                resumeAfterSave = true
                                feedback("LED đã xác nhận lưu vào bộ nhớ thiết bị (SPIFFS).")
                            }

                            is TelemetryEvent.UploadError -> feedback("Không gửi được: ${event.error}")
                            else -> Unit
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                feedback("Chưa nhận xác nhận từ LED. Kiểm tra kết nối trước khi thử lại.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                feedback("Không gửi được: ${e.message}")
            } finally {
                _state.update { it.copy(sending = false) }; changed.trySend(Unit)
            }
        }
    }
}