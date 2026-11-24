package com.carled.app.presentation.screens.commission

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carled.app.core.common.Resource
import com.carled.app.domain.model.*
import com.carled.app.domain.model.commission.CommissionMessageResponse
import com.carled.app.domain.model.commission.CommissionPriceCalculator
import com.carled.app.domain.model.commission.CommissionResponse
import com.carled.app.domain.model.commission.CommissionSummaryResponse
import com.carled.app.domain.model.commission.isChatActive
import com.carled.app.presentation.components.MirageButton
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale
import com.carled.app.core.di.ServiceLocator
import okhttp3.Request

private const val MAX_REFERENCES = 5
private val CardShape = RoundedCornerShape(3.dp)
private fun formatVnd(amount: Double) =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " VND"
private val ImagePickerRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

private enum class CommissionTab(val label: String) {
    CREATE("Create request"), PROGRESS("Progress tracking"), INBOX("Inbox")
}
private fun extractJsonField(json: String?, field: String): String? {
    if (json.isNullOrBlank()) return null
    return try { org.json.JSONObject(json).optString(field).takeIf { it.isNotBlank() } } catch (e: Exception) { null }
}

@Composable
internal fun CommissionScreen(user: User, modifier: Modifier = Modifier) {
    val model: CommissionViewModel = viewModel(key = "commission:${user.id}")
    var tab by rememberSaveable(user.id) { mutableStateOf(CommissionTab.CREATE) }
    var selectedId by rememberSaveable(user.id) { mutableStateOf<Long?>(null) }
    var conversationId by rememberSaveable(user.id) { mutableStateOf<Long?>(null) }
    var paymentCode by rememberSaveable(user.id) { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            CommissionTab.entries.forEach { item ->
                val selected = tab == item
                Box(
                    Modifier.weight(1f)
                        .background(if (selected) GreenLight else Cream)
                        .border(0.5.dp, Ink)
                        .selectable(selected, role = Role.Tab) {
                            tab = item
                            if (item == CommissionTab.PROGRESS || item == CommissionTab.INBOX) model.loadCommissions()
                        }
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.label, style = MaterialTheme.typography.labelMedium, color = Ink)
                }
            }
        }
        key(tab) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    CommissionTab.CREATE -> CommissionForm(user, model) { id, code ->
                        tab = CommissionTab.PROGRESS
                        selectedId = id
                        model.loadDetail(id)
                        if (code != null) paymentCode = code
                    }
                    CommissionTab.PROGRESS -> {
                        val commissionsState by model.commissions.collectAsState()
                        CommissionPanel("Progress tracking") {
                            when (val state = commissionsState) {
                                is Resource.Loading -> { CircularProgressIndicator(color = Ink); Text("Đang tải…", color = Ink) }
                                is Resource.Error -> {
                                    Text(state.message, color = Ink)
                                    MirageButton("Thử lại", { model.loadCommissions() })
                                }
                                is Resource.Success -> {
                                    if (state.data.isEmpty()) Text("Chưa có yêu cầu commission nào.", color = Ink)
                                    state.data.forEach { item ->
                                        RequestCard(item) {
                                            selectedId = item.commissionId
                                            model.loadDetail(item.commissionId)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    CommissionTab.INBOX -> {
                        val commissionsState by model.commissions.collectAsState()
                        CommissionPanel("Incoming messages") {
                            when (val state = commissionsState) {
                                is Resource.Loading -> CircularProgressIndicator(color = Ink)
                                is Resource.Error -> {
                                    Text(state.message, color = Ink)
                                    MirageButton("Thử lại", { model.loadCommissions() })
                                }
                                is Resource.Success -> {
                                    val chatty = state.data.filter { it.isChatActive }
                                    if (chatty.isEmpty()) Text("Chưa có cuộc trò chuyện nào.", color = Ink)
                                    chatty.forEach { item ->
                                        RequestCard(item) { conversationId = item.commissionId }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    paymentCode?.let { code ->
        PaymentDialog(code, model, onClose = { paymentCode = null })
    }
    if (paymentCode == null) {
        conversationId?.let { id ->
            CommissionConversation(id, model, onClose = { conversationId = null })
        }
    }
    if (conversationId == null && paymentCode == null) {
        selectedId?.let { id ->
            val detailState by model.detail.collectAsState()
            when (val state = detailState[id]) {
                is Resource.Success -> CommissionTimeline(
                    state.data,
                    model,
                    onClose = { selectedId = null },
                    onOpenChat = { selectedId = null; conversationId = id },
                    onOpenPayment = { code -> paymentCode = code }
                )
                is Resource.Error -> Dialog(onDismissRequest = { selectedId = null }) {
                    WindowFrame("Lỗi", Modifier.fillMaxWidth(), onClose = { selectedId = null }, compact = true) {
                        Text(state.message, color = Ink)
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun CommissionForm(user: User, model: CommissionViewModel, onSubmit: (Long, String?) -> Unit) {
    val draft = model.draft
    val pricingState by model.pricing.collectAsState()
    val submissionState by model.submission.collectAsState()
    var editingBrief by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = editingBrief) { editingBrief = false }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) model.addReference(uri, MAX_REFERENCES)
    }

    when (val state = pricingState) {
        is Resource.Loading -> CommissionPanel("Choose design and price") {
            CircularProgressIndicator(color = Ink)
            Text("Đang tải bảng giá…", color = Ink)
        }
        is Resource.Error -> CommissionPanel("Choose design and price") {
            Text(state.message, color = Ink)
            MirageButton("Thử lại", model::loadPricing)
        }
        is Resource.Success -> {
            val catalog = state.data
            val selectedTier = catalog.tiers.firstOrNull { it.tierCode == draft.selectedTierCode }
            val visibleRules = remember(catalog.rules, draft.selections) {
                CommissionPriceCalculator.visibleRulesInOrder(catalog.rules, draft.selections)
            }
            val priceResult = remember(catalog.rules, draft.selections) {
                CommissionPriceCalculator.calculate(catalog.rules, draft.selections)
            }
            val selectedOptionIds = remember(catalog.rules, draft.selections) {
                catalog.rules.flatMap { rule ->
                    rule.options.filter { it.optionCode in draft.selections[rule.ruleKey]?.selectedOptionCodes.orEmpty() }
                }.map { it.optionId }.toSet()
            }

            if (!editingBrief) {
                CommissionPanel("Choose design and price") {
                    Text("Configure your request", color = Ink, style = MaterialTheme.typography.labelLarge)
                    visibleRules.forEach { rule ->
                        PricingRuleRow(
                            rule = rule,
                            selection = draft.selections[rule.ruleKey],
                            onSelectSingle = { code -> model.selectSingle(rule.ruleKey, code) },
                            onToggleMulti = { code -> model.toggleMulti(rule.ruleKey, code) },
                            onQuantityChange = { qty -> model.setQuantity(rule.ruleKey, qty) },
                            onTextChange = { text -> model.setText(rule.ruleKey, text) }
                        )
                    }
                    Text("Choose a package", color = Ink, style = MaterialTheme.typography.labelLarge)
                    catalog.tiers.sortedBy { it.displayOrder }.forEach { tier ->
                        val eligible = tier.triggerOptionIds.isEmpty() || tier.triggerOptionIds.any { it in selectedOptionIds }
                        TierCard(
                            tier = tier,
                            selected = draft.selectedTierCode == tier.tierCode,
                            liveCustomTotal = if (tier.isCustom) priceResult.total else null,
                            eligible = eligible,
                            onSelect = { model.selectTier(tier.tierCode) }
                        )
                    }
                    val canContinue = selectedTier != null &&
                            (!selectedTier.isCustom || priceResult.missingRequiredRuleKeys.isEmpty())
                    MirageButton("Continue to brief", { editingBrief = true }, Modifier.fillMaxWidth(), enabled = canContinue)
                }
            } else {
                val priceVnd = if (selectedTier?.isCustom == true) priceResult.total else selectedTier?.basePrice
                CommissionPanel("Design brief") {
                    Text(
                        "${selectedTier?.tierName.orEmpty()} · ${priceVnd?.let { formatVnd(it.toDouble()) }.orEmpty()}",
                        color = Ink
                    )
                    TextButton(onClick = { editingBrief = false }) { Text("Change design", color = Ink) }
                    CommissionField(user.fullName?.takeIf { it.isNotBlank() } ?: user.id, {}, "Username (user ID)", readOnly = true)
                    CommissionField(draft.title, model::updateTitle, "Title")
                    CommissionField(draft.description, model::updateDescription, "Description", multiline = true)
                    Text("Reference images (optional, up to $MAX_REFERENCES)", style = MaterialTheme.typography.labelLarge, color = Ink)
                    (draft.references.map<Uri, Uri?> { it } +
                            List(minOf(3, MAX_REFERENCES - draft.references.size)) { null }).chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { uri ->
                                val slot = Modifier.weight(1f).height(72.dp).background(Cream).border(1.dp, Ink)
                                if (uri == null) {
                                    Box(
                                        slot.clickable(role = Role.Button) { picker.launch(ImagePickerRequest) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.NoteAdd, "Add reference image", tint = Ink, modifier = Modifier.size(34.dp))
                                    }
                                } else {
                                    Box(slot) {
                                        LocalImagePreview(uri, "Reference image", Modifier.fillMaxSize())
                                        TextButton(
                                            onClick = { model.removeReference(uri) },
                                            modifier = Modifier.align(Alignment.TopEnd).size(44.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) { Text("×", color = Ink, modifier = Modifier.background(Cream)) }
                                    }
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    MirageButton(
                        when (submissionState) {
                            is Resource.Loading -> "Đang gửi…"
                            else -> "Submit request"
                        },
                        model::submit,
                        Modifier.fillMaxWidth(),
                        enabled = draft.isBriefValid && submissionState !is Resource.Loading,
                        secondary = true
                    )
                    (submissionState as? Resource.Error)?.let { err ->
                        Text(err.message, color = MaterialTheme.colorScheme.error)
                    }
                    LaunchedEffect(submissionState) {
                        val success = submissionState as? Resource.Success ?: return@LaunchedEffect
                        editingBrief = false
                        onSubmit(success.data.commissionId, success.data.activePaymentCode)
                        model.consumeSubmission()
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommissionField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    numeric: Boolean = false,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, color = Ink) },
        readOnly = readOnly,
        singleLine = !multiline,
        minLines = if (multiline) 4 else 1,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        shape = CardShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Cream, unfocusedContainerColor = Cream,
            focusedTextColor = Ink, unfocusedTextColor = Ink,
            focusedBorderColor = Ink, unfocusedBorderColor = Ink
        )
    )
}

@Composable
private fun CommissionPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Green).border(2.dp, Ink).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(title)
        content()
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier, color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun RequestCard(commission: CommissionSummaryResponse, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Cream).border(1.dp, Ink)
            .clickable(role = Role.Button, onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("#${commission.commissionId} · ${commission.title}", color = Ink, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatVnd(commission.totalPrice), Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.bodySmall)
            Text(commission.status, color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CommissionTimeline(
    commission: CommissionResponse,
    model: CommissionViewModel,
    onClose: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenPayment: (String) -> Unit
) {
    // Reference attachments and delivered previews are private signed URLs that
    // expire after ~15 min. When any of them 401/403, silently re-fetch the detail
    // to get fresh URLs — the existing content stays visible during the reload.
    val onImageExpired = remember(commission.commissionId) {
        { model.refreshDetail(commission.commissionId) }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WindowFrame(
            "Request #${commission.commissionId}",
            Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            onClose = onClose,
            compact = true
        ) {
            Text(commission.title, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(commission.status, color = Ink, fontWeight = FontWeight.Bold)
            commission.description?.let { Text(it, color = Ink) }

            Column(Modifier.fillMaxWidth().background(Cream).border(1.dp, Ink).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Gói: ${commission.tierCode}", color = Ink)
                Text("Giá ban đầu: ${formatVnd(commission.initialPrice)}", color = Ink)
                if (commission.extraRevisionFee > 0) {
                    Text("Phí sửa thêm: ${formatVnd(commission.extraRevisionFee)}", color = Ink)
                }
                Text("Tổng cộng: ${formatVnd(commission.totalPrice)}", color = Ink, fontWeight = FontWeight.Bold)
                Text("Đã sửa ${commission.revisionsUsed}/${commission.maxRevisions} lần", color = Ink)
            }

            commission.rejectionReason?.let { reason ->
                Text("Lý do từ chối: $reason", color = MaterialTheme.colorScheme.error)
            }

            commission.activePaymentCode?.let { code ->
                MirageButton("Thanh toán ngay · ${formatVnd(commission.totalPrice)}", { onOpenPayment(code) }, Modifier.fillMaxWidth())
            }

            if (commission.attachments.isNotEmpty()) {
                Text("Ảnh tham khảo", color = Ink, style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    commission.attachments.take(3).forEach { att ->
                        RemoteImagePreview(
                            att.imageUrl,
                            att.caption ?: "Reference",
                            Modifier.weight(1f).height(72.dp),
                            onExpired = onImageExpired
                        )
                    }
                }
            }

            if (commission.deliveredPreviews.isNotEmpty()) {
                Text("Bản xem trước đã giao", color = Ink, style = MaterialTheme.typography.labelLarge)
                commission.deliveredPreviews.forEach { preview ->
                    Column(Modifier.fillMaxWidth().background(Cream).border(1.dp, Ink).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RemoteImagePreview(
                            preview.imageUrl,
                            "Preview v${preview.versionNumber}",
                            Modifier.fillMaxWidth().height(140.dp),
                            onExpired = onImageExpired
                        )
                        Text("v${preview.versionNumber} · ${preview.status}", color = Ink, fontWeight = FontWeight.Bold)
                        preview.designerNote?.let { Text(it, color = Ink, style = MaterialTheme.typography.bodySmall) }
                        if (preview.status == "STAFF_APPROVED") {
                            MirageButton("Duyệt bản xem trước này", { model.approveDeliverable(commission.commissionId) }, Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (commission.chatActive) {
                MirageButton("Open commission chat", onOpenChat, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PaymentDialog(paymentCode: String, model: CommissionViewModel, onClose: () -> Unit) {
    val paymentState by model.payment.collectAsState()

    LaunchedEffect(paymentCode) { model.loadPayment(paymentCode) }
    // No timer-based polling. The only automatic network calls here are the
    // single initial fetch above and whatever the user explicitly triggers
    // with the "Check now" button below.

    Dialog(onDismissRequest = onClose) {
        WindowFrame("SePay · Thanh toán", Modifier.fillMaxWidth(), onClose = onClose, compact = true) {
            when (val state = paymentState) {
                is Resource.Loading, null -> CircularProgressIndicator(color = Ink)
                is Resource.Error -> {
                    Text(state.message, color = Ink)
                    MirageButton("Thử lại", { model.loadPayment(paymentCode) })
                }
                is Resource.Success -> {
                    val session = state.data
                    val now = remember { System.currentTimeMillis() }
                    val expiresAtMillis = remember(session.expiresAt) { parseIsoToMillis(session.expiresAt) }
                    val isExpired = expiresAtMillis != null && now >= expiresAtMillis

                    SectionTitle(
                        when {
                            session.status == "SUCCEEDED" -> "Đã thanh toán"
                            session.status == "EXPIRED" || isExpired -> "Đã hết hạn"
                            session.status == "CANCELLED" || session.status == "FAILED" -> "Thanh toán thất bại"
                            else -> "Đang chờ chuyển khoản"
                        }
                    )

                    if (session.status == "PENDING" && !isExpired) {
                        session.qrUrl?.let { url -> RemoteImagePreview(url, "Mã QR thanh toán", Modifier.size(220.dp)) }
                    }

                    Column(
                        Modifier.fillMaxWidth().background(GreenLight).border(1.dp, Ink).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Số tiền: ${formatVnd(session.amount)}", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Ngân hàng: ${session.bankName}", color = Ink)
                        Text("Số tài khoản: ${session.accountNumber}", color = Ink)
                        Text("Chủ tài khoản: ${session.accountName}", color = Ink)
                        Text("Nội dung chuyển khoản: ${session.paymentCode}", color = Ink)
                    }

                    when {
                        session.status == "PENDING" && isExpired -> {
                            Text(
                                "Phiên thanh toán này đã hết hạn. Vui lòng liên hệ hỗ trợ hoặc kiểm tra lại yêu cầu commission.",
                                color = MaterialTheme.colorScheme.error
                            )
                            MirageButton("Đóng", onClose, Modifier.fillMaxWidth())
                        }
                        session.status == "PENDING" -> {
                            MirageButton("Tôi đã chuyển khoản · Kiểm tra ngay", { model.loadPayment(paymentCode) }, Modifier.fillMaxWidth())
                            MirageButton("Mô phỏng thanh toán thành công (staging)", { model.mockPaySuccess(paymentCode) }, Modifier.fillMaxWidth(), secondary = true)
                        }
                        else -> MirageButton("Đóng", onClose, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

private fun parseIsoToMillis(iso: String?): Long? = try {
    iso?.let { java.time.Instant.parse(it).toEpochMilli() }
} catch (e: Exception) { null }

@Composable
private fun CommissionConversation(commissionId: Long, model: CommissionViewModel, onClose: () -> Unit) {
    var text by rememberSaveable(commissionId) { mutableStateOf("") }
    var attachment by rememberSaveable(commissionId) { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) attachment = uri
    }
    val scroll = rememberScrollState()
    val messagesState by model.messages.collectAsState()
    val messages = (messagesState[commissionId] as? Resource.Success)?.data.orEmpty()
    val actionResult by model.actionResult.collectAsState()
    val detailState by model.detail.collectAsState()
    val commissionStatus = (detailState[commissionId] as? Resource.Success)?.data?.status

    // Chat images (IMAGE messages, DELIVERABLE_CARD previews) are private signed
    // URLs. On 401/403 this lambda triggers a silent refreshMessages so Compose
    // re-renders with fresh URLs. The stable remember key means multiple expired
    // images in the same conversation only produce one lambda instance.
    val onImageExpired = remember(commissionId) { { model.refreshMessages(commissionId) } }

    DisposableEffect(commissionId) {
        model.loadMessages(commissionId)
        model.loadDetail(commissionId) // needed so action cards can gate on the live status, not just their own metadataJson
        model.connectChat(commissionId)
        onDispose { model.disconnectChat() }
    }
    LaunchedEffect(messages.size, scroll.maxValue) {
        if (scroll.maxValue != Int.MAX_VALUE) scroll.scrollTo(scroll.maxValue)
    }
    LaunchedEffect(actionResult) {
        val result = actionResult ?: return@LaunchedEffect
        if (result is Resource.Success) model.consumeActionResult()
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WindowFrame(
            "Inbox · Request #$commissionId",
            modifier = Modifier.fillMaxSize().imePadding().padding(8.dp),
            onClose = onClose,
            compact = true,
            scrollState = scroll,
            fillContent = true,
            footer = {
                attachment?.let { uri ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LocalImagePreview(uri, "Pending attachment", Modifier.size(52.dp))
                        TextButton(onClick = { attachment = null }) { Text("Remove attachment", color = Ink) }
                    }
                }
                (actionResult as? Resource.Error)?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
                Row(
                    Modifier.fillMaxWidth().background(Cream, CardShape).border(1.dp, Ink, CardShape),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { picker.launch(ImagePickerRequest) }) {
                        Icon(Icons.Default.AddPhotoAlternate, "Attach image", tint = Ink)
                    }
                    OutlinedTextField(
                        text, { text = it }, Modifier.weight(1f), placeholder = { Text("Message", color = Ink) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Ink, unfocusedTextColor = Ink)
                    )
                    IconButton(
                        enabled = text.isNotBlank() || attachment != null,
                        onClick = {
                            model.sendMessage(commissionId, text, attachment)
                            text = ""
                            attachment = null
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send message")
                    }
                }
            }
        ) {
            when (val state = messagesState[commissionId]) {
                is Resource.Loading -> CircularProgressIndicator(color = Ink)
                is Resource.Error -> Text(state.message, color = Ink)
                else -> messages.forEach { message ->
                    MessageBubble(commissionId, message, model, commissionStatus, onImageExpired)
                }
            }
        }
    }
}
@Composable
private fun MessageBubble(
    commissionId: Long,
    message: CommissionMessageResponse,
    model: CommissionViewModel,
    commissionStatus: String?,
    onImageExpired: () -> Unit
) {
    val outgoing = message.senderRole == "CUSTOMER"
    val actionable = commissionStatus == "PREVIEW_DELIVERED"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!outgoing) MessageAvatar(Green, Modifier.padding(end = 8.dp))
        Column(
            Modifier.widthIn(max = 224.dp).background(if (outgoing) Green else Cream, CardShape)
                .border(1.dp, Ink, CardShape).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (message.messageType) {
                "DELIVERABLE_CARD" -> {
                    Text("Bản xem trước đã sẵn sàng", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    message.content?.let { Text(it, color = Ink) }
                    message.mediaUrl?.let { url ->
                        RemoteImagePreview(url, "Deliverable preview", Modifier.size(158.dp, 90.dp), onExpired = onImageExpired)
                    }
                    if (actionable) {
                        MirageButton("Duyệt bản xem trước", { model.approveDeliverable(commissionId) }, Modifier.fillMaxWidth())
                    } else {
                        Text("Yêu cầu này không còn khả dụng.", color = Ink, style = MaterialTheme.typography.labelSmall)
                    }
                }
                "EXTRA_FEE_CARD" -> {
                    val cardStatus = extractJsonField(message.metadataJson, "status")
                    Text("Yêu cầu phụ phí sửa đổi", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    message.content?.let { Text(it, color = Ink) }
                    when {
                        cardStatus != null && cardStatus != "PENDING" ->
                            Text("Trạng thái: $cardStatus", color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        !actionable ->
                            Text("Yêu cầu này không còn khả dụng.", color = Ink, style = MaterialTheme.typography.labelSmall)
                        else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MirageButton("Xác nhận", { model.confirmRevisionCard(commissionId, message.messageId) }, Modifier.weight(1f))
                            MirageButton("Huỷ", { model.cancelRevisionCard(commissionId) }, Modifier.weight(1f), secondary = true)
                        }
                    }
                }
                "SYSTEM" -> Text(message.content.orEmpty(), color = Ink, style = MaterialTheme.typography.bodySmall)
                else -> {
                    message.content?.takeIf { it.isNotBlank() }?.let { Text(it, color = Ink) }
                    message.mediaUrl?.let { url ->
                        RemoteImagePreview(url, "Attached image", Modifier.size(158.dp, 90.dp), onExpired = onImageExpired)
                    }
                }
            }
        }
        if (outgoing) MessageAvatar(Cream, Modifier.padding(start = 8.dp))
    }
}
@Composable
private fun MessageAvatar(color: Color, modifier: Modifier) {
    Box(modifier.size(24.dp).background(color, CircleShape).border(1.dp, Ink, CircleShape))
}

@Composable
private fun LocalImagePreview(uri: Uri, label: String, modifier: Modifier) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val options = BitmapFactory.Options()
                while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize.coerceAtLeast(1) > 512) {
                    options.inSampleSize = options.inSampleSize.coerceAtLeast(1) * 2
                }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }
    }
    val preview = bitmap
    if (preview == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AddPhotoAlternate, label, tint = Ink)
        }
    } else {
        Image(preview, label, modifier.clip(CardShape), contentScale = ContentScale.Crop)
    }
}

private sealed class RemotePreviewState {
    object Loading : RemotePreviewState()
    data class Loaded(val bitmap: ImageBitmap) : RemotePreviewState()
    object Failed : RemotePreviewState()
    // Distinct from Failed so the UI can show "retrying…" instead of a permanent error.
    object Expired : RemotePreviewState()
}

/**
 * Fetches and renders a remote image. For private commission/warranty media the
 * backend issues signed URLs that expire after ~15 min. When the URL returns
 * HTTP 401 or 403 this composable moves to the [RemotePreviewState.Expired] state
 * and fires [onExpired] so the caller can re-fetch the parent record (commission
 * detail or messages) and get a fresh signed URL. All other failures (network
 * errors, 404, decode failures) land on [RemotePreviewState.Failed] and show the
 * broken-image placeholder without triggering a refetch.
 */
@Composable
private fun RemoteImagePreview(
    url: String?,
    label: String,
    modifier: Modifier,
    onExpired: (() -> Unit)? = null
) {
    val currentOnExpired by rememberUpdatedState(onExpired)
    val state by produceState<RemotePreviewState>(RemotePreviewState.Loading, url) {
        value = withContext(Dispatchers.IO) {
            if (url.isNullOrBlank()) {
                android.util.Log.w("RemoteImagePreview", "Skipped fetch: null/blank url for $label")
                return@withContext RemotePreviewState.Failed
            }
            try {
                val request = Request.Builder().url(url).build()
                ServiceLocator.plainHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        android.util.Log.e("RemoteImagePreview", "Fetch failed: HTTP ${response.code} for $url")
                        if (response.code == 401 || response.code == 403) {
                            currentOnExpired?.invoke()
                        }
                        return@use RemotePreviewState.Failed
                    }
                    val bytes = response.body?.bytes()
                    val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    if (bitmap != null) RemotePreviewState.Loaded(bitmap.asImageBitmap())
                    else {
                        android.util.Log.e("RemoteImagePreview", "Decode returned null for $url")
                        RemotePreviewState.Failed
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RemoteImagePreview", "Fetch threw for $url", e)
                RemotePreviewState.Failed
            }
        }
    }
    when (val s = state) {
        is RemotePreviewState.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Ink, modifier = Modifier.size(20.dp))
        }
        is RemotePreviewState.Expired -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Ink, modifier = Modifier.size(20.dp))
        }
        is RemotePreviewState.Failed -> Box(modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AddPhotoAlternate, "$label (không tải được)", tint = Ink)
        }
        is RemotePreviewState.Loaded -> Image(s.bitmap, label, modifier.clip(CardShape), contentScale = ContentScale.Crop)
    }
}