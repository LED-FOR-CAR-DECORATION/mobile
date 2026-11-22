package com.carled.app.presentation.screens.commission

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.core.di.ServiceLocator
import com.carled.app.domain.model.*
import com.carled.app.domain.repository.CommissionPricingRepository
import com.carled.app.domain.repository.CommissionRepository
import com.carled.app.domain.repository.MediaRepository
import com.carled.app.data.remote.CommissionChatSocket
import com.carled.app.domain.model.commission.CommissionAttachmentInput
import com.carled.app.domain.model.commission.CommissionMessageResponse
import com.carled.app.domain.model.commission.CommissionPriceCalculator
import com.carled.app.domain.model.commission.CommissionPricingResponse
import com.carled.app.domain.model.commission.CommissionResponse
import com.carled.app.domain.model.commission.CommissionSummaryResponse
import com.carled.app.domain.model.commission.CreateCommissionMessageRequest
import com.carled.app.domain.model.commission.CreateCommissionRequest
import com.carled.app.domain.model.commission.PricingRule
import com.carled.app.domain.model.commission.SelectedOptionInput
import com.carled.app.domain.model.commission.SelectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CommissionDraft(
    val selectedTierCode: String? = null,
    val selections: Map<String, CommissionPriceCalculator.RuleSelection> = emptyMap(),
    val title: String = "",
    val description: String = "",
    val references: List<Uri> = emptyList()
) {
    val isBriefValid: Boolean get() = title.isNotBlank() && description.isNotBlank()
}

internal class CommissionViewModel(
    private val pricingRepository: CommissionPricingRepository = ServiceLocator.commissionPricingRepository,
    private val repository: CommissionRepository = ServiceLocator.commissionRepository,
    private val mediaRepository: MediaRepository = ServiceLocator.mediaRepository,
    private val chatSocket: CommissionChatSocket = ServiceLocator.commissionChatSocket
) : ViewModel() {

    var draft by mutableStateOf(CommissionDraft())

    private val _pricing = MutableStateFlow<Resource<CommissionPricingResponse>>(Resource.Loading)
    val pricing: StateFlow<Resource<CommissionPricingResponse>> = _pricing.asStateFlow()

    private val _submission = MutableStateFlow<Resource<CommissionResponse>?>(null)
    val submission: StateFlow<Resource<CommissionResponse>?> = _submission.asStateFlow()

    private val _commissions = MutableStateFlow<Resource<List<CommissionSummaryResponse>>>(Resource.Loading)
    val commissions: StateFlow<Resource<List<CommissionSummaryResponse>>> = _commissions.asStateFlow()

    private val _detail = MutableStateFlow<Map<Long, Resource<CommissionResponse>>>(emptyMap())
    val detail: StateFlow<Map<Long, Resource<CommissionResponse>>> = _detail.asStateFlow()

    private val _messages = MutableStateFlow<Map<Long, Resource<List<CommissionMessageResponse>>>>(emptyMap())
    val messages: StateFlow<Map<Long, Resource<List<CommissionMessageResponse>>>> = _messages.asStateFlow()

    private val _payment = MutableStateFlow<Resource<PaymentSessionResponse>?>(null)
    val payment: StateFlow<Resource<PaymentSessionResponse>?> = _payment.asStateFlow()

    private val _actionResult = MutableStateFlow<Resource<Unit>?>(null)
    val actionResult: StateFlow<Resource<Unit>?> = _actionResult.asStateFlow()
    init {
        loadPricing()
        loadCommissions()
    }

    // -------------------------------------------------------------------------
    // Pricing / draft
    // -------------------------------------------------------------------------

    fun loadPricing() {
        viewModelScope.launch {
            _pricing.value = Resource.Loading
            _pricing.value = pricingRepository.getPricing()
        }
    }

    fun selectTier(tierCode: String) { draft = draft.copy(selectedTierCode = tierCode) }

    fun selectSingle(ruleKey: String, optionCode: String) {
        draft = draft.copy(selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(selectedOptionCodes = setOf(optionCode))))
    }

    fun toggleMulti(ruleKey: String, optionCode: String) {
        val current = draft.selections[ruleKey]?.selectedOptionCodes.orEmpty()
        val updated = if (optionCode in current) current - optionCode else current + optionCode
        draft = draft.copy(selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(selectedOptionCodes = updated)))
    }

    fun setQuantity(ruleKey: String, quantity: Int) {
        draft = draft.copy(selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(quantity = quantity)))
    }

    fun setText(ruleKey: String, text: String) {
        draft = draft.copy(selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(text = text)))
    }

    fun updateTitle(text: String) { draft = draft.copy(title = text) }
    fun updateDescription(text: String) { draft = draft.copy(description = text) }

    fun addReference(uri: Uri, max: Int) {
        draft = draft.copy(references = (draft.references + uri).distinct().take(max))
    }

    fun removeReference(uri: Uri) {
        draft = draft.copy(references = draft.references - uri)
    }

    private fun buildSelectedOptionsPayload(rules: List<PricingRule>): List<SelectedOptionInput> =
        draft.selections.mapNotNull { (ruleKey, selection) ->
            val rule = rules.firstOrNull { it.ruleKey == ruleKey } ?: return@mapNotNull null
            when (rule.selectionType) {
                SelectionType.SINGLE_SELECT, SelectionType.MULTI_SELECT -> {
                    val ids = rule.options.filter { it.optionCode in selection.selectedOptionCodes }.map { it.optionId }
                    if (ids.isEmpty()) null else SelectedOptionInput(
                        configId = rule.configId,
                        optionIds = ids
                    )
                }
                SelectionType.QUANTITY -> selection.quantity?.takeIf { it > 0 }
                    ?.let { SelectedOptionInput(configId = rule.configId, quantity = it) }
                SelectionType.TEXT_INPUT -> selection.text?.takeIf { it.isNotBlank() }
                    ?.let { SelectedOptionInput(configId = rule.configId, textValue = it) }
            }
        }

    // -------------------------------------------------------------------------
    // Submit — uploads reference images first (MED-01), then creates the
    // commission, then refreshes the real list from the server.
    // -------------------------------------------------------------------------

    fun submit() {
        val catalog = (pricing.value as? Resource.Success)?.data ?: return
        val tier = catalog.tiers.firstOrNull { it.tierCode == draft.selectedTierCode } ?: return
        if (!draft.isBriefValid) return
        if (tier.isCustom) {
            val result = CommissionPriceCalculator.calculate(catalog.rules, draft.selections)
            if (result.missingRequiredRuleKeys.isNotEmpty()) return
        }

        viewModelScope.launch {
            _submission.value = Resource.Loading

            val attachmentUrls = mutableListOf<String>()
            for (uri in draft.references) {
                when (val uploaded = mediaRepository.uploadImage(uri, purpose = "COMMISSION_REFERENCE")) {
                    is Resource.Success -> attachmentUrls += uploaded.data
                    is Resource.Error -> {
                        _submission.value = Resource.Error("Tải ảnh tham khảo thất bại: ${uploaded.message}")
                        return@launch
                    }
                    Resource.Loading -> Unit
                }
            }

            val request = CreateCommissionRequest(
                title = draft.title.trim(),
                description = draft.description.trim(),
                tierCode = tier.tierCode,
                selectedOptions = buildSelectedOptionsPayload(catalog.rules),
                attachments = attachmentUrls.map { CommissionAttachmentInput(imageUrl = it) },
                openPaymentImmediately = true
            )

            val result = repository.submit(request)
            _submission.value = result
            if (result is Resource.Success) {
                draft = CommissionDraft()
                loadCommissions()
            }
        }
    }

    fun consumeSubmission() { _submission.value = null }

    // -------------------------------------------------------------------------
    // Commission list / detail — GET /api/commissions/my, GET /api/commissions/{id}
    // -------------------------------------------------------------------------

    fun loadCommissions(status: String? = null) {
        viewModelScope.launch {
            _commissions.value = Resource.Loading
            _commissions.value = repository.getMyCommissions(status = status)
        }
    }

    fun loadDetail(id: Long) {
        viewModelScope.launch {
            _detail.update { it + (id to Resource.Loading) }
            _detail.update { it + (id to repository.getCommissionDetail(id)) }
        }
    }

    // -------------------------------------------------------------------------
    // Chat — REST history (GET/POST .../messages) + live updates over STOMP.
    // Our own sent message is appended from the REST response immediately;
    // if the server also echoes it back over the topic, dedup by messageId
    // in both appenders prevents it showing twice.
    // -------------------------------------------------------------------------

    fun loadMessages(id: Long) {
        viewModelScope.launch {
            _messages.update { it + (id to Resource.Loading) }
            _messages.update { it + (id to repository.getMessages(id)) }
        }
    }

    /** Call when a commission's chat screen becomes visible. Pair with [disconnectChat] in onDispose. */
    fun connectChat(id: Long) {
        val token = ServiceLocator.userPreferences.getAuthToken() ?: return
        chatSocket.connect(id, token)
        viewModelScope.launch {
            chatSocket.incoming.collect { incoming ->
                if (incoming.commissionId != id) return@collect
                appendMessage(id, incoming)
            }
        }
    }

    fun disconnectChat() {
        chatSocket.disconnect()
    }

    fun sendMessage(id: Long, text: String, imageUri: Uri? = null) {
        if (text.isBlank() && imageUri == null) return
        viewModelScope.launch {
            var mediaUrl: String? = null
            if (imageUri != null) {
                // COMMISSION_CHAT requires targetId = commissionId per AC01.
                when (val uploaded = mediaRepository.uploadImage(imageUri, purpose = "COMMISSION_CHAT", targetId = id)) {
                    is Resource.Success -> mediaUrl = uploaded.data
                    is Resource.Error -> return@launch
                    Resource.Loading -> Unit
                }
            }
            val request = CreateCommissionMessageRequest(
                messageType = if (mediaUrl != null) "IMAGE" else "TEXT",
                content = text.trim().ifBlank { null },
                mediaUrl = mediaUrl
            )
            val result = repository.sendMessage(id, request)
            if (result is Resource.Success) appendMessage(id, result.data)
        }
    }

    private fun appendMessage(id: Long, message: CommissionMessageResponse) {
        _messages.update { current ->
            val existing = (current[id] as? Resource.Success)?.data.orEmpty()
            if (existing.any { it.messageId == message.messageId }) current
            else current + (id to Resource.Success(existing + message))
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatSocket.disconnect()
    }

    fun loadPayment(code: String) {
        viewModelScope.launch {
            _payment.value = Resource.Loading
            _payment.value = repository.getPaymentSession(code)
        }
    }

    fun mockPaySuccess(code: String) {
        viewModelScope.launch {
            repository.mockPaymentSuccess(code)
            loadPayment(code)
            loadCommissions()
        }
    }
    fun approveDeliverable(id: Long) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading
            when (val result = repository.approveDeliverable(id)) {
                is Resource.Success -> {
                    _actionResult.value = Resource.Success(Unit)
                    appendMessage(id, result.data.systemMessage)
                    loadDetail(id)
                    loadCommissions()
                }
                is Resource.Error -> _actionResult.value = Resource.Error(result.message)
                Resource.Loading -> Unit
            }
        }
    }

    fun confirmRevisionCard(id: Long, messageId: Long) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading
            when (val result = repository.confirmRevisionCard(id, messageId)) {
                is Resource.Success -> {
                    _actionResult.value = Resource.Success(Unit)
                    loadDetail(id)
                    loadCommissions()
                }
                is Resource.Error -> _actionResult.value = Resource.Error(result.message)
                Resource.Loading -> Unit
            }
        }
    }

    fun cancelRevisionCard(id: Long) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading
            _actionResult.value = repository.cancelRevisionCard(id)
            loadDetail(id)
            loadCommissions()
        }
    }

    fun consumeActionResult() { _actionResult.value = null }
}