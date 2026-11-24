package com.carled.app.domain.model

data class CustomerOwnedCharacterResponse(
    val characterId: Long,
    val characterName: String,
    val previewGifUrl: String,
    val frameCount: Int,
    val frameDelayMs: Int,
    val orderId: Long? = null,
    val storeProductId: Long? = null,
    val productName: String? = null,
    val purchasedPrice: Double? = null,
    val purchasedAt: String? = null
)