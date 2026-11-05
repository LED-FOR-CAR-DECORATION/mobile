package com.carled.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.FirmwareRelease
import com.carled.app.domain.model.FirmwareTag
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink

// Shared by MirageControlScreen (full device/firmware manager) and MirageProfileScreen
// (the compact "Active panels" summary), so both render identical cards.

@Composable
fun DeviceSection(
    title: String,
    onAdd: (() -> Unit)? = null,
    onNavigate: (() -> Unit)? = null,
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
            Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            when {
                onAdd != null -> Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Cream)
                        .border(1.5.dp, Ink, RoundedCornerShape(6.dp))
                        .clickable(onClick = onAdd)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) { PlusGlyph(color = Ink) }
                onNavigate != null -> Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Cream)
                        .border(1.5.dp, Ink, CircleShape)
                        .clickable(onClick = onNavigate)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
                }
            }
        }
        content()
    }
}

@Composable
fun EmptyRow(text: String) {
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
fun ConnectedPanelCard(
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
            Text(name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Trạng thái: Đã kết nối Bluetooth", color = MirageMuted, fontSize = 13.sp)
            Text("Nguồn: ${if (powerOn) "Bật" else "Tắt"}", color = MirageMuted, fontSize = 13.sp)
            Text("Chế độ: $mode", color = MirageMuted, fontSize = 13.sp)
            Text("Địa chỉ phần cứng: $address", color = MirageMuted, fontSize = 13.sp)
            Text("Firmware version: Chưa báo cáo", color = MirageMuted, fontSize = 13.sp)
            Text("Nhấn để điều khiển", color = GreenDark, fontSize = 11.sp)
        }
        ChevronGlyph()
    }
}

@Composable
fun FirmwareReleaseCard(release: FirmwareRelease) {
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
            Text(release.label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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

/** Lower-emphasis row (cream fill, Ink border) for a help/info link inside a card list. */
@Composable
fun GuideRow(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Cream)
            .border(1.5.dp, Ink, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = GreenDark, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = GreenDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}