package com.carled.app.presentation.screens.upload

data class UploadUiState(
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,        // 0.0 → 1.0
    val selectedImageUri: String? = null,
    val previewBitmap: Any? = null,         // android.graphics.Bitmap (Any để tránh Android import trong data class)

    // Telemetry events
    val uploadExpectedBytes: Int = 0,
    val uploadStatus: UploadStatus = UploadStatus.Idle,
    val feedbackMessage: String? = null
)

enum class UploadStatus {
    Idle,
    Starting,       // upload_started received
    Uploading,      // chunks being sent
    Complete,       // upload_complete received
    Error           // upload_error received
}
