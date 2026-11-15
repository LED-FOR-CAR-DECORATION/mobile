package com.carled.app.presentation.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import com.carled.app.R
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.data.local.PixelDraftStore
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.User
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.GreenLight
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
    val connectionState by control.connectionState.collectAsState()
    val ledState by control.ledState.collectAsState()
    val device = connectionState as? ConnectionState.Connected
    val context = LocalContext.current
    val drafts = remember { runCatching { PixelDraftStore(context).drafts() }.getOrDefault(emptyList()) }
    var confirmLogout by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
            .padding(8.dp)
    ) {
        WindowFrame(
            title = "My profile",
            onClose = back,
            modifier = Modifier.fillMaxSize()
        ) {
            DeviceSection(title = "Account info") {
                AccountInfoRow(user)
            }

            DeviceSection(title = "My gallery", onNavigate = onOpenLibraryTab) {
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
                                    .flatSurface(color = Cream)
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

            DeviceSection(title = "Active panels", onNavigate = onOpenControlTab) {
                if (device == null) {
                    EmptyRow(
                        if (connectionState is ConnectionState.Connecting) "Đang kết nối…"
                        else "Chưa có thiết bị kết nối."
                    )
                } else {
                    ConnectedPanelCard(
                        name = device.deviceName,
                        powerOn = ledState.isPowerOn,
                        mode = ledState.currentMode.displayName,
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
    Row(
        Modifier
            .fillMaxWidth()
            .flatSurface(color = Cream, borderWidth = 3.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(72.dp)
                .flatSurface(color = GreenLight, borderWidth = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_user),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                user?.fullName?.ifBlank { "Nickname" } ?: "Nickname",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(user?.email?.ifBlank { "Email" } ?: "Email", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Edit", color = GreenDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}