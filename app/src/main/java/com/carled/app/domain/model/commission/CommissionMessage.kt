package com.carled.app.domain.model.commission

data class CommissionMessageResponse(
    val messageId: Long,
    val commissionId: Long,
    val senderId: Long? = null, // null for SYSTEM messages — confirmed by spec
    val senderRole: String,
    val messageType: String,
    val content: String? = null,
    val mediaUrl: String? = null,
    val metadataJson: String? = null,
    val createdAt: String? = null
)

/** Only TEXT/IMAGE are ever sent by the customer — DELIVERABLE_CARD/EXTRA_FEE_CARD/SYSTEM are server-originated. */
data class CreateCommissionMessageRequest(
    val messageType: String,
    val content: String? = null,
    val mediaUrl: String? = null
)