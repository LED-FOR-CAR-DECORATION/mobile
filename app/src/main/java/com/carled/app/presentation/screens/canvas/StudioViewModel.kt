package com.carled.app.presentation.screens.canvas

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.data.local.*
import com.carled.app.domain.model.*
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import com.carled.app.data.ble.TouchPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StudioState(
    val document: PixelDocument? = null, val name: String = "Thiết kế của tôi",
    val tool: PixelTool = PixelTool.PEN, val color: Int = 0xff89c89b.toInt(), val brushSize: Int = 1,
    val mirror: MirrorMode = MirrorMode.NONE, val shape: PixelShape = PixelShape.RECTANGLE,
    val filled: Boolean = false, val grid: Boolean = true, val canUndo: Boolean = false, val canRedo: Boolean = false,
    val dirty: Boolean = false, val palettes: List<PixelPalette> = emptyList(), val feedback: String? = null,
    val sending: Boolean = false, val connected: Boolean = false,
    val liveEnabled: Boolean = true, val syncing: Boolean = false, val liveError: String? = null
)

class StudioViewModel(private val ble: BleUseCases, context: Context, draftId: String? = null) : ViewModel() {
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
    init {
        runCatching {
            val draft = draftId?.let { value -> store.drafts().firstOrNull { it.id == value } }
            saved = draft?.document
            savedName = draft?.name ?: "Thiết kế của tôi"
            _state.value = StudioState(document = saved, name = draft?.name ?: "Thiết kế của tôi", palettes = store.palettes())
        }.onFailure { feedback("Không thể đọc bản nháp hoặc bảng màu đã lưu.") }
        viewModelScope.launch { ble.getConnectionState().distinctUntilChanged { old, new -> old == new }.collect { connection ->
            generation++; wire = null
            _state.update { it.copy(connected = connection is ConnectionState.Connected, syncing = false) }
            changed.trySend(Unit)
        } }
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
                            val indices = target.indices.filter { target[it] != current[it] }.take(15)
                            if (indices.isEmpty()) { _state.update { it.copy(syncing = false) }; return@withLock false }
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
                                TouchPoint(index % 64, index / 64, (pixel ushr 16) and 255, (pixel ushr 8) and 255, pixel and 255)
                            })
                            if (epoch != generation) return@withLock false
                            indices.forEach { current[it] = target[it] }
                            true
                        }
                        if (!more) break
                        delay(40)
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    wire = null
                    _state.update { it.copy(liveEnabled = false, syncing = false, liveError = "LIVE bị gián đoạn: ${e.message}. Bật lại LIVE để đồng bộ canvas.") }
                }
            }
        }
    }
    fun create(w: Int, h: Int) { if (_state.value.sending) return; undo.clear(); redo.clear(); saved = null; show(PixelDocument(w,h)) }
    fun name(value: String) { _state.update { it.copy(name = value.take(80), dirty = value.take(80) != savedName || it.document != saved) } }
    fun tool(value: PixelTool) { _state.update { it.copy(tool = value) } }
    fun color(value: Int) { _state.update { it.copy(color = value) } }
    fun brush(value: Int) { _state.update { it.copy(brushSize = value.coerceIn(1, 4)) } }
    fun mirror(value: MirrorMode) { _state.update { it.copy(mirror = value) } }
    fun shape(value: PixelShape, filled: Boolean) { _state.update { it.copy(shape = value, filled = filled, tool = PixelTool.SHAPE) } }
    fun grid() { _state.update { it.copy(grid = !it.grid) } }
    fun feedback(value: String?) { _state.update { it.copy(feedback = value) } }
    private fun history(document: PixelDocument) { undo.addLast(document); if (undo.size > 50) undo.removeFirst(); redo.clear() }
    private fun show(document: PixelDocument) {
        _state.update { it.copy(document = document, canUndo = undo.isNotEmpty(), canRedo = redo.isNotEmpty(), dirty = document != saved || it.name != savedName) }
        changed.trySend(Unit)
    }
    private fun livePixels(document: PixelDocument): IntArray = document.composite().map { pixel ->
        if (pixel == 0) 0 else pixel and 0x00ffffff
    }.toIntArray()
    fun live(enabled: Boolean) {
        if (_state.value.sending) return
        _state.update { it.copy(liveEnabled = enabled, liveError = null, syncing = false) }
        if (enabled) { wire = null; changed.trySend(Unit) }
    }
    fun clear() { if (!_state.value.sending) _state.value.document?.let { commit(PixelDocument(it.width,it.height)) } }

    private fun commit(document: PixelDocument) { if (_state.value.sending) return; val before = _state.value.document ?: return; if (before != document) { history(before); show(document) } }
    fun down(x: Int, y: Int) {
        val s = _state.value; if (s.sending) return; val d = s.document ?: return
        if (x !in 0 until d.width || y !in 0 until d.height) return
        if (s.tool == PixelTool.PICKER) {
            val picked = d.composite()[y * d.width + x]
            if (picked != 0) _state.update { it.copy(color = picked, tool = PixelTool.PEN) }
            else feedback("Pixel này chưa có màu. Chọn một pixel đã vẽ.")
            return
        }
        if (s.tool == PixelTool.TEXT) return
        strokeStart = d; start = x to y; previous = x to y
        if (s.tool == PixelTool.FILL) show(d.fill(x, y, s.color)) else move(x, y)
    }
    fun move(x: Int, y: Int) {
        val s = _state.value; if (s.sending) return; val d = s.document ?: return; val base = strokeStart ?: return
        if (x !in 0 until d.width || y !in 0 until d.height) return
        val from = previous ?: return
        when (s.tool) {
            PixelTool.PEN, PixelTool.ERASER -> show(d.brush(from.first, from.second, x, y, if (s.tool == PixelTool.ERASER) 0 else s.color, s.brushSize, s.mirror))
            PixelTool.SHAPE -> start?.let { show(base.shape(it.first, it.second, x, y, s.color, s.shape, s.filled, s.mirror)) }
            else -> Unit
        }
        previous = x to y
    }
    fun end(cancel: Boolean = false) {
        if (_state.value.sending) { strokeStart = null; start = null; previous = null; return }
        strokeStart?.let { before ->
            if (cancel) show(before)
            else if (_state.value.document != before) { history(before); show(_state.value.document!!) }
        }
        strokeStart = null; start = null; previous = null
    }
    fun undo() { if (!_state.value.sending && undo.isNotEmpty()) { redo.addLast(_state.value.document!!); show(undo.removeLast()) } }
    fun redo() { if (!_state.value.sending && redo.isNotEmpty()) { undo.addLast(_state.value.document!!); show(redo.removeLast()) } }
    fun text(block: PixelText): Boolean {
        if (_state.value.sending) return false
        val d = _state.value.document ?: return false
        if (!PixelFont.fits(block, d.width, d.height)) { feedback("Chỉ dùng ký tự tiếng Anh; khối chữ phải nằm trong canvas."); return false }
        commit(d.copy(texts = d.texts.filterNot { it.id == block.id } + block)); return true
    }
    fun deleteText(id: String) { _state.value.document?.let { commit(it.copy(texts = it.texts.filterNot { b -> b.id == id })) } }
    fun importPixels(pixels: List<Int>) { _state.value.document?.let { commit(it.copy(pixels = pixels)) } }
    fun palettes(value: List<PixelPalette>) {
        viewModelScope.launch { runCatching { withContext(Dispatchers.IO) { store.savePalettes(value) } }
            .onSuccess { _state.update { it.copy(palettes = value) } }.onFailure { feedback("Không thể lưu bảng màu: ${it.message}") } }
    }
    fun save() {
        val s = _state.value; if (s.sending) return; val d = s.document ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { store.save(PixelDraft(id, s.name.ifBlank { "Thiết kế của tôi" }, d)) } }
                .onSuccess { saved = d; savedName = s.name; _state.update { it.copy(dirty = it.document != d || it.name != savedName, feedback = "Đã lưu bản nháp trên điện thoại") } }
                .onFailure { feedback("Không thể lưu bản nháp: ${it.message}") }
        }
    }
    fun send() {
        val s = _state.value; if (s.sending) return; val d = s.document ?: return
        if (s.sending) return
        if (!s.connected) { feedback("Kết nối thiết bị trước khi gửi."); return }
        // Firmware transport is fixed at 64×32. Do not silently rescale users' artwork.
        if (d.width != 64 || d.height != 32) { feedback("Giao thức LED hiện tại chỉ nhận 64×32. Hãy lưu bản nháp để giữ nguyên kích thước."); return }
        _state.update { it.copy(sending = true, feedback = "Đang lưu vào bộ nhớ LED…") }
        viewModelScope.launch {
            try {
                writes.withLock {
                val epoch = generation
                coroutineScope {
                    val ack = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(30_000) {
                        ble.getTelemetryEvent().first { it is TelemetryEvent.UploadComplete || it is TelemetryEvent.UploadError }
                    } }
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
            } catch (e: TimeoutCancellationException) { feedback("Chưa nhận xác nhận từ LED. Kiểm tra kết nối trước khi thử lại.") }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { feedback("Không gửi được: ${e.message}") }
            finally { _state.update { it.copy(sending = false) }; changed.trySend(Unit) }
        }
    }
}
