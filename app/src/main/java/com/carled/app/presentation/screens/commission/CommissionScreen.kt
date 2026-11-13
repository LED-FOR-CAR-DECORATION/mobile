package com.carled.app.presentation.screens.commission

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carled.app.R
import com.carled.app.domain.model.User
import com.carled.app.presentation.components.MirageButton
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

private const val MAX_REFERENCES = 5
private val CardShape = RoundedCornerShape(3.dp)
private val ImagePickerRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
private val Steps = listOf(
    "Request created", "Staff is checking the request", "Staff approved the request",
    "Payment completed", "Assigning a designer", "Commission is in progress", "Commission completed"
)

private enum class CommissionTab(val label: String) {
    CREATE("Create request"), PROGRESS("Progress tracking"), INBOX("Inbox")
}

@Composable
internal fun CommissionScreen(user: User, modifier: Modifier = Modifier) {
    val model: CommissionViewModel = viewModel(key = "commission:${user.id}")
    var tab by rememberSaveable(user.id) { mutableStateOf(CommissionTab.CREATE) }
    var selectedId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var paymentId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    val request = model.requests.firstOrNull { it.id == selectedId }
    val checkout = model.requests.firstOrNull { it.id == paymentId }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            CommissionTab.entries.forEach { item ->
                val selected = tab == item
                Box(
                    Modifier.weight(1f)
                        .background(if (selected) GreenLight else Cream)
                        .border(0.5.dp, Ink)
                        .selectable(selected, role = Role.Tab) { tab = item }
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
                    CommissionTab.CREATE -> CommissionForm(user, model) { id ->
                        tab = CommissionTab.PROGRESS
                        paymentId = id
                    }
                    CommissionTab.PROGRESS -> CommissionPanel("Progress tracking") {
                        model.requests.forEach { item ->
                            RequestCard(item, item.draft.description, item.status) { selectedId = item.id }
                        }
                    }
                    CommissionTab.INBOX -> CommissionPanel("Incoming messages") {
                        model.requests.filter { it.messages.isNotEmpty() }.forEach { item ->
                            val last = item.messages.last()
                            val sender = if (last.outgoing) "You" else "Staff"
                            RequestCard(item, "$sender: ${last.preview}") { selectedId = item.id }
                        }
                        SectionTitle("Notifications", Modifier.padding(top = 8.dp))
                        model.requests.forEach { item ->
                            item.notification?.let { notification ->
                                RequestCard(item, notification) { selectedId = item.id }
                            }
                        }
                    }
                }
            }
        }
    }
    if (checkout?.payment != null) {
        MockSePayCheckout(
            checkout.payment,
            onClose = { paymentId = null },
            onConfirm = { model.simulateSePayReceipt(checkout.id) }
        )
    } else if (request != null) {
        val close = { selectedId = null }
        if (tab == CommissionTab.INBOX) {
            CommissionConversation(request, close) { model.send(request.id, it) }
        } else {
            CommissionTimeline(request, close) {
                selectedId = null
                paymentId = request.id
            }
        }
    }
}

@Composable
private fun CommissionForm(user: User, model: CommissionViewModel, onSubmit: (String) -> Unit) {
    val draft = model.draft
    var choosingType by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val current = model.draft
            model.draft = current.copy(references = (current.references + uri).distinct().take(MAX_REFERENCES))
        }
    }

    CommissionPanel("Create commission form") {
        CommissionField(user.fullName?.takeIf { it.isNotBlank() } ?: user.id, {}, "Username (user ID)", readOnly = true)
        Text("Commission type", color = Ink, style = MaterialTheme.typography.labelLarge)
        Box {
            MirageButton(draft.type.label, { choosingType = true }, secondary = true)
            DropdownMenu(expanded = choosingType, onDismissRequest = { choosingType = false }) {
                CommissionType.entries.forEach { type ->
                    DropdownMenuItem(text = { Text(type.label) }, onClick = {
                        model.draft = draft.copy(type = type)
                        choosingType = false
                    })
                }
            }
        }
        CommissionField(draft.description, { model.draft = draft.copy(description = it) }, "Description", multiline = true)
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
                            AttachmentPreview(uri, "Reference image", Modifier.fillMaxSize())
                            TextButton(
                                onClick = { model.draft = draft.copy(references = draft.references - uri) },
                                modifier = Modifier.align(Alignment.TopEnd).size(44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text("×", color = Ink, modifier = Modifier.background(Cream)) }
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (draft.type == CommissionType.GIF) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommissionField(draft.frameRate, { model.draft = draft.copy(frameRate = it) }, "Frame rate (FPS)",
                    Modifier.weight(1f), numeric = true)
                CommissionField(draft.frameCount, { model.draft = draft.copy(frameCount = it) }, "Frame count",
                    Modifier.weight(1f), numeric = true)
            }
            Text("Current frame price: 50¢ per frame", color = Ink)
            draft.estimateInCents?.let { cents ->
                val price = NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100.0)
                Text("Estimated price: $price", color = Ink)
            }
        }
        MirageButton("Submit request", {
            model.submit()?.let(onSubmit)
        }, Modifier.fillMaxWidth(), enabled = draft.isValid, secondary = true)
    }
}

@Composable
private fun CommissionField(
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
private fun RequestCard(request: CommissionRequest, detail: String, status: String? = null, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Cream).border(1.dp, Ink)
            .clickable(role = Role.Button, onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Commission request #${request.id}", color = Ink, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(detail, Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            status?.let { Text(it, color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CommissionTimeline(request: CommissionRequest, onClose: () -> Unit, openPayment: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        WindowFrame("Progress tracking", modifier = Modifier.fillMaxWidth(), onClose = onClose, compact = true) {
            Text("Request #${request.id} · ${request.status}", color = Ink, fontWeight = FontWeight.Bold)
            Text(request.draft.description, color = Ink)
            request.payment?.let { payment ->
                MirageButton(if (payment.isPaid) "View payment receipt" else "Open SePay payment", openPayment)
            }
            Steps.forEachIndexed { index, step ->
                val completed = index < request.completedSteps || (step == "Payment completed" && request.payment?.isPaid == true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(28.dp)) {
                        val stroke = 1.dp.toPx()
                        val centerX = size.width / 2
                        if (index > 0) drawLine(Ink, Offset(centerX, -16.dp.toPx()), center, stroke)
                        drawCircle(Ink, radius = 7.dp.toPx())
                        drawCircle(if (completed) Green else Cream, radius = 6.dp.toPx())
                    }
                    Text(
                        step, Modifier.weight(1f).border(1.dp, Ink, CardShape).padding(7.dp)
                            .semantics { stateDescription = if (completed) "Complete" else "Pending" },
                        color = Ink, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun MockSePayCheckout(payment: MockSePayPayment, onClose: () -> Unit, onConfirm: () -> Unit) {
    val amount = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(payment.amountVnd)
    Dialog(onDismissRequest = onClose) {
        WindowFrame("SePay · Demo payment", Modifier.fillMaxWidth(), onClose = onClose, compact = true) {
            SectionTitle(if (payment.isPaid) "Payment received" else "Awaiting bank transfer")
            Text("Simulation only. No money is transferred.", color = Ink)
            Column(
                Modifier.fillMaxWidth().background(GreenLight).border(1.dp, Ink).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Amount: $amount VND", color = Ink, fontWeight = FontWeight.Bold)
                Text("Bank account: DEMO ONLY", color = Ink)
                Text("Transfer content: ${payment.transferCode}", color = Ink)
                payment.transactionId?.let { Text("Transaction: $it", color = Ink) }
            }
            if (payment.isPaid) {
                MirageButton("Back to progress", onClose, Modifier.fillMaxWidth())
            } else {
                MirageButton("Simulate successful transfer", onConfirm, Modifier.fillMaxWidth())
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Pay later", color = Ink)
                }
            }
        }
    }
}

@Composable
private fun CommissionConversation(request: CommissionRequest, onClose: () -> Unit, onSend: (CommissionMessage) -> Unit) {
    var text by rememberSaveable(request.id) { mutableStateOf("") }
    var attachment by rememberSaveable(request.id) { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) attachment = uri
    }
    val scroll = rememberScrollState()
    LaunchedEffect(request.messages.size, scroll.maxValue) {
        if (scroll.maxValue != Int.MAX_VALUE) scroll.scrollTo(scroll.maxValue)
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WindowFrame(
            "Inbox · Request #${request.id}",
            modifier = Modifier.fillMaxSize().imePadding().padding(8.dp),
            onClose = onClose,
            compact = true,
            scrollState = scroll,
            fillContent = true,
            footer = {
                if (request.canceled) {
                    Text("This request has been canceled.", color = Ink)
                } else {
                    attachment?.let { uri ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AttachmentPreview(uri, "Pending attachment", Modifier.size(52.dp))
                            TextButton(onClick = { attachment = null }) { Text("Remove attachment", color = Ink) }
                        }
                    }
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
                                onSend(CommissionMessage(text.trim(), outgoing = true, attachment = attachment))
                                text = ""
                                attachment = null
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send message")
                        }
                    }
                }
            }
        ) {
            request.messages.forEach { message -> MessageBubble(message) }
        }
    }
}

@Composable
private fun MessageBubble(message: CommissionMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.outgoing) MessageAvatar(Green, Modifier.padding(end = 8.dp))
        Column(
            Modifier.widthIn(max = 224.dp).background(if (message.outgoing) Green else Cream, CardShape)
                .border(1.dp, Ink, CardShape).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (message.text.isNotBlank()) Text(message.text, color = Ink)
            if (message.sampleImage) {
                Image(painterResource(R.drawable.commission_sample_attachment), "Sample receipt",
                    Modifier.size(158.dp, 90.dp), contentScale = ContentScale.Crop)
            }
            message.attachment?.let { AttachmentPreview(it, "Attached image", Modifier.size(158.dp, 90.dp)) }
        }
        if (message.outgoing) MessageAvatar(Cream, Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun MessageAvatar(color: Color, modifier: Modifier) {
    Box(modifier.size(24.dp).background(color, CircleShape).border(1.dp, Ink, CircleShape))
}

@Composable
private fun AttachmentPreview(uri: Uri, label: String, modifier: Modifier) {
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
