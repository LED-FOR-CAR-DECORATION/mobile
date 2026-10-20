package com.carled.app.presentation.screens.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.*
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class UploadViewModel(private val bleUseCases: BleUseCases) : ViewModel() {
    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState = _uiState.asStateFlow()
    val connection = bleUseCases.getConnectionState()
    private var original: Bitmap? = null
    private var loadJob: Job? = null
    private var processJob: Job? = null

    fun onImageSelected(context: Context, uri: Uri) {
        if (_uiState.value.isUploading) return
        loadJob?.cancel(); processJob?.cancel()
        _uiState.update { it.copy(isLoadingImage = true, previewBitmap = null, previewDocument = null, feedbackMessage = null, uploadStatus = UploadStatus.Idle) }
        loadJob = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
                    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Định dạng ảnh chưa được hỗ trợ" }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
                    context.contentResolver.openInputStream(uri).use { requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })) }
                }
                ensureActive(); original = bitmap
                val isGif = context.contentResolver.getType(uri) == "image/gif"
                _uiState.update { it.copy(selectedImageUri = uri.toString(), isLoadingImage = false, imageNote = if (isGif) "GIF: chiếu khung hình đầu tiên, giống công cụ chiếu ảnh hiện tại." else null) }
                process()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { original = null; _uiState.update { it.copy(isLoadingImage = false, isProcessing = false, feedbackMessage = "Không đọc được ảnh. Hãy chọn lại ảnh JPG, PNG hoặc GIF.") } }
        }
    }

    fun loadMockAsset(context: Context, asset: String) {
        if (original != null) return
        _uiState.update { it.copy(isLoadingImage = true, fitMode = FitMode.CONTAIN) }
        loadJob = viewModelScope.launch {
            try {
                original = withContext(Dispatchers.IO) { context.assets.open(asset).use { requireNotNull(BitmapFactory.decodeStream(it)) } }
                _uiState.update { it.copy(isLoadingImage = false, imageNote = "Nội dung mô phỏng: chiếu ảnh mẫu tĩnh. File hoạt ảnh và quyền sở hữu thật sẽ lấy từ BE.") }
                process()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isLoadingImage = false, feedbackMessage = "Không đọc được ảnh mẫu Character.") } }
        }
    }

    fun setFitMode(mode: FitMode) { if (!_uiState.value.isUploading) { _uiState.update { it.copy(fitMode = mode) }; process() } }
    fun setDitherMode(mode: DitherMode) { if (!_uiState.value.isUploading) { _uiState.update { it.copy(ditherMode = mode) }; process() } }
    fun setBrightness(percent: Int) { if (!_uiState.value.isUploading) { _uiState.update { it.copy(brightnessPercent = percent.coerceIn(40, 220)) }; process() } }
    fun setContrast(percent: Int) { if (!_uiState.value.isUploading) { _uiState.update { it.copy(contrastPercent = percent.coerceIn(40, 220)) }; process() } }

    private fun process() {
        val input = original ?: return
        processJob?.cancel()
        val options = _uiState.value
        _uiState.update { it.copy(isProcessing = true, uploadStatus = UploadStatus.Idle, feedbackMessage = null) }
        processJob = viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.Default) {
                    val pixels = IntArray(input.width * input.height)
                    input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
                    PhotoRasterizer.process(pixels, input.width, input.height, when(options.fitMode) {
                        FitMode.STRETCH -> PhotoFit.STRETCH; FitMode.COVER -> PhotoFit.COVER; FitMode.CONTAIN -> PhotoFit.CONTAIN
                    }, options.brightnessPercent, options.contrastPercent, options.ditherMode == DitherMode.FLOYD_STEINBERG)
                }
                ensureActive()
                val preview = Bitmap.createBitmap(document.pixels.toIntArray(), 64, 32, Bitmap.Config.ARGB_8888)
                _uiState.update { it.copy(previewBitmap = preview, previewDocument = document, isProcessing = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isProcessing = false, previewDocument = null, previewBitmap = null, feedbackMessage = "Không xử lý được ảnh. Hãy chọn lại.") } }
        }
    }

    fun startUpload() {
        val state = _uiState.value
        if (state.isUploading || state.isProcessing || state.isLoadingImage) return
        val document = state.previewDocument ?: return
        if (connection.value !is ConnectionState.Connected) { _uiState.update { it.copy(feedbackMessage = "Kết nối LED trước khi chiếu ảnh.") }; return }
        _uiState.update { it.copy(isUploading = true, uploadProgress = 0f, uploadStatus = UploadStatus.Uploading, feedbackMessage = "Đang gửi ảnh 64×32…") }
        viewModelScope.launch {
            try {
                coroutineScope {
                    val ack = async(start = CoroutineStart.UNDISPATCHED) { withTimeout(30_000) {
                        bleUseCases.getTelemetryEvent().first { it is TelemetryEvent.UploadComplete || it is TelemetryEvent.UploadError }
                    } }
                    bleUseCases.uploadProjection(document.rgb565())
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
