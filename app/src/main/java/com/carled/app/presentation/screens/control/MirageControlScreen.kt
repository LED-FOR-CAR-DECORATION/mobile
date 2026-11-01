package com.carled.app.presentation.screens.control

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.*

@Composable
fun MirageControlScreen(
    vm: ControlViewModel,
    back: () -> Unit,
    scanner: () -> Unit,
    openPanelControl: (String) -> Unit = {},
    userId: String = ""
) {
    val state by vm.uiState.collectAsState()
    val device = state.connectionState as? ConnectionState.Connected
    val firmware = remember { MockDeviceInventory.firmwareReleases() }

    Box(Modifier.fillMaxSize().background(MirageBackground).padding(16.dp)) {
        WindowFrame(
            title = "Thiết bị của tôi",
            onClose = back,
            modifier = Modifier.fillMaxSize()
        ) {
            DeviceSection(title = "Owned device:", onAdd = scanner) {
                if (device == null) {
                    EmptyRow(
                        if (state.connectionState is ConnectionState.Connecting) "Đang kết nối…"
                        else "Chưa có thiết bị kết nối. Nhấn + để thêm."
                    )
                } else {
                    ConnectedPanelCard(
                        name = device.deviceName,
                        powerOn = state.ledState.isPowerOn,
                        mode = state.ledState.currentMode.displayName,
                        address = device.deviceAddress,
                        onOpenControl = { openPanelControl("current") }
                    )
                }
            }

            DeviceSection(title = "Firmware manager:", onAdd = null) {
                firmware.forEach { release -> FirmwareReleaseCard(release) }
            }

            state.feedbackMessage?.let { Text(it, color = MirageMuted, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun DeviceSection(
    title: String,
    onAdd: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GreenLight)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Cream, fontSize = 18.sp, style = MaterialTheme.typography.titleLarge)
            if (onAdd != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Cream)
                        .border(1.5.dp, Ink, RoundedCornerShape(6.dp))
                        .clickable(onClick = onAdd)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) { PlusGlyph(color = Ink) }
            }
        }
        content()
    }
}

@Composable
private fun EmptyRow(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Cream)
            .border(2.dp, Ink, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) { Text(text, color = MirageMuted) }
}

@Composable
private fun ConnectedPanelCard(
    name: String,
    address: String,
    powerOn: Boolean,
    mode: String,
    onOpenControl: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Cream)
            .border(2.dp, Ink, RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenControl)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, color = Ink, fontSize = 16.sp, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Trạng thái: Đã kết nối Bluetooth", color = MirageMuted, fontSize = 13.sp)
            Text("Nguồn: ${if (powerOn) "Bật" else "Tắt"}", color = MirageMuted, fontSize = 13.sp)
            Text("Chế độ: $mode", color = MirageMuted, fontSize = 13.sp)
            Text("Firmware version: Chưa báo cáo", color = MirageMuted, fontSize = 13.sp)
            Text("Nhấn để điều khiển", color = GreenDark, fontSize = 11.sp)
        }
        ChevronGlyph()
    }
}

@Composable
private fun FirmwareReleaseCard(release: FirmwareRelease) {
    val dimmed = release.tag == FirmwareTag.LEGACY_OUTDATED
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Cream)
            .border(2.dp, Ink, RoundedCornerShape(8.dp))
            .alpha(if (dimmed) 0.55f else 1f)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(release.label, color = Ink, fontSize = 16.sp, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Compatible for: ${release.compatibility}", color = MirageMuted, fontSize = 13.sp)
            Text("Release date: ${release.releaseDate}", color = MirageMuted, fontSize = 13.sp)
            if (release.tag == FirmwareTag.LEGACY_OUTDATED) {
                Text("Out-dated", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
        ChevronGlyph()
    }
}