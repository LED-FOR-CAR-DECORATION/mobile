package com.carled.app.presentation.screens.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.core.di.ServiceLocator
import com.carled.app.data.local.PixelDraft
import com.carled.app.data.media.GifFrameDecoder
import com.carled.app.domain.model.CustomerOwnedCharacterResponse
import com.carled.app.domain.model.PhotoFit
import com.carled.app.domain.model.PhotoRasterizer
import com.carled.app.domain.model.PixelDocument
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.domain.model.scrollFrames
import com.carled.app.domain.repository.CharacterRepository
import com.carled.app.domain.usecase.ble.BleUseCases
import com.carled.app.presentation.screens.upload.DitherMode
import com.carled.app.presentation.screens.upload.FitMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

sealed class QuickProjectionStatus {
    object Idle : QuickProjectionStatus()
    object Connecting : QuickProjectionStatus()
    object Downloading : QuickProjectionStatus()
    object Decoding : QuickProjectionStatus()
    object Sending : QuickProjectionStatus()
    object WaitingAck : QuickProjectionStatus()
    object Success : QuickProjectionStatus()
    data class Error(val message: String) : QuickProjectionStatus()

    val isBusy: Boolean
        get() = this is Connecting || this is Downloading || this is Decoding || this is Sending || this is WaitingAck
}

data class LibraryProjectionEditorState(
    val isPreparing: Boolean = false,
    val fitMode: FitMode = FitMode.CONTAIN,
    val ditherMode: DitherMode = DitherMode.NONE,
    val brightnessPercent: Int = 100,
    val contrastPercent: Int = 100,
    val frameCount: Int = 0,
    val frameDelayMs: Int = 200,
    val previewDocument: PixelDocument? = null,
    val previewBitmap: Bitmap? = null,
    val status: QuickProjectionStatus = QuickProjectionStatus.Idle
)

internal class ContentLibraryViewModel(
    private val repository: CharacterRepository = ServiceLocator.characterRepository,
    private val ble: BleUseCases = ServiceLocator.bleUseCases,
    private val httpClient: OkHttpClient = ServiceLocator.plainHttpClient
) : ViewModel() {

    private val _ownedCharacters = MutableStateFlow<Resource<List<CustomerOwnedCharacterResponse>>>(Resource.Loading)
    val ownedCharacters: StateFlow<Resource<List<CustomerOwnedCharacterResponse>>> = _ownedCharacters.asStateFlow()

    private val _editorState = MutableStateFlow(LibraryProjectionEditorState())
    val editorState: StateFlow<LibraryProjectionEditorState> = _editorState.asStateFlow()

    val quickProjection: StateFlow<QuickProjectionStatus>
        get() = MutableStateFlow(_editorState.value.status).asStateFlow()

    private var sourceFrames: List<GifFrameDecoder.DecodedFrame> = emptyList()
    private var preparedDocuments: List<PixelDocument> = emptyList()
    private var activeWidth = 64
    private var activeHeight = 32
    private var activeDelayMs = 200

    private var prepareJob: Job? = null
    private var rasterizeJob: Job? = null
    private var animationJob: Job? = null
    private var quickProjectionJob: Job? = null

    init { loadOwnedCharacters() }

    fun loadOwnedCharacters() {
        viewModelScope.launch {
            _ownedCharacters.value = Resource.Loading
            _ownedCharacters.value = repository.getOwnedCharacters()
        }
    }

    fun prepareCharacter(character: CustomerOwnedCharacterResponse) {
        prepareJob?.cancel(); rasterizeJob?.cancel(); animationJob?.cancel()
        sourceFrames = emptyList(); preparedDocuments = emptyList()
        activeWidth = 64; activeHeight = 32
        activeDelayMs = character.frameDelayMs.coerceIn(1, 65535)

        _editorState.update {
            it.copy(
                isPreparing = true,
                fitMode = FitMode.CONTAIN,
                ditherMode = DitherMode.NONE,
                brightnessPercent = 100,
                contrastPercent = 100,
                frameCount = 0,
                frameDelayMs = activeDelayMs,
                previewDocument = null,
                previewBitmap = null,
                status = QuickProjectionStatus.Downloading
            )
        }

        prepareJob = viewModelScope.launch {
            try {
                val bytes = downloadBytes(character.previewGifUrl)
                _editorState.update { it.copy(status = QuickProjectionStatus.Decoding) }
                val decoded = withContext(Dispatchers.Default) {
                    val gifFrames = runCatching { GifFrameDecoder.decodeFrames(bytes, activeDelayMs) }.getOrNull()
                    if (!gifFrames.isNullOrEmpty()) {
                        gifFrames
                    } else {
                        val bmp = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
                            "Không đọc được định dạng ảnh."
                        }
                        val pixels = IntArray(bmp.width * bmp.height)
                        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                        listOf(GifFrameDecoder.DecodedFrame(pixels, bmp.width, bmp.height))
                    }
                }
                ensureActive()
                sourceFrames = decoded
                _editorState.update {
                    it.copy(
                        isPreparing = false,
                        frameCount = decoded.size,
                        frameDelayMs = activeDelayMs,
                        status = QuickProjectionStatus.Idle
                    )
                }
                rasterizeCurrentFrames()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _editorState.update {
                    it.copy(
                        isPreparing = false,
                        status = QuickProjectionStatus.Error(e.message ?: "Không tải được ảnh Character.")
                    )
                }
            }
        }
    }

    fun prepareDraft(draft: PixelDraft) {
        prepareJob?.cancel(); rasterizeJob?.cancel(); animationJob?.cancel()
        sourceFrames = emptyList(); preparedDocuments = emptyList()
        activeWidth = draft.document.width.coerceAtLeast(1)
        activeHeight = draft.document.height.coerceAtLeast(1)

        _editorState.update {
            it.copy(
                isPreparing = true,
                fitMode = FitMode.CONTAIN,
                ditherMode = DitherMode.NONE,
                brightnessPercent = 100,
                contrastPercent = 100,
                frameCount = 0,
                frameDelayMs = 80,
                previewDocument = null,
                previewBitmap = null,
                status = QuickProjectionStatus.Decoding
            )
        }

        prepareJob = viewModelScope.launch {
            try {
                val scrollDocs = withContext(Dispatchers.Default) { draft.document.scrollFrames() }
                ensureActive()
                activeDelayMs = if (scrollDocs.size > 1) 80 else 200
                sourceFrames = scrollDocs.map { doc ->
                    GifFrameDecoder.DecodedFrame(doc.pixels.toIntArray(), doc.width, doc.height)
                }
                _editorState.update {
                    it.copy(
                        isPreparing = false,
                        frameCount = sourceFrames.size,
                        frameDelayMs = activeDelayMs,
                        status = QuickProjectionStatus.Idle
                    )
                }
                rasterizeCurrentFrames()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _editorState.update {
                    it.copy(
                        isPreparing = false,
                        status = QuickProjectionStatus.Error(e.message ?: "Không đọc được bản nháp.")
                    )
                }
            }
        }
    }

    fun setFitMode(mode: FitMode) {
        if (_editorState.value.status.isBusy) return
        _editorState.update { it.copy(fitMode = mode) }
        rasterizeCurrentFrames()
    }

    fun setDitherMode(mode: DitherMode) {
        if (_editorState.value.status.isBusy) return
        _editorState.update { it.copy(ditherMode = mode) }
        rasterizeCurrentFrames()
    }

    fun setBrightness(percent: Int) {
        if (_editorState.value.status.isBusy) return
        _editorState.update { it.copy(brightnessPercent = percent.coerceIn(40, 220)) }
        rasterizeCurrentFrames()
    }

    fun setContrast(percent: Int) {
        if (_editorState.value.status.isBusy) return
        _editorState.update { it.copy(contrastPercent = percent.coerceIn(40, 220)) }
        rasterizeCurrentFrames()
    }

    private fun rasterizeCurrentFrames() {
        val frames = sourceFrames.takeIf { it.isNotEmpty() } ?: return
        rasterizeJob?.cancel()
        animationJob?.cancel()
        val options = _editorState.value

        rasterizeJob = viewModelScope.launch {
            try {
                val fit = when (options.fitMode) {
                    FitMode.STRETCH -> PhotoFit.STRETCH
                    FitMode.COVER -> PhotoFit.COVER
                    FitMode.CONTAIN -> PhotoFit.CONTAIN
                }
                val dither = options.ditherMode == DitherMode.FLOYD_STEINBERG

                val docs = withContext(Dispatchers.Default) {
                    frames.map { frame ->
                        ensureActive()
                        PhotoRasterizer.process(
                            frame.pixels,
                            frame.width,
                            frame.height,
                            fit,
                            options.brightnessPercent,
                            options.contrastPercent,
                            dither
                        )
                    }
                }
                ensureActive()
                preparedDocuments = docs
                val first = docs.first()
                val bmp = Bitmap.createBitmap(first.pixels.toIntArray(), first.width, first.height, Bitmap.Config.ARGB_8888)
                _editorState.update {
                    it.copy(
                        previewDocument = first,
                        previewBitmap = bmp,
                        frameCount = docs.size
                    )
                }

                if (docs.size > 1) {
                    animationJob = viewModelScope.launch {
                        var index = 0
                        while (isActive) {
                            delay(activeDelayMs.toLong())
                            index = (index + 1) % docs.size
                            _editorState.update { it.copy(previewDocument = docs[index]) }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _editorState.update {
                    it.copy(status = QuickProjectionStatus.Error("Không xử lý được khung hình: ${e.message}"))
                }
            }
        }
    }

    fun resetQuickProjection() {
        prepareJob?.cancel()
        rasterizeJob?.cancel()
        animationJob?.cancel()
        quickProjectionJob?.cancel()
        sourceFrames = emptyList()
        preparedDocuments = emptyList()
        _editorState.value = LibraryProjectionEditorState()
    }

    fun quickProjectCharacter(character: CustomerOwnedCharacterResponse) {
        startQuickProjection {
            if (preparedDocuments.isEmpty()) {
                prepareCharacter(character)
                prepareJob?.join()
                rasterizeJob?.join()
            }
            val docs = preparedDocuments.takeIf { it.isNotEmpty() } ?: error("Chưa có khung hình sẵn sàng để chiếu.")
            val combined = withContext(Dispatchers.Default) {
                docs.map { it.rgb565() }.reduce { acc, f -> acc + f }
            }
            sendToLed(
                rgb565Data = combined,
                width = docs.first().width,
                height = docs.first().height,
                frameCount = docs.size,
                frameDelayMs = activeDelayMs
            )
        }
    }

    fun quickProjectDraft(draft: PixelDraft) {
        startQuickProjection {
            if (preparedDocuments.isEmpty()) {
                prepareDraft(draft)
                prepareJob?.join()
                rasterizeJob?.join()
            }
            val docs = preparedDocuments.takeIf { it.isNotEmpty() } ?: error("Chưa có khung hình sẵn sàng để chiếu.")
            val combined = withContext(Dispatchers.Default) {
                docs.map { it.rgb565() }.reduce { acc, f -> acc + f }
            }
            sendToLed(
                rgb565Data = combined,
                width = docs.first().width,
                height = docs.first().height,
                frameCount = docs.size,
                frameDelayMs = activeDelayMs
            )
        }
    }

    private fun startQuickProjection(block: suspend () -> Unit) {
        if (_editorState.value.status.isBusy) return

        quickProjectionJob?.cancel()
        quickProjectionJob = viewModelScope.launch {
            _editorState.update { it.copy(status = QuickProjectionStatus.Connecting) }
            if (ble.getConnectionState().value !is ConnectionState.Connected) {
                _editorState.update { it.copy(status = QuickProjectionStatus.Error("Kết nối thiết bị LED trước khi chiếu.")) }
                return@launch
            }
            try {
                coroutineScope {
                    val watcher = launch {
                        ble.getConnectionState().first { it !is ConnectionState.Connected }
                        this@coroutineScope.cancel(CancellationException("LED bị ngắt kết nối"))
                    }
                    try {
                        block()
                    } finally {
                        watcher.cancel()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _editorState.update {
                    it.copy(status = QuickProjectionStatus.Error("Chưa nhận xác nhận từ LED. Kiểm tra kết nối trước khi thử lại."))
                }
            } catch (e: CancellationException) {
                if (ble.getConnectionState().value !is ConnectionState.Connected) {
                    _editorState.update {
                        it.copy(status = QuickProjectionStatus.Error("Mất kết nối LED trong khi chiếu. Kết nối lại và thử lại."))
                    }
                } else {
                    throw e
                }
            } catch (e: Exception) {
                _editorState.update {
                    it.copy(status = QuickProjectionStatus.Error(e.message ?: "Không thể chiếu lên LED."))
                }
            }
        }
    }

    private suspend fun sendToLed(rgb565Data: ByteArray, width: Int, height: Int, frameCount: Int, frameDelayMs: Int) {
        _editorState.update { it.copy(status = QuickProjectionStatus.Sending) }
        coroutineScope {
            val ack = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(30_000) {
                    ble.getTelemetryEvent().first { it is TelemetryEvent.UploadComplete || it is TelemetryEvent.UploadError }
                }
            }
            ble.uploadProjection(rgb565Data, width, height, frameCount, frameDelayMs)
            _editorState.update { it.copy(status = QuickProjectionStatus.WaitingAck) }
            when (val event = ack.await()) {
                is TelemetryEvent.UploadComplete -> _editorState.update { it.copy(status = QuickProjectionStatus.Success) }
                is TelemetryEvent.UploadError -> _editorState.update {
                    it.copy(status = QuickProjectionStatus.Error("LED báo lỗi: ${event.error}"))
                }
                else -> Unit
            }
        }
    }

    private suspend fun downloadBytes(url: String): ByteArray = withTimeout(20_000) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Không tải được ảnh (lỗi ${response.code}).")
                    response.body?.bytes() ?: error("Phản hồi ảnh trống.")
                }
            } catch (e: IOException) {
                error("Không kết nối được tới máy chủ để tải ảnh. Kiểm tra mạng và thử lại.")
            }
        }
    }
}