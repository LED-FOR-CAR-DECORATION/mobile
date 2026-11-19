package com.carled.app.domain.model

data class MediaUploadUrlRequest(
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val purpose: String,       // e.g. COMMISSION_REFERENCE, COMMISSION_CHAT, CHARACTER_ASSET
    val targetId: Long? = null // ASSUMPTION: nullable — see note on pre-creation uploads
)

data class MediaUploadUrlResponse(
    val signedUploadUrl: String,
    val publicUrl: String,
    val objectKey: String,
    val expiresAt: String
)