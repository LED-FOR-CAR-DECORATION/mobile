package com.carled.app.presentation.screens.commission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carled.app.core.common.Resource
import com.carled.app.domain.model.User
import com.carled.app.domain.model.commission.isChatActive
import com.carled.app.presentation.components.MirageButton
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink

private enum class CommissionTab(val label: String) {
    CREATE("Form dịch vụ"),
    PROGRESS("Lịch sử"),
    INBOX("Inbox")
}

@Composable
internal fun CommissionScreen(user: User, modifier: Modifier = Modifier) {
    val model: CommissionViewModel = viewModel(key = "commission:${user.id}")
    var tab by rememberSaveable(user.id) { mutableStateOf(CommissionTab.CREATE) }
    var selectedId by rememberSaveable(user.id) { mutableStateOf<Long?>(null) }
    var conversationId by rememberSaveable(user.id) { mutableStateOf<Long?>(null) }
    var paymentCode by rememberSaveable(user.id) { mutableStateOf<String?>(null) }

    WindowFrame(
        title = "Commission",
        modifier = modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxWidth()) {
            CommissionTabRow(
                selectedTab = tab,
                onSelectTab = { item ->
                    tab = item
                    if (item == CommissionTab.PROGRESS || item == CommissionTab.INBOX) {
                        model.loadCommissions()
                    }
                }
            )

            key(tab) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (tab) {
                        CommissionTab.CREATE -> CommissionForm(user, model) { id, code ->
                            tab = CommissionTab.PROGRESS
                            selectedId = id
                            model.loadDetail(id)
                            if (code != null) paymentCode = code
                        }
                        CommissionTab.PROGRESS -> CommissionHistoryTab(
                            model = model,
                            onSelectCommission = { id ->
                                selectedId = id
                                model.loadDetail(id)
                            }
                        )
                        CommissionTab.INBOX -> CommissionInboxTab(
                            model = model,
                            onOpenConversation = { id -> conversationId = id }
                        )
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
private fun CommissionTabRow(
    selectedTab: CommissionTab,
    onSelectTab: (CommissionTab) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectableGroup()
    ) {
        CommissionTab.entries.forEach { item ->
            val selected = selectedTab == item
            Box(
                Modifier
                    .weight(1f)
                    .background(if (selected) GreenLight else Cream)
                    .border(0.5.dp, Ink)
                    .selectable(selected, role = Role.Tab) { onSelectTab(item) }
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CommissionHistoryTab(
    model: CommissionViewModel,
    onSelectCommission: (Long) -> Unit
) {
    val commissionsState by model.commissions.collectAsState()
    CommissionPanel("Lịch sử đơn tạo") {
        when (val state = commissionsState) {
            is Resource.Loading -> {
                CircularProgressIndicator(color = Ink)
                Text("Đang tải…", color = Ink)
            }
            is Resource.Error -> {
                Text(state.message, color = Ink)
                MirageButton("Thử lại", { model.loadCommissions() })
            }
            is Resource.Success -> {
                if (state.data.isEmpty()) Text("Chưa có yêu cầu commission nào.", color = Ink)
                state.data.forEach { item ->
                    RequestCard(item) { onSelectCommission(item.commissionId) }
                }
            }
        }
    }
}

@Composable
private fun CommissionInboxTab(
    model: CommissionViewModel,
    onOpenConversation: (Long) -> Unit
) {
    val commissionsState by model.commissions.collectAsState()
    CommissionPanel("Quản lý inbox") {
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
                    RequestCard(item) { onOpenConversation(item.commissionId) }
                }
            }
        }
    }
}