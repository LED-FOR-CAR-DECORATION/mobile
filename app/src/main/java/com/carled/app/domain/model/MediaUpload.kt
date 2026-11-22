package com.carled.app.domain.model

data class CreateSignedUploadUrlRequest(
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val purpose: String,
    val targetId: Long? = null
)

data class SignedUploadUrlResponse(
    val signedUploadUrl: String,
    val publicUrl: String? = null,
    val objectKey: String,
    val uploadHeaders: Map<String, String> = emptyMap(),
    val expiresAt: String
)