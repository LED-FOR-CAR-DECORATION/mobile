package com.carled.app.presentation.screens.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.TelemetryEvent
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadViewModel(private val bleUseCases: BleUseCases) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private var rawOriginalBitmap: Bitmap? = null

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
                                feedbackMessage = "✅ ĐÃ CHIẾU ẢNH LÊN BẢNG LED THÀNH CÔNG! (Mode ${event.mode})"
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
     * Người dùng chọn ảnh từ Thư viện.
     */
    fun onImageSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                val original = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                rawOriginalBitmap = original
                _uiState.update {
                    it.copy(
                        selectedImageUri = uri.toString(),
                        uploadStatus = UploadStatus.Idle,
                        uploadProgress = 0f,
                        feedbackMessage = "Đã nạp ảnh gốc (${original.width}×${original.height} px)"
                    )
                }
                reprocessImage()
            }.onFailure { e ->
                _uiState.update { it.copy(feedbackMessage = "Không thể đọc ảnh: ${e.message}") }
            }
        }
    }

    fun setFitMode(mode: FitMode) {
        _uiState.update { it.copy(fitMode = mode) }
        reprocessImage()
    }

    fun setDitherMode(mode: DitherMode) {
        _uiState.update { it.copy(ditherMode = mode) }
        reprocessImage()
    }

    fun setBrightness(percent: Int) {
        _uiState.update { it.copy(brightnessPercent = percent) }
        reprocessImage()
    }

    fun setContrast(percent: Int) {
        _uiState.update { it.copy(contrastPercent = percent) }
        reprocessImage()
    }

    /**
     * Xử lý lại ảnh 64×32 theo các tham số: FitMode, Brightness, Contrast, Dithering
     */
    private fun reprocessImage() {
        val original = rawOriginalBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val fit = _uiState.value.fitMode
            val dither = _uiState.value.ditherMode
            val bri = _uiState.value.brightnessPercent / 100f
            val con = _uiState.value.contrastPercent / 100f

            // 1. Tạo Canvas 64×32 nền đen
            val targetW = 64
            val targetH = 32
            val resultBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBmp)
            canvas.drawColor(Color.BLACK)

            val srcW = original.width
            val srcH = original.height

            when (fit) {
                FitMode.STRETCH -> {
                    val destRect = Rect(0, 0, targetW, targetH)
                    canvas.drawBitmap(original, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                }
                FitMode.CONTAIN -> {
                    val scale = minOf(targetW.toFloat() / srcW, targetH.toFloat() / srcH)
                    val nw = (srcW * scale).toInt()
                    val nh = (srcH * scale).toInt()
                    val left = (targetW - nw) / 2
                    val top = (targetH - nh) / 2
                    val destRect = Rect(left, top, left + nw, top + nh)
                    canvas.drawBitmap(original, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                }
                FitMode.COVER -> {
                    val scale = maxOf(targetW.toFloat() / srcW, targetH.toFloat() / srcH)
                    val nw = (srcW * scale).toInt()
                    val nh = (srcH * scale).toInt()
                    val left = (targetW - nw) / 2
                    val top = (targetH - nh) / 2
                    val destRect = Rect(left, top, left + nw, top + nh)
                    canvas.drawBitmap(original, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                }
            }

            // 2. Chỉnh Brightness & Contrast từng pixel (theo công thức ble_image_importer.html)
            val pixels = IntArray(targetW * targetH)
            resultBmp.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

            for (i in pixels.indices) {
                val px = pixels[i]
                var r = (px shr 16) and 0xFF
                var g = (px shr 8) and 0xFF
                var b = px and 0xFF

                // Formula: v = ((v - 128) * con + 128) * bri
                r = (((r - 128f) * con + 128f) * bri).toInt().coerceIn(0, 255)
                g = (((g - 128f) * con + 128f) * bri).toInt().coerceIn(0, 255)
                b = (((b - 128f) * con + 128f) * bri).toInt().coerceIn(0, 255)

                pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }

            // 3. Thuật toán Floyd-Steinberg Dithering cho dải màu RGB565 (64×32)
            if (dither == DitherMode.FLOYD_STEINBERG) {
                applyFloydSteinbergDither(pixels, targetW, targetH)
            }

            resultBmp.setPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(previewBitmap = resultBmp)
                }
            }
        }
    }

    /**
     * Floyd-Steinberg Error Diffusion Algorithm
     * Phân phối lượng sai số lượng tử hóa màu: (x+1, y): 7/16, (x-1, y+1): 3/16, (x, y+1): 5/16, (x+1, y+1): 1/16
     */
    private fun applyFloydSteinbergDither(pixels: IntArray, w: Int, h: Int) {
        val rArr = FloatArray(w * h)
        val gArr = FloatArray(w * h)
        val bArr = FloatArray(w * h)

        for (i in pixels.indices) {
            val px = pixels[i]
            rArr[i] = ((px shr 16) and 0xFF).toFloat()
            gArr[i] = ((px shr 8) and 0xFF).toFloat()
            bArr[i] = (px and 0xFF).toFloat()
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val oldR = rArr[idx]
                val oldG = gArr[idx]
                val oldB = bArr[idx]

                // Lượng tử hóa về 5-bit R, 6-bit G, 5-bit B
                val newR = Math.round(oldR / 255f * 31f) * (255f / 31f)
                val newG = Math.round(oldG / 255f * 63f) * (255f / 63f)
                val newB = Math.round(oldB / 255f * 31f) * (255f / 31f)

                val errR = oldR - newR
                val errG = oldG - newG
                val errB = oldB - newB

                fun distribute(nx: Int, ny: Int, factor: Float) {
                    if (nx in 0 until w && ny in 0 until h) {
                        val nIdx = ny * w + nx
                        rArr[nIdx] = (rArr[nIdx] + errR * factor).coerceIn(0f, 255f)
                        gArr[nIdx] = (gArr[nIdx] + errG * factor).coerceIn(0f, 255f)
                        bArr[nIdx] = (bArr[nIdx] + errB * factor).coerceIn(0f, 255f)
                    }
                }

                distribute(x + 1, y, 7f / 16f)
                distribute(x - 1, y + 1, 3f / 16f)
                distribute(x, y + 1, 5f / 16f)
                distribute(x + 1, y + 1, 1f / 16f)

                val ir = newR.toInt().coerceIn(0, 255)
                val ig = newG.toInt().coerceIn(0, 255)
                val ib = newB.toInt().coerceIn(0, 255)
                pixels[idx] = (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
            }
        }
    }

    /**
     * Bắt đầu upload ảnh lên ESP32 qua BLE Binary Protocol
     */
    fun startUpload() {
        val bitmap = _uiState.value.previewBitmap ?: run {
            _uiState.update { it.copy(feedbackMessage = "Vui lòng chọn ảnh trước") }
            return
        }

        _uiState.update {
            it.copy(
                isUploading = true,
                uploadProgress = 0.05f,
                uploadStatus = UploadStatus.Uploading,
                feedbackMessage = "Đang truyền dữ liệu ảnh qua BLE (Opcode 0x10 -> 0x11 -> 0x12)..."
            )
        }

        viewModelScope.launch {
            runCatching {
                val rgb565Data = bitmapToLittleEndianRgb565(bitmap)
                bleUseCases.uploadProjection(rgb565Data)
                _uiState.update { it.copy(uploadProgress = 1f) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadStatus = UploadStatus.Error,
                        feedbackMessage = "❌ Lỗi upload: ${e.message}"
                    )
                }
            }
        }
    }

    fun reloadProjection() {
        viewModelScope.launch {
            bleUseCases.reloadProjection()
            _uiState.update { it.copy(feedbackMessage = "Đã gửi lệnh nạp lại ảnh từ SPIFFS Flash") }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    /**
     * Convert 64×32 Bitmap sang mảng RGB565 Little-Endian (2 bytes/pixel, 4096 bytes).
     * Chuẩn đồng bộ 100% với getRgb565Bytes() trong ble_image_importer.html và ESP32 NimBLE:
     *   col565 = ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3)
     *   bytes[0] = col565 & 0xFF        (Low byte first)
     *   bytes[1] = (col565 >> 8) & 0xFF (High byte second)
     */
    private fun bitmapToLittleEndianRgb565(bitmap: Bitmap): ByteArray {
        val data = ByteArray(64 * 32 * 2)
        var idx = 0
        for (y in 0 until 32) {
            for (x in 0 until 64) {
                val px = bitmap.getPixel(x, y)
                val r = (Color.red(px) shr 3) and 0x1F
                val g = (Color.green(px) shr 2) and 0x3F
                val b = (Color.blue(px) shr 3) and 0x1F
                val col565 = (r shl 11) or (g shl 5) or b
                data[idx++] = (col565 and 0xFF).toByte()
                data[idx++] = ((col565 shr 8) and 0xFF).toByte()
            }
        }
        return data
    }
}

