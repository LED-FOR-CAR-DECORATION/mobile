package com.carled.app.presentation.screens.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.di.ServiceLocator
import com.carled.app.domain.model.*
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request

class UploadViewModel(
    private val bleUseCases: BleUseCases,
    private val httpClient: OkHttpClient = ServiceLocator.plainHttpClient
) : ViewModel() {
    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState = _uiState.asStateFlow()
    val connection = bleUseCases.getConnectionState()
    private var original: Bitmap? = null
    private var animation: List<Bitmap> = emptyList()
    private var frameDelayMs = 200
    private var preparedFrames: List<PixelDocument> = emptyList()
    private var previewJob: Job? = null
    private var loadJob: Job? = null
    private var processJob: Job? = null

    fun onImageSelected(context: Context, uri: Uri) {
        if (_uiState.value.isUploading) return
        loadJob?.cancel(); processJob?.cancel(); previewJob?.cancel()
        original = null; animation = emptyList(); preparedFrames = emptyList()
        _uiState.update { it.copy(isLoadingImage = true, previewBitmap = null, previewDocument = null, feedbackMessage = null, uploadStatus = UploadStatus.Idle) }
        loadJob = viewModelScope.launch {
            try {
                val decoded = withContext(Dispatchers.IO) {
                    val signature = context.contentResolver.openInputStream(uri).use { input ->
                        val header = ByteArray(6)
                        var offset = 0
                        val source = requireNotNull(input)
                        while (offset < header.size) {
                            val count = source.read(header, offset, header.size - offset)
                            if (count < 0) break
                            offset += count
                        }
                        header.copyOf(offset)
                    }
                    if (GifTimeline.isGif(signature)) {
                        val bytes = context.contentResolver.openInputStream(uri).use { input ->
                            val source = requireNotNull(input)
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = source.read(buffer); if (count < 0) break
                                require(output.size() + count <= 8 * 1024 * 1024) { "GIF tối đa 8 MB" }
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        return@withContext com.carled.app.data.media.GifFrameReader.read(bytes)
                    }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
                    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Định dạng ảnh chưa được hỗ trợ" }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
                    val bitmap = context.contentResolver.openInputStream(uri).use { requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })) }
                    listOf(bitmap) to 200
                }
                ensureActive(); animation = decoded.first; original = animation.first(); frameDelayMs = decoded.second
                _uiState.update { it.copy(selectedImageUri = uri.toString(), isLoadingImage = false, imageNote = if (animation.size > 1) "GIF động · ${animation.size} khung hình · ${frameDelayMs} ms/khung. Tối đa 16 khung được lấy mẫu cho bộ nhớ LED." else null) }
                process()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { original = null; animation = emptyList(); _uiState.update { it.copy(isLoadingImage = false, isProcessing = false, feedbackMessage = "Không đọc được ảnh: ${e.message ?: "Hãy chọn lại JPG, PNG hoặc GIF."}") } }
        }
    }

    /**
     * Loads an owned character's preview GIF from the backend (CustomerOwnedCharacterResponse.previewGifUrl).
     * Only the first frame is decoded here, same convention as onImageSelected()'s GIF handling —
     * this manual editor flow (fit/dither/brightness controls, single still frame) is distinct from
     * "Quick projection" in the Content Library (ContentLibraryViewModel), which sends the full animation.
     */
    fun loadRemoteCharacter(context: Context, imageUrl: String) {
        if (original != null) return
        _uiState.update { it.copy(isLoadingImage = true, fitMode = FitMode.CONTAIN) }
        loadJob = viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(imageUrl).build()
                    httpClient.newCall(request).execute().use { response ->
                        require(response.isSuccessful) { "Không tải được ảnh (lỗi ${response.code})." }
                        requireNotNull(response.body?.bytes()) { "Phản hồi ảnh trống." }
                    }
                }
                ensureActive()
                original = withContext(Dispatchers.Default) {
                    requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) { "Định dạng ảnh chưa được hỗ trợ" }
                }
                _uiState.update {
                    it.copy(
                        isLoadingImage = false,
                        imageNote = "Character: chiếu khung hình đầu tiên. Dùng \"Quick projection\" ở Inventory để chiếu hoạt ảnh đầy đủ."
                    )
                }
                animation = emptyList(); frameDelayMs = 200
                process()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isLoadingImage = false, feedbackMessage = "Không tải được ảnh Character: ${e.message}") } }
        }
    }

    fun setFitMode(mode: FitMode) { if (!_uiState.value.isUploading) { _uiState.update { it.copy(fitMode = mode) }; process() } }
    fun setDitherMode(mode: DitherMode) { if (!_uiState.value.isUploading) { _uiState.update { it.copy(ditherMode = mode) }; process() } }
    fun setBrightness(percent: Int) { if (!_uiState.value.isUploading) { _uiState.update { it.copy(brightnessPercent = percent.coerceIn(40, 220)) }; process() } }
    fun setContrast(percent: Int) { if (!_uiState.value.isUploading) { _uiState.update { it.copy(contrastPercent = percent.coerceIn(40, 220)) }; process() } }

    private fun process() {
        val input = original ?: return
        processJob?.cancel()
        previewJob?.cancel(); preparedFrames = emptyList()
        val options = _uiState.value
        _uiState.update { it.copy(isProcessing = true, uploadStatus = UploadStatus.Idle, feedbackMessage = null) }
        processJob = viewModelScope.launch {
            try {
                val documents = withContext(Dispatchers.Default) {
                    (animation.ifEmpty { listOf(input) }).map { frame ->
                    ensureActive()
                    val input = frame
                    val pixels = IntArray(input.width * input.height)
                    input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
                    PhotoRasterizer.process(pixels, input.width, input.height, when(options.fitMode) {
                        FitMode.STRETCH -> PhotoFit.STRETCH; FitMode.COVER -> PhotoFit.COVER; FitMode.CONTAIN -> PhotoFit.CONTAIN
                    }, options.brightnessPercent, options.contrastPercent, options.ditherMode == DitherMode.FLOYD_STEINBERG)
                    }
                }
                ensureActive()
                preparedFrames = documents
                val document = documents.first()
                val preview = Bitmap.createBitmap(document.pixels.toIntArray(), 64, 32, Bitmap.Config.ARGB_8888)
                _uiState.update { it.copy(previewBitmap = preview, previewDocument = document, isProcessing = false) }
                if (documents.size > 1) previewJob = viewModelScope.launch {
                    var index = 0
                    while (isActive) {
                        delay(frameDelayMs.toLong()); index = (index + 1) % documents.size
                        _uiState.update { it.copy(previewDocument = documents[index]) }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isProcessing = false, previewDocument = null, previewBitmap = null, feedbackMessage = "Không xử lý được ảnh. Hãy chọn lại.") } }
        }
    }

    fun startUpload() {
        val state = _uiState.value
        if (state.isUploading || state.isProcessing || state.isLoadingImage) return
        val frames = preparedFrames.takeIf { it.isNotEmpty() } ?: return
        if (connection.value !is ConnectionState.Connected) { _uiState.update { it.copy(feedbackMessage = "Kết nối LED trước khi chiếu ảnh.") }; return }
        _uiState.update { it.copy(isUploading = true, uploadProgress = 0f, uploadStatus = UploadStatus.Uploading, feedbackMessage = "Đang gửi ảnh 64×32…") }
        viewModelScope.launch {
            try {
                coroutineScope {
                    val ack = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(30_000L + frames.size * 2_000L) {
                        bleUseCases.getTelemetryEvent().first { it is TelemetryEvent.UploadComplete || it is TelemetryEvent.UploadError }
                    } }
                    val data = ByteArray(frames.size * 64 * 32 * 2)
                    frames.forEachIndexed { index, frame -> frame.rgb565().copyInto(data, index * 64 * 32 * 2) }
                    bleUseCases.uploadProjection(data, width = 64, height = 32, frameCount = frames.size, frameDelayMs = frameDelayMs)
                    _uiState.update { it.copy(feedbackMessage = "Đã gửi dữ liệu; chờ LED xác nhận…") }
                    when (val result = ack.await()) {
                        is TelemetryEvent.UploadComplete -> _uiState.update { it.copy(uploadStatus = UploadStatus.Complete, uploadProgress = 1f, feedbackMessage = "LED đã xác nhận nhận ảnh.") }
                        is TelemetryEvent.UploadError -> _uiState.update { it.copy(uploadStatus = UploadStatus.Error, feedbackMessage = "LED báo lỗi: ${result.error}") }
                        else -> Unit
                    }
                }
            } catch (e: TimeoutCancellationException) { _uiState.update { it.copy(uploadStatus = UploadStatus.Error, feedbackMessage = "Chưa nhận xác nhận từ LED. Kiểm tra kết nối trước khi thử lại.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(uploadStatus = UploadStatus.Error, feedbackMessage = "Không gửi được ảnh: ${e.message}") } }
            finally { _uiState.update { it.copy(isUploading = false) } }
        }
    }
    fun reloadProjection() { viewModelScope.launch { try { bleUseCases.reloadProjection() } catch (e: Exception) { _uiState.update { it.copy(feedbackMessage = "Không tải lại được ảnh: ${e.message}") } } } }
    fun clearFeedback() { _uiState.update { it.copy(feedbackMessage = null) } }
}