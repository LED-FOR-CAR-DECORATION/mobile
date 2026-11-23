package com.carled.app.domain.model

data class PaymentSessionResponse(
    val paymentId: Long,
    val paymentCode: String,
    val targetType: String, // ORDER, COMMISSION_INITIAL, COMMISSION_REVISION
    val targetId: Long,
    val gateway: String,
    val qrUrl: String? = null,
    val bankName: String,
    val accountNumber: String,
    val accountName: String,
    val amount: Double,
    val status: String, // PENDING, SUCCEEDED, EXPIRED, CANCELLED, FAILED, REFUNDED
    val expiresAt: String? = null,
    val paidAt: String? = null,
    val createdAt: String? = null
)