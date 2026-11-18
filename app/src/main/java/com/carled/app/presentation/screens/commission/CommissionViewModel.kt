package com.carled.app.presentation.screens.commission

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.core.di.ServiceLocator
import com.carled.app.domain.model.CommissionPriceCalculator
import com.carled.app.domain.model.CommissionPricingResponse
import com.carled.app.domain.repository.CommissionPricingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Replaces the earlier hardcoded CommissionType/CommissionSize mock: the create-
 * request form is now driven entirely by GET /api/commissions/pricing (AC01/AC02),
 * so pricing, revision quotas and the option catalog can change server-side
 * without an app update.
 *
 * See CommissionPriceCalculator for how a CUSTOM selection turns into a live
 * price, and CommissionPricingForm.kt for the Composables that render this.
 *
 * WIRING NEEDED: add a `commissionPricingRepository: CommissionPricingRepository`
 * property to ServiceLocator (same pattern as its other repositories/use-cases),
 * backed by a Retrofit instance of CommissionPricingApi — e.g.
 *   val commissionPricingRepository: CommissionPricingRepository by lazy {
 *       CommissionPricingRepositoryImpl(retrofit.create(CommissionPricingApi::class.java))
 *   }
 */

internal data class MockSePayPayment(
    val transferCode: String,
    val amountVnd: Long,
    val transactionId: String? = null
) {
    val isPaid: Boolean get() = transactionId != null
}

internal data class CommissionDraft(
    val selectedTierCode: String? = null,
    val selections: Map<String, CommissionPriceCalculator.RuleSelection> = emptyMap(),
    val description: String = "",
    val references: List<Uri> = emptyList()
) {
    val isBriefValid: Boolean get() = description.isNotBlank()
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

internal class CommissionViewModel(
    private val repository: CommissionPricingRepository = ServiceLocator.commissionPricingRepository
) : ViewModel() {

    var draft by mutableStateOf(CommissionDraft())

    // Request list / inbox / mock payment flow — unrelated to this user story,
    // kept as-is apart from CommissionDraft's new shape.
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

    private val _pricing = MutableStateFlow<Resource<CommissionPricingResponse>>(Resource.Loading)
    val pricing: StateFlow<Resource<CommissionPricingResponse>> = _pricing.asStateFlow()

    init { loadPricing() }

    fun loadPricing() {
        viewModelScope.launch {
            _pricing.value = Resource.Loading
            _pricing.value = repository.getPricing()
        }
    }

    /** Called when the user taps a tier card — a fixed preset, or the CUSTOM tier. */
    fun selectTier(tierCode: String) {
        draft = draft.copy(selectedTierCode = tierCode)
    }

    fun selectSingle(ruleKey: String, optionCode: String) {
        draft = draft.copy(
            selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(selectedOptionCodes = setOf(optionCode)))
        )
    }

    fun toggleMulti(ruleKey: String, optionCode: String) {
        val current = draft.selections[ruleKey]?.selectedOptionCodes.orEmpty()
        val updated = if (optionCode in current) current - optionCode else current + optionCode
        draft = draft.copy(
            selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(selectedOptionCodes = updated))
        )
    }

    fun setQuantity(ruleKey: String, quantity: Int) {
        draft = draft.copy(selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(quantity = quantity)))
    }

    fun setText(ruleKey: String, text: String) {
        draft = draft.copy(selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(text = text)))
    }

    fun updateDescription(text: String) {
        draft = draft.copy(description = text)
    }

    fun addReference(uri: Uri, max: Int) {
        draft = draft.copy(references = (draft.references + uri).distinct().take(max))
    }

    fun removeReference(uri: Uri) {
        draft = draft.copy(references = draft.references - uri)
    }

    fun submit(): String? {
        val catalog = (pricing.value as? Resource.Success)?.data ?: return null
        val tier = catalog.tiers.firstOrNull { it.tierCode == draft.selectedTierCode } ?: return null
        if (!draft.isBriefValid) return null
        val amountVnd = if (tier.isCustom) {
            val result = CommissionPriceCalculator.calculate(catalog.rules, draft.selections)
            if (result.missingRequiredRuleKeys.isNotEmpty()) return null
            result.total
        } else {
            tier.basePrice
        }
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