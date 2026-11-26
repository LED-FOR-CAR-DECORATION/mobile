package com.carled.app.presentation.screens.commission

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carled.app.core.common.Resource
import com.carled.app.domain.model.commission.CommissionMessageResponse
import com.carled.app.domain.model.commission.CommissionResponse
import com.carled.app.presentation.components.MirageButton
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink
import org.json.JSONObject
import java.time.Instant

@Composable
internal fun CommissionTimeline(
    commission: CommissionResponse,
    model: CommissionViewModel,
    onClose: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenPayment: (String) -> Unit
) {
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

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Cream)
                    .border(1.dp, Ink)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                MirageButton(
                    "Thanh toán ngay · ${formatVnd(commission.totalPrice)}",
                    { onOpenPayment(code) },
                    Modifier.fillMaxWidth()
                )
            }

            if (commission.attachments.isNotEmpty()) {
                Text("Ảnh tham khảo", color = Ink, style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    commission.attachments.take(3).forEach { att ->
                        RemoteImagePreview(
                            att.imageUrl,
                            att.caption ?: "Reference",
                            Modifier.weight(1f).aspectRatio(ImagePreviewAspectRatio),
                            onExpired = onImageExpired
                        )
                    }
                }
            }
            val latestVersionNumber = remember(commission.deliveredPreviews) {
                commission.deliveredPreviews.maxOfOrNull { it.versionNumber }
            }

            if (commission.deliveredPreviews.isNotEmpty()) {
                Text("Bản xem trước đã giao", color = Ink, style = MaterialTheme.typography.labelLarge)
                commission.deliveredPreviews
                    .sortedByDescending { it.versionNumber }
                    .forEach { preview ->
                        val isLatest = preview.versionNumber == latestVersionNumber
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(Cream)
                                .border(1.dp, Ink)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RemoteImagePreview(
                                preview.imageUrl,
                                "Preview v${preview.versionNumber}",
                                Modifier.fillMaxWidth().aspectRatio(ImagePreviewAspectRatio),
                                onExpired = onImageExpired
                            )
                            Text("v${preview.versionNumber} · ${preview.status}", color = Ink, fontWeight = FontWeight.Bold)
                            preview.designerNote?.let { Text(it, color = Ink, style = MaterialTheme.typography.bodySmall) }
                            if (isLatest && preview.status == "STAFF_APPROVED" && commission.status == "PREVIEW_DELIVERED") {
                                MirageButton(
                                    "Duyệt bản xem trước này",
                                    { model.approveDeliverable(commission.commissionId) },
                                    Modifier.fillMaxWidth()
                                )
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
internal fun PaymentDialog(paymentCode: String, model: CommissionViewModel, onClose: () -> Unit) {
    val paymentState by model.payment.collectAsState()

    LaunchedEffect(paymentCode) { model.loadPayment(paymentCode) }

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
                        Modifier
                            .fillMaxWidth()
                            .background(GreenLight)
                            .border(1.dp, Ink)
                            .padding(12.dp),
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
                            MirageButton(
                                "Tôi đã chuyển khoản · Kiểm tra ngay",
                                { model.loadPayment(paymentCode) },
                                Modifier.fillMaxWidth()
                            )
                            MirageButton(
                                "Mô phỏng thanh toán thành công (staging)",
                                { model.mockPaySuccess(paymentCode) },
                                Modifier.fillMaxWidth(),
                                secondary = true
                            )
                        }
                        else -> MirageButton("Đóng", onClose, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommissionConversation(commissionId: Long, model: CommissionViewModel, onClose: () -> Unit) {
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
    val onImageExpired = remember(commissionId) { { model.refreshMessages(commissionId) } }

    DisposableEffect(commissionId) {
        model.loadMessages(commissionId)
        model.loadDetail(commissionId)
        model.connectChat(commissionId)
        onDispose { model.disconnectChat() }
    }
    LaunchedEffect(messages.size, scroll.maxValue) {
        if (scroll.maxValue != Int.MAX_VALUE) scroll.scrollTo(scroll.maxValue)
    }
    LaunchedEffect(actionResult) {
        if (actionResult is Resource.Success) model.consumeActionResult()
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
                    Modifier
                        .fillMaxWidth()
                        .background(Cream, CardShape)
                        .border(1.dp, Ink, CardShape),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { picker.launch(ImagePickerRequest) }) {
                        Icon(Icons.Default.AddPhotoAlternate, "Attach image", tint = Ink)
                    }
                    OutlinedTextField(
                        text,
                        { text = it },
                        Modifier.weight(1f),
                        placeholder = { Text("Message", color = Ink) },
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
            Modifier
                .widthIn(max = 224.dp)
                .background(if (outgoing) Green else Cream, CardShape)
                .border(1.dp, Ink, CardShape)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (message.messageType) {
                "DELIVERABLE_CARD" -> {
                    Text("Bản xem trước đã sẵn sàng", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    message.content?.let { Text(it, color = Ink) }
                    message.mediaUrl?.let { url ->
                        RemoteImagePreview(
                            url,
                            "Deliverable preview",
                            Modifier.width(180.dp).aspectRatio(ImagePreviewAspectRatio),
                            onExpired = onImageExpired
                        )
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
                        RemoteImagePreview(
                            url,
                            "Attached image",
                            Modifier.width(180.dp).aspectRatio(ImagePreviewAspectRatio),
                            onExpired = onImageExpired
                        )
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

private fun extractJsonField(json: String?, field: String): String? {
    if (json.isNullOrBlank()) return null
    return try {
        JSONObject(json).optString(field).takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}

private fun parseIsoToMillis(iso: String?): Long? = try {
    iso?.let { Instant.parse(it).toEpochMilli() }
} catch (_: Exception) {
    null
}