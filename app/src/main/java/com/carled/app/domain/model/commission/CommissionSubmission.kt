package com.carled.app.domain.model.commission

import com.carled.app.domain.model.PaymentSessionResponse

data class SelectedOptionInput(
    val configId: Long,
    val optionIds: List<Long>? = null,
    val quantity: Int? = null,
    val textValue: String? = null
)

data class CommissionAttachmentInput(
    val imageUrl: String,
    val caption: String? = null
)

data class CreateCommissionRequest(
    val title: String,
    val description: String? = null,
    val tierCode: String? = null, // server defaults to CUSTOM when omitted
    val selectedOptions: List<SelectedOptionInput> = emptyList(),
    val attachments: List<CommissionAttachmentInput> = emptyList(),
    val openPaymentImmediately: Boolean = false
)

data class CommissionAttachment(
    val attachmentId: Long,
    val imageUrl: String,
    val caption: String? = null,
    val displayOrder: Int = 0,
    val createdAt: String? = null
)

data class DeliveredPreview(
    val submissionId: Long,
    val versionNumber: Int,
    val imageUrl: String,
    val designerNote: String? = null,
    val status: String,
    val deliveredAt: String? = null,
    val createdAt: String? = null
)

data class CommissionResponse(
    val commissionId: Long,
    val customerId: Long,
    val title: String,
    val description: String? = null,
    val tierCode: String,
    val targetMatrixDimensions: String? = null,
    val priceBreakdownJson: String? = null, // confirmed a JSON-encoded string, not an object
    val initialPrice: Double,
    val extraRevisionFee: Double = 0.0,
    val totalPrice: Double,
    val revisionsUsed: Int = 0,
    val maxRevisions: Int = 0,
    val status: String,
    val activePaymentCode: String? = null,
    val approvedSubmissionId: Long? = null,
    val rejectionReason: String? = null,
    val chatActive: Boolean = false,
    val quoteExpiresAt: String? = null,
    val slaDueAt: String? = null,
    val autoCompleteAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val attachments: List<CommissionAttachment> = emptyList(),
    val deliveredPreviews: List<DeliveredPreview> = emptyList()
)

data class CommissionSummaryResponse(
    val commissionId: Long,
    val title: String,
    val tierCode: String,
    val targetMatrixDimensions: String? = null,
    val status: String,
    val initialPrice: Double,
    val extraRevisionFee: Double = 0.0,
    val totalPrice: Double,
    val revisionsUsed: Int = 0,
    val maxRevisions: Int = 0,
    val activePaymentCode: String? = null,
    val quoteExpiresAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ApproveDeliverableCardResponse(
    val commissionId: Long,
    val commissionStatus: String,
    val approvedSubmissionId: Long,
    val versionNumber: Int,
    val submissionStatus: String,
    val completedAt: String,
    val systemMessage: CommissionMessageResponse
)

data class ConfirmRevisionCardRequest(val messageId: Long)

data class ConfirmRevisionCardResponse(
    val commissionId: Long,
    val messageId: Long,
    val status: String,
    val revisionsUsed: Int,
    val extraRevisionFee: Double,
    val paymentSession: PaymentSessionResponse? = null
)

private val CHAT_ACTIVE_STATUSES = setOf("PAID", "IN_PROGRESS", "INTERNAL_REVIEW", "INTERNAL_REVISION_REQUIRED", "PREVIEW_DELIVERED", "AWAITING_EXTRA_PAYMENT", "CUSTOMER_REVISION_REQUESTED", "AWAITING_ESTIMATE")
val CommissionSummaryResponse.isChatActive: Boolean get() = status in CHAT_ACTIVE_STATUSES