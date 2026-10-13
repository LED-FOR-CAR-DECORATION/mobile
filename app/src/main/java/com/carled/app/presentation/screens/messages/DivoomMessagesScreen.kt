package com.carled.app.presentation.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.*

@Composable
fun DivoomMessagesScreen(
    onOpenSupportChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var topSelectedTab by remember { mutableStateOf(1) } // 0: Mail, 1: Community, 2: Megaphone

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Icons: Mail | Community | Announcement (Ảnh 4 Divoom)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { topSelectedTab = 0 }) {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = "Mail",
                    tint = if (topSelectedTab == 0) DivoomGold else DivoomTextMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
            IconButton(onClick = { topSelectedTab = 1 }) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Community",
                    tint = if (topSelectedTab == 1) DivoomGold else DivoomTextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = { topSelectedTab = 2 }) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Announcements",
                    tint = if (topSelectedTab == 2) DivoomGold else DivoomTextMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Nhóm # Community
        Text(
            text = "▾ # Community:",
            color = DivoomTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DivoomCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ChannelItem(
                    title = "Chat Hội Chủ Xe Car LED",
                    subtitle = "Thảo luận độ đèn & gắn kính lái",
                    iconColor = Color(0xFFFF4081),
                    badgeCount = 8,
                    onClick = onOpenSupportChat
                )
                Divider(color = DivoomBorder, thickness = 0.5.dp)
                ChannelItem(
                    title = "Gallery Chia Sẻ Hoạt Ảnh",
                    subtitle = "Kho pixel art từ cộng đồng",
                    iconColor = Color(0xFF00E676),
                    badgeCount = null,
                    onClick = {}
                )
                Divider(color = DivoomBorder, thickness = 0.5.dp)
                ChannelItem(
                    title = "AI Drawing - Tạo Hoạt Ảnh Xe",
                    subtitle = "Nhập mô tả tạo icon pixel tự động",
                    iconColor = Color(0xFFFFA726),
                    badgeCount = null,
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Nhóm # Help & Kỹ thuật
        Text(
            text = "▾ # Help & Support:",
            color = DivoomTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DivoomCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ChannelItem(
                    title = "Kỹ Thuật Viên Hỗ Trợ Lắp Đặt",
                    subtitle = "Tư vấn nguồn điện 12V-5V & bảo hành",
                    iconColor = PrimaryCyan,
                    badgeCount = 1,
                    onClick = onOpenSupportChat
                )
                Divider(color = DivoomBorder, thickness = 0.5.dp)
                ChannelItem(
                    title = "Hướng Dẫn Kết Nối Bluetooth BLE",
                    subtitle = "Xử lý lỗi ghép nối ESP32",
                    iconColor = AccentPurple,
                    badgeCount = null,
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Nhóm # Ngôn ngữ (Language)
        Text(
            text = "▾ # Language:",
            color = DivoomTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DivoomCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LanguageItem("Tiếng Việt (Mặc định)", "Xin chào")
                Divider(color = DivoomBorder, thickness = 0.5.dp)
                LanguageItem("English", "Hello")
                Divider(color = DivoomBorder, thickness = 0.5.dp)
                LanguageItem("日本語", "こんにちは")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun ChannelItem(
    title: String,
    subtitle: String,
    iconColor: Color,
    badgeCount: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChatBubble, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = DivoomTextMuted, fontSize = 11.sp)
            }
        }

        if (badgeCount != null) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(DivoomCardSecondary),
                contentAlignment = Alignment.Center
            ) {
                Text(badgeCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LanguageItem(name: String, sample: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = Color.White, fontSize = 13.sp)
        Text(sample, color = DivoomGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
