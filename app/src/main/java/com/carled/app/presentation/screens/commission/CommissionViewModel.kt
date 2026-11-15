package com.carled.app.presentation.screens.commission

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.UUID

internal enum class CommissionType(val label: String) { IMAGE("Image"), GIF("GIF") }

internal enum class CommissionSize(val label: String, val staticPriceVnd: Long) {
    SQUARE("32 × 32", 50_000L),
    WIDE("64 × 32", 100_000L)
}

internal const val COMMISSION_FRAME_PRICE_VND = 50_000L
// Mobile projects 64 × 32 RGB565 frames (4 KiB each). Eight frames use 32 KiB before filesystem overhead.
internal const val COMMISSION_MAX_FRAMES = 8

internal data class MockSePayPayment(
    val transferCode: String,
    val amountVnd: Long,
    val transactionId: String? = null
) {
    val isPaid: Boolean get() = transactionId != null
}

internal data class CommissionDraft(
    val type: CommissionType = CommissionType.IMAGE,
    val size: CommissionSize = CommissionSize.SQUARE,
    val description: String = "",
    val frameCount: String = "8",
    val references: List<Uri> = emptyList()
) {
    val priceVnd: Long?
        get() = when (type) {
            CommissionType.IMAGE -> size.staticPriceVnd
            CommissionType.GIF -> frameCount.toIntOrNull()
                ?.takeIf { it in 2..COMMISSION_MAX_FRAMES }
                ?.times(COMMISSION_FRAME_PRICE_VND)
        }

    val isValid: Boolean
        get() = description.isNotBlank() && priceVnd != null
}

internal data class CommissionMessage(
    val text: String = "",
    val outgoing: Boolean = false,
    val attachment: Uri? = null,
    val sampleImage: Boolean = false
) {
    val preview: String
        get() = if (text.isNotBlank()) text else "Image attachment"
}

internal data class CommissionRequest(
    val id: String,
    val draft: CommissionDraft,
    val completedSteps: Int = 1,
    val canceled: Boolean = false,
    val messages: List<CommissionMessage> = emptyList(),
    val notification: String? = null,
    val payment: MockSePayPayment? = null
) {
    val status: String
        get() = when {
            canceled -> "Canceled"
            payment?.isPaid == false -> "Awaiting payment"
            payment?.isPaid == true -> "Paid"
            completedSteps >= 6 -> "In progress"
            else -> "Submitted"
        }
}

internal class CommissionViewModel : ViewModel() {
    var draft by mutableStateOf(CommissionDraft())
    var requests by mutableStateOf(
        listOf(
            CommissionRequest(
                id = "231231231",
                draft = CommissionDraft(description = "Design a pixel-art character for the LED panel."),
                completedSteps = 6,
                messages = listOf(
                    CommissionMessage("Hello! Your request has been approved. Please attach your payment receipt."),
                    CommissionMessage(outgoing = true, sampleImage = true),
                    CommissionMessage("Payment received! We will get back to you as soon as possible.")
                ),
                notification = "Request is in progress. We will share work-in-progress updates."
            ),
            CommissionRequest(
                id = "Wataa2312",
                draft = CommissionDraft(description = "Create a looping animation for the LED panel."),
                completedSteps = 3,
                canceled = true,
                messages = listOf(CommissionMessage("This request has been canceled.")),
                notification = "The commission request was canceled."
            )
        )
    )
        private set

    fun submit(): String? {
        val amountVnd = draft.priceVnd ?: return null
        if (!draft.isValid) return null
        val id = UUID.randomUUID().toString().substringBefore('-').uppercase()
        val payment = MockSePayPayment(transferCode = "CM$id", amountVnd = amountVnd)
        requests = listOf(CommissionRequest(id, draft.copy(description = draft.description.trim()), payment = payment)) + requests
        draft = CommissionDraft()
        return id
    }

    fun simulateSePayReceipt(requestId: String) {
        requests = requests.map { request ->
            val payment = request.payment
            if (request.id != requestId || request.canceled || payment == null || payment.isPaid) {
                request
            } else {
                val receipt = "Demo SePay payment received for ${payment.transferCode}."
                request.copy(
                    payment = payment.copy(transactionId = "MOCK-$requestId"),
                    notification = receipt,
                    messages = request.messages + CommissionMessage(receipt)
                )
            }
        }
    }

    fun send(requestId: String, message: CommissionMessage) {
        if (message.text.isBlank() && message.attachment == null) return
        requests = requests.map { request ->
            if (request.id == requestId && !request.canceled && request.payment?.isPaid != false) {
                request.copy(messages = request.messages + message)
            } else request
        }
    }
}
