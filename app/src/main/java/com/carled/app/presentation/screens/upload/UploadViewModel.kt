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
import kotlin.math.roundToInt

class UploadViewModel(private val bleUseCases: BleUseCases) : ViewModel() {
    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState = _uiState.asStateFlow()
    val connection = bleUseCases.getConnectionState()
    private var original: Bitmap? = null
    private var originalFrames: List<Bitmap> = emptyList()
    private var loadJob: Job? = null
    private var processJob: Job? = null
    private var animationJob: Job? = null

    fun onImageSelected(context: Context, uri: Uri) {
        if (_uiState.value.isUploading) return
        loadJob?.cancel()
        processJob?.cancel()
        animationJob?.cancel()

        _uiState.update {
            it.copy(
                isLoadingImage = true,
                previewBitmap = null,
                previewDocument = null,
                previewFrames = emptyList(),
                isAnimatedGif = false,
                gifFrameCount = 1,
                currentFrameIndex = 0,
                feedbackMessage = null,
                uploadStatus = UploadStatus.Idle
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val mimeType = context.contentResolver.getType(uri)
                val isGif = mimeType == "image/gif" || uri.toString().endsWith(".gif", ignoreCase = true)

                if (isGif) {
                    val gifResult = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { GifDecoder.decode(it) }
                    }

                    if (gifResult != null && gifResult.isAnimated) {
                        ensureActive()
                        original = gifResult.first
                        originalFrames = gifResult.frames
                        val fps = (1000f / gifResult.frameDelayMs).roundToInt()
                        _uiState.update {
                            it.copy(
                                selectedImageUri = uri.toString(),
                                isLoadingImage = false,
                                isAnimatedGif = true,
                                gifFrameCount = gifResult.frames.size,
                                gifFrameDelayMs = gifResult.frameDelayMs,
                                imageNote = "GIF Hoạt ảnh: ${gifResult.frames.size} frame • ${gifResult.frameDelayMs}ms/frame (~$fps FPS)"
                            )
                        }
                        process()
                        return@launch
                    }
                }

                // Ảnh tĩnh (JPG, PNG, WebP hoặc GIF 1 frame)
                val bitmap = withContext(Dispatchers.IO) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
                    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Định dạng ảnh chưa được hỗ trợ" }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
                    context.contentResolver.openInputStream(uri).use {
                        requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }))
                    }
                }
                ensureActive()
                original = bitmap
                originalFrames = listOf(bitmap)
                _uiState.update {
                    it.copy(
                        selectedImageUri = uri.toString(),
                        isLoadingImage = false,
                        isAnimatedGif = false,
                        gifFrameCount = 1,
                        imageNote = if (isGif) "GIF tĩnh (1 frame duy nhất)" else null
                    )
                }
                process()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                original = null
                originalFrames = emptyList()
                _uiState.update {
                    it.copy(
                        isLoadingImage = false,
                        isProcessing = false,
                        feedbackMessage = "Không đọc được ảnh. Hãy chọn lại ảnh JPG, PNG hoặc GIF."
                    )
                }
            }
        }
    }

    fun loadMockAsset(context: Context, asset: String) {
        if (original != null) return
        _uiState.update { it.copy(isLoadingImage = true, fitMode = FitMode.CONTAIN) }
        loadJob = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.assets.open(asset).use { requireNotNull(BitmapFactory.decodeStream(it)) }
                }
                original = bitmap
                originalFrames = listOf(bitmap)
                _uiState.update {
                    it.copy(
                        isLoadingImage = false,
                        isAnimatedGif = false,
                        gifFrameCount = 1,
                        imageNote = "Nội dung mô phỏng: chiếu ảnh mẫu tĩnh. File hoạt ảnh và quyền sở hữu thật sẽ lấy từ BE."
                    )
                }
                process()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingImage = false,
                        feedbackMessage = "Không đọc được ảnh mẫu Character."
                    )
                }
            }
        }
    }

    fun setFitMode(mode: FitMode) {
        if (!_uiState.value.isUploading) {
            _uiState.update { it.copy(fitMode = mode) }
            process()
        }
    }

    fun setDitherMode(mode: DitherMode) {
        if (!_uiState.value.isUploading) {
            _uiState.update { it.copy(ditherMode = mode) }
            process()
        }
    }

    fun setBrightness(percent: Int) {
        if (!_uiState.value.isUploading) {
            _uiState.update { it.copy(brightnessPercent = percent.coerceIn(40, 220)) }
            process()
        }
    }

    fun setContrast(percent: Int) {
        if (!_uiState.value.isUploading) {
            _uiState.update { it.copy(contrastPercent = percent.coerceIn(40, 220)) }
            process()
        }
    }

    fun setFrameDelay(delayMs: Int) {
        if (!_uiState.value.isUploading) {
            val clamped = delayMs.coerceIn(20, 1000)
            val fps = (1000f / clamped).roundToInt()
            _uiState.update {
                it.copy(
                    gifFrameDelayMs = clamped,
                    imageNote = "GIF Hoạt ảnh: ${it.gifFrameCount} frame • ${clamped}ms/frame (~$fps FPS)"
                )
            }
            startAnimationPreview()
        }
    }

    private fun process() {
        val frames = originalFrames.ifEmpty { original?.let { listOf(it) } ?: emptyList() }
        if (frames.isEmpty()) return

        processJob?.cancel()
        animationJob?.cancel()

        val options = _uiState.value
        _uiState.update { it.copy(isProcessing = true, uploadStatus = UploadStatus.Idle, feedbackMessage = null) }

        processJob = viewModelScope.launch {
            try {
                val processedDocs = withContext(Dispatchers.Default) {
                    frames.map { input ->
                        val pixels = IntArray(input.width * input.height)
                        input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
                        PhotoRasterizer.process(
                            pixels, input.width, input.height,
                            when (options.fitMode) {
                                FitMode.STRETCH -> PhotoFit.STRETCH
                                FitMode.COVER -> PhotoFit.COVER
                                FitMode.CONTAIN -> PhotoFit.CONTAIN
                            },
                            options.brightnessPercent,
                            options.contrastPercent,
                            options.ditherMode == DitherMode.FLOYD_STEINBERG
                        )
                    }
                }
                ensureActive()

                val firstDoc = processedDocs.first()
                val firstPreview = Bitmap.createBitmap(firstDoc.pixels.toIntArray(), 64, 32, Bitmap.Config.ARGB_8888)

                _uiState.update {
                    it.copy(
                        previewBitmap = firstPreview,
                        previewDocument = firstDoc,
                        previewFrames = processedDocs,
                        currentFrameIndex = 0,
                        isProcessing = false
                    )
                }

                if (options.isAnimatedGif && processedDocs.size > 1) {
                    startAnimationPreview()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        previewDocument = null,
                        previewBitmap = null,
                        previewFrames = emptyList(),
                        feedbackMessage = "Không xử lý được ảnh. Hãy chọn lại."
                    )
                }
            }
        }
    }

    private fun startAnimationPreview() {
        animationJob?.cancel()
        val frames = _uiState.value.previewFrames
        if (frames.size <= 1) return

        animationJob = viewModelScope.launch {
            var idx = _uiState.value.currentFrameIndex
            while (isActive) {
                val delayMs = _uiState.value.gifFrameDelayMs.toLong().coerceIn(20L, 1000L)
                delay(delayMs)
                idx = (idx + 1) % frames.size
                val doc = frames[idx]
                val bmp = Bitmap.createBitmap(doc.pixels.toIntArray(), 64, 32, Bitmap.Config.ARGB_8888)
                _uiState.update {
                    it.copy(
                        previewBitmap = bmp,
                        previewDocument = doc,
                        currentFrameIndex = idx
                    )
                }
            }
        }
    }

    fun startUpload() {
        val state = _uiState.value
        if (state.isUploading || state.isProcessing || state.isLoadingImage) return
        val document = state.previewDocument ?: return
        if (connection.value !is ConnectionState.Connected) {
            _uiState.update { it.copy(feedbackMessage = "Kết nối LED trước khi chiếu ảnh.") }
            return
        }

        val isAnimation = state.isAnimatedGif && state.previewFrames.size > 1
        val frameCount = if (isAnimation) state.previewFrames.size else 1
        val delayMs = state.gifFrameDelayMs.coerceIn(20, 65535)

        _uiState.update {
            it.copy(
                isUploading = true,
                uploadProgress = 0f,
                uploadStatus = UploadStatus.Uploading,
                feedbackMessage = if (isAnimation) "Đang gửi hoạt ảnh GIF $frameCount frame lên LED…" else "Đang gửi ảnh 64×32…"
            )
        }

        viewModelScope.launch {
            try {
                coroutineScope {
                    val ack = async(start = CoroutineStart.UNDISPATCHED) {
                        withTimeout(60_000) {
                            bleUseCases.getTelemetryEvent().first {
                                it is TelemetryEvent.UploadComplete || it is TelemetryEvent.UploadError
                            }
                        }
                    }

                    if (isAnimation) {
                        val allRgb565 = ByteArray(64 * 32 * 2 * frameCount)
                        var offset = 0
                        for (frameDoc in state.previewFrames) {
                            val frameBytes = frameDoc.rgb565()
                            frameBytes.copyInto(allRgb565, offset)
                            offset += frameBytes.size
                        }
                        bleUseCases.uploadProjection(allRgb565, frameCount, delayMs)
                    } else {
                        bleUseCases.uploadProjection(document.rgb565())
                    }

                    _uiState.update { it.copy(feedbackMessage = "Đã gửi dữ liệu; chờ LED xác nhận…") }

                    when (val result = ack.await()) {
                        is TelemetryEvent.UploadComplete -> {
                            _uiState.update {
                                it.copy(
                                    uploadStatus = UploadStatus.Complete,
                                    uploadProgress = 1f,
                                    feedbackMessage = if (isAnimation) {
                                        "LED đã xác nhận nhận hoạt ảnh GIF ($frameCount frame)."
                                    } else {
                                        "LED đã xác nhận nhận ảnh."
                                    }
                                )
                            }
                        }
                        is TelemetryEvent.UploadError -> {
                            _uiState.update {
                                it.copy(
                                    uploadStatus = UploadStatus.Error,
                                    feedbackMessage = "LED báo lỗi: ${result.error}"
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.update {
                    it.copy(
                        uploadStatus = UploadStatus.Error,
                        feedbackMessage = "Chưa nhận xác nhận từ LED. Kiểm tra kết nối trước khi thử lại."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        uploadStatus = UploadStatus.Error,
                        feedbackMessage = "Không gửi được ảnh: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }

    fun reloadProjection() {
        viewModelScope.launch {
            try {
                bleUseCases.reloadProjection()
            } catch (e: Exception) {
                _uiState.update { it.copy(feedbackMessage = "Không tải lại được ảnh: ${e.message}") }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        processJob?.cancel()
        animationJob?.cancel()
    }
}
