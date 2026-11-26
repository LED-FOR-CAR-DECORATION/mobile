package com.carled.app.presentation.screens.commission

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.core.di.ServiceLocator
import com.carled.app.data.remote.CommissionChatSocket
import com.carled.app.domain.model.PaymentSessionResponse
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
import com.carled.app.domain.repository.CommissionPricingRepository
import com.carled.app.domain.repository.CommissionRepository
import com.carled.app.domain.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
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
        private set

    private val _pricing = MutableStateFlow<Resource<CommissionPricingResponse>>(Resource.Loading)
    val pricing = _pricing.asStateFlow()

    private val _submission = MutableStateFlow<Resource<CommissionResponse>?>(null)
    val submission = _submission.asStateFlow()

    private val _commissions = MutableStateFlow<Resource<List<CommissionSummaryResponse>>>(Resource.Loading)
    val commissions = _commissions.asStateFlow()

    private val _detail = MutableStateFlow(emptyMap<Long, Resource<CommissionResponse>>())
    val detail = _detail.asStateFlow()

    private val _messages = MutableStateFlow(emptyMap<Long, Resource<List<CommissionMessageResponse>>>())
    val messages = _messages.asStateFlow()

    private val _payment = MutableStateFlow<Resource<PaymentSessionResponse>?>(null)
    val payment = _payment.asStateFlow()

    private val _actionResult = MutableStateFlow<Resource<Unit>?>(null)
    val actionResult = _actionResult.asStateFlow()

    // Set when confirming a revision card comes back with a new payment session
    // (an extra-revision fee is owed). The screen observes this to open the
    // payment dialog immediately instead of waiting for the user to notice the
    // "pay now" button that reappears once activePaymentCode is reloaded.
    private val _pendingPaymentCode = MutableStateFlow<String?>(null)
    val pendingPaymentCode = _pendingPaymentCode.asStateFlow()

    fun consumePendingPaymentCode() {
        _pendingPaymentCode.value = null
    }

    private var chatJob: Job? = null

    init {
        loadPricing()
        loadCommissions()
    }

    fun loadPricing() {
        viewModelScope.launch {
            _pricing.value = Resource.Loading
            val result = pricingRepository.getPricing()
            _pricing.value = result
            if (result is Resource.Success) applyDefaultSelections(result.data.rules)
        }
    }

    /**
     * Seeds [draft] with each rule's isDefault option(s) so a fresh form already reflects
     * the backend's defaults — without this, PricingOption.isDefault was never read anywhere
     * and selections started completely empty, leaving any tier whose triggerOptionIds
     * pointed at a "default" option permanently unselectable until the customer happened to
     * click that exact option themselves. Never overwrites a rule the customer already touched.
     */
    private fun applyDefaultSelections(rules: List<PricingRule>) {
        val defaults = rules.mapNotNull { rule ->
            if (rule.ruleKey in draft.selections) return@mapNotNull null
            when (rule.selectionType) {
                SelectionType.SINGLE_SELECT -> rule.options.firstOrNull { it.isDefault }?.let { option ->
                    rule.ruleKey to CommissionPriceCalculator.RuleSelection(selectedOptionCodes = setOf(option.optionCode))
                }
                SelectionType.MULTI_SELECT -> {
                    val codes = rule.options.filter { it.isDefault }.map { it.optionCode }.toSet()
                    if (codes.isEmpty()) null else rule.ruleKey to CommissionPriceCalculator.RuleSelection(selectedOptionCodes = codes)
                }
                SelectionType.QUANTITY, SelectionType.TEXT_INPUT -> null
            }
        }.toMap()
        if (defaults.isNotEmpty()) {
            draft = draft.copy(selections = defaults + draft.selections)
        }
    }

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
        draft = draft.copy(
            selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(quantity = quantity))
        )
    }

    fun setText(ruleKey: String, text: String) {
        draft = draft.copy(
            selections = draft.selections + (ruleKey to CommissionPriceCalculator.RuleSelection(text = text))
        )
    }

    fun updateTitle(text: String) {
        draft = draft.copy(title = text)
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

    private fun buildSelectedOptionsPayload(rules: List<PricingRule>): List<SelectedOptionInput> {
        // Only rules whose prerequisite chain is currently satisfied may be submitted —
        // a selection left over from a rule that became hidden after the user changed
        // its parent option must be dropped, or the server rejects the whole request
        // with INVALID_COMMISSION_RULE_SELECTION.
        val visibleRuleKeys = CommissionPriceCalculator
            .visibleRulesInOrder(rules, draft.selections)
            .map { it.ruleKey }
            .toSet()

        return draft.selections.mapNotNull { (ruleKey, selection) ->
            if (ruleKey !in visibleRuleKeys) return@mapNotNull null
            val rule = rules.firstOrNull { it.ruleKey == ruleKey } ?: return@mapNotNull null
            when (rule.selectionType) {
                SelectionType.SINGLE_SELECT, SelectionType.MULTI_SELECT -> {
                    val ids = rule.options.filter { it.optionCode in selection.selectedOptionCodes }.map { it.optionId }
                    if (ids.isEmpty()) null else SelectedOptionInput(configId = rule.configId, optionIds = ids)
                }
                SelectionType.QUANTITY -> selection.quantity?.takeIf { it > 0 }
                    ?.let { SelectedOptionInput(configId = rule.configId, quantity = it) }
                SelectionType.TEXT_INPUT -> selection.text?.takeIf { it.isNotBlank() }
                    ?.let { SelectedOptionInput(configId = rule.configId, textValue = it) }
            }
        }
    }

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
                openPaymentImmediately = false
            )

            val result = repository.submit(request)
            _submission.value = result
            if (result is Resource.Success) {
                draft = CommissionDraft()
                applyDefaultSelections(catalog.rules)
                loadCommissions()
            }
        }
    }

    fun consumeSubmission() {
        _submission.value = null
    }

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

    fun refreshDetail(id: Long) {
        viewModelScope.launch {
            _detail.update { it + (id to repository.getCommissionDetail(id)) }
        }
    }

    fun refreshMessages(id: Long) {
        viewModelScope.launch {
            _messages.update { it + (id to repository.getMessages(id)) }
        }
    }

    fun loadMessages(id: Long) {
        viewModelScope.launch {
            _messages.update { it + (id to Resource.Loading) }
            _messages.update { it + (id to repository.getMessages(id)) }
        }
    }

    fun connectChat(id: Long) {
        val token = ServiceLocator.userPreferences.getAuthToken() ?: return
        chatSocket.connect(id, token)
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            chatSocket.incoming.collect { incoming ->
                if (incoming.commissionId == id) appendMessage(id, incoming)
            }
        }
    }

    fun disconnectChat() {
        chatSocket.disconnect()
        chatJob?.cancel()
        chatJob = null
    }

    fun sendMessage(id: Long, text: String, imageUri: Uri? = null) {
        if (text.isBlank() && imageUri == null) return
        viewModelScope.launch {
            var mediaUrl: String? = null
            if (imageUri != null) {
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

    private fun appendMessage(id: Long, message: CommissionMessageResponse?) {
        val msg = message ?: return
        _messages.update { current ->
            val existing = (current[id] as? Resource.Success)?.data.orEmpty()
            if (existing.any { it.messageId == msg.messageId }) current
            else current + (id to Resource.Success(existing + msg))
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatSocket.disconnect()
        chatJob?.cancel()
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
                    loadMessages(id)
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
                    result.data.paymentSession?.paymentCode?.let { _pendingPaymentCode.value = it }
                    loadMessages(id)
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
            when (val result = repository.cancelRevisionCard(id)) {
                is Resource.Success -> {
                    _actionResult.value = Resource.Success(Unit)
                    loadMessages(id)
                    loadDetail(id)
                    loadCommissions()
                }
                is Resource.Error -> _actionResult.value = Resource.Error(result.message)
                Resource.Loading -> Unit
            }
        }
    }

    fun consumeActionResult() {
        _actionResult.value = null
    }
}