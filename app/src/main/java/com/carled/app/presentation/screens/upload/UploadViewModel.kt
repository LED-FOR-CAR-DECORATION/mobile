package com.carled.app.presentation.screens.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadViewModel(private val bleUseCases: BleUseCases) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private var scaledBitmap: Bitmap? = null

    init {
        // Lắng nghe tất cả telemetry events liên quan đến upload
        viewModelScope.launch {
            bleUseCases.getTelemetryEvent().collect { event ->
                when (event) {
                    is TelemetryEvent.UploadStarted -> {
                        _uiState.update {
                            it.copy(
                                uploadStatus = UploadStatus.Starting,
                                uploadExpectedBytes = event.expected,
                                feedbackMessage = "ESP32 đã sẵn sàng nhận ảnh (${event.expected} bytes)"
                            )
                        }
                    }
                    is TelemetryEvent.UploadComplete -> {
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadProgress = 1f,
                                uploadStatus = UploadStatus.Complete,
                                feedbackMessage = "✅ Upload thành công! Đang chiếu mode ${event.mode}"
                            )
                        }
                    }
                    is TelemetryEvent.UploadError -> {
                        _uiState.update {
                            it.copy(
                                isUploading = false,
                                uploadStatus = UploadStatus.Error,
                                feedbackMessage = "❌ Lỗi upload: ${event.error}"
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * Người dùng chọn ảnh từ Gallery.
     * Scale về 64×32 và lưu preview.
     */
    fun onImageSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                val original = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val scaled = Bitmap.createScaledBitmap(original, 64, 32, true)
                scaledBitmap = scaled
                _uiState.update {
                    it.copy(
                        selectedImageUri = uri.toString(),
                        previewBitmap = scaled,
                        uploadStatus = UploadStatus.Idle,
                        uploadProgress = 0f,
                        feedbackMessage = "Ảnh đã sẵn sàng — 64×32 px • ${scaled.byteCount / 2} bytes (RGB565)"
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(feedbackMessage = "Không thể đọc ảnh: ${e.message}") }
            }
        }
    }

    /**
     * Bắt đầu upload ảnh lên ESP32.
     *
     * Quy trình:
     *   1. Convert Bitmap → RGB565 ByteArray
     *   2. Gửi qua BleDeviceRepository.uploadProjection()
     *   3. ESP32 xác nhận qua telemetry events (upload_started, upload_complete/error)
     */
    fun startUpload() {
        val bitmap = scaledBitmap ?: run {
            _uiState.update { it.copy(feedbackMessage = "Vui lòng chọn ảnh trước") }
            return
        }

        _uiState.update {
            it.copy(isUploading = true, uploadProgress = 0f, uploadStatus = UploadStatus.Uploading, feedbackMessage = "Đang upload ảnh lên ESP32...")
        }

        viewModelScope.launch {
            runCatching {
                val rgb565Data = bitmapToRgb565(bitmap)
                val chunkSize = 240
                val totalChunks = (rgb565Data.size + chunkSize - 1) / chunkSize

                // Cập nhật progress theo số chunk
                bleUseCases.uploadProjection(rgb565Data)

                // Cập nhật progress đơn giản (upload đã hoàn tất khi không exception)
                _uiState.update { it.copy(uploadProgress = 0.95f) }
                // uploadStatus sẽ được cập nhật bởi telemetry event "upload_complete"
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadStatus = UploadStatus.Error,
                        feedbackMessage = "❌ Lỗi gửi dữ liệu: ${e.message}"
                    )
                }
            }
        }
    }

    fun reloadProjection() {
        viewModelScope.launch {
            bleUseCases.reloadProjection()
            _uiState.update { it.copy(feedbackMessage = "Đã gửi lệnh reload projection từ Flash") }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    /**
     * Convert Android Bitmap sang mảng RGB565 (Big-Endian, 2 bytes/pixel).
     * Format: RRRRRGGGGGGBBBBB (16 bit)
     */
    private fun bitmapToRgb565(bitmap: Bitmap): ByteArray {
        val data = ByteArray(bitmap.width * bitmap.height * 2)
        var idx = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val px = bitmap.getPixel(x, y)
                val r = (Color.red(px) shr 3) and 0x1F
                val g = (Color.green(px) shr 2) and 0x3F
                val b = (Color.blue(px) shr 3) and 0x1F
                val rgb565 = (r shl 11) or (g shl 5) or b
                data[idx++] = ((rgb565 shr 8) and 0xFF).toByte()
                data[idx++] = (rgb565 and 0xFF).toByte()
            }
        }
        return data
    }
}
