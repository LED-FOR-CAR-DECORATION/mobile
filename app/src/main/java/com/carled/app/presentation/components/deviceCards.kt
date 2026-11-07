package com.carled.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.FirmwareRelease
import com.carled.app.domain.model.FirmwareTag
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.Ink
import com.carled.app.R
private val ThickBorder = 3.dp
private val SectionTitleSize = 20.sp

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
            .imageSurface(imageRes = R.drawable.bg_image_card, borderWidth = ThickBorder)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = Ink,
                    fontSize = SectionTitleSize,
                    fontWeight = FontWeight.ExtraBold
                )
                if (onNavigate != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .flatSurface(color = Cream, borderWidth = ThickBorder)
                            .clickable(onClick = onNavigate)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (onAdd != null) {
                Box(
                    Modifier
                        .flatSurface(color = Cream, borderWidth = ThickBorder)
                        .clickable(onClick = onAdd)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) { PlusGlyph(color = Ink) }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            content = content
        )
    }
}

@Composable
fun EmptyRow(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .flatSurface(color = Cream, borderWidth = ThickBorder)
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
            .flatSurface(color = Cream, borderWidth = ThickBorder)
            .clickable(onClick = onOpenControl)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                name,
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text("Trạng thái: Đã kết nối Bluetooth", color = MirageMuted, fontSize = 13.sp)
            Text(
                "Nguồn: ${if (powerOn) "Bật" else "Tắt"}",
                color = MirageMuted,
                fontSize = 13.sp
            )
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
            .flatSurface(color = Cream, borderWidth = ThickBorder)
            .alpha(if (dimmed) 0.55f else 1f)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                release.label,
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Compatible for: ${release.compatibility}",
                color = MirageMuted,
                fontSize = 13.sp
            )
            Text("Release date: ${release.releaseDate}", color = MirageMuted, fontSize = 13.sp)
            if (release.tag == FirmwareTag.LEGACY_OUTDATED) {
                Text("Out-dated", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
        ChevronGlyph()
    }
}

@Composable
fun GuideRow(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .flatSurface(color = Cream, borderWidth = ThickBorder)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.HelpOutline,
            contentDescription = null,
            tint = GreenDark,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = GreenDark, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}