package com.carled.app.presentation.screens.upload

import android.graphics.Bitmap

enum class FitMode(val displayName: String) {
    STRETCH("Stretch (Kéo dãn toàn khung)"),
    COVER("Cover (Cắt vừa tỉ lệ 2:1)"),
    CONTAIN("Contain (Giữ nguyên tỉ lệ)")
}

enum class DitherMode(val displayName: String) {
    NONE("Không Dithering (Mịn)"),
    FLOYD_STEINBERG("Floyd-Steinberg (Pixel Art)")
}

data class UploadUiState(
    val isProcessing: Boolean = false,
    val isLoadingImage: Boolean = false,
    val imageNote: String? = null,
    val previewDocument: com.carled.app.domain.model.PixelDocument? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f, // 0.0 → 1.0
    val selectedImageUri: String? = null,
    val previewBitmap: Bitmap? = null,
    val fitMode: FitMode = FitMode.STRETCH,
    val ditherMode: DitherMode = DitherMode.NONE,
    val brightnessPercent: Int = 100, // 40..220%
    val contrastPercent: Int = 100, // 40..220%

    // GIF animation support
    val isAnimatedGif: Boolean = false,
    val gifFrameCount: Int = 1,
    val gifFrameDelayMs: Int = 100,
    val currentFrameIndex: Int = 0,
    val previewFrames: List<com.carled.app.domain.model.PixelDocument> = emptyList(),

    // Telemetry events
    val uploadExpectedBytes: Int = 0,
    val uploadStatus: UploadStatus = UploadStatus.Idle,
    val feedbackMessage: String? = null
)

enum class UploadStatus {
    Idle,
    Starting, // upload_started received
    Uploading, // chunks being sent
    Complete, // upload_complete received
    Error // upload_error received
}
