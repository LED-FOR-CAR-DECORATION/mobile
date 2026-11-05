package com.carled.app.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.data.local.PixelDraftStore
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.User
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.Ink

@Composable
fun MirageProfileScreen(
    user: User?,
    control: ControlViewModel,
    back: () -> Unit,
    onOpenControlTab: () -> Unit,
    onOpenPanelControl: () -> Unit,
    onOpenLibraryTab: () -> Unit,
    onLogout: () -> Unit
) {
    val state by control.uiState.collectAsState()
    val device = state.connectionState as? ConnectionState.Connected
    val context = LocalContext.current
    val drafts = remember { runCatching { PixelDraftStore(context).drafts() }.getOrDefault(emptyList()) }
    var confirmLogout by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        WindowFrame(
            title = "My profile",
            onClose = back,
            modifier = Modifier.fillMaxSize()
        ) {
            DeviceSection(title = "Account info:") {
                AccountInfoRow(user)
            }

            DeviceSection(title = "My gallery:", onNavigate = onOpenLibraryTab) {
                if (drafts.isEmpty()) {
                    EmptyRow("Chưa có thiết kế nào. Vẽ DIY để thêm vào đây.")
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) { index ->
                            val draft = drafts.getOrNull(index)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Cream)
                                    .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
                                    .then(if (draft != null) Modifier.clickable(onClick = onOpenLibraryTab) else Modifier)
                            ) {
                                if (draft != null) {
                                    PixelPreview(draft.document, Modifier.fillMaxSize())
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Green)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            DeviceSection(title = "Active panels:", onNavigate = onOpenControlTab) {
                if (device == null) {
                    EmptyRow(
                        if (state.connectionState is ConnectionState.Connecting) "Đang kết nối…"
                        else "Chưa có thiết bị kết nối."
                    )
                } else {
                    ConnectedPanelCard(
                        name = device.deviceName,
                        powerOn = state.ledState.isPowerOn,
                        mode = state.ledState.currentMode.displayName,
                        address = device.deviceAddress,
                        onOpenControl = onOpenPanelControl
                    )
                }
            }

            HardMenuList(
                items = listOf(
                    HardMenuItem("Account Security"),
                    HardMenuItem("Settings"),
                    HardMenuItem("Logout") { confirmLogout = true }
                )
            )
        }

        if (confirmLogout) AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Đăng xuất?") },
            text = { Text("Bản vẽ đã lưu vẫn ở trên điện thoại này.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Đăng xuất") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun AccountInfoRow(user: User?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Cream)
                .border(1.5.dp, Ink, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            PersonGlyph(color = Ink, size = 28.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                user?.fullName?.ifBlank { "Người dùng Mirage" } ?: "Người dùng Mirage",
                color = Ink,
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Text(user?.email ?: "", color = MirageMuted, fontSize = 12.sp)
        }
    }
}