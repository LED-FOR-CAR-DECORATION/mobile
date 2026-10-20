package com.carled.app.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.*

@Composable
fun DivoomProfileScreen(
    userName: String = "Tài xế Car LED",
    userEmail: String? = null,
    onOpenStore: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(1) } // 0: Thư viện máy, 1: Tải lên, 2: Yêu thích
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Xác nhận đăng xuất", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Bạn có chắc chắn muốn đăng xuất tài khoản khỏi ứng dụng Car LED không?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DivoomDanger)
                ) {
                    Text("Đăng xuất", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Hủy", color = DivoomTextMuted)
                }
            },
            containerColor = DivoomCard,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Actions (Share / Settings)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = DivoomDanger)
            }
        }

        // 1. Header Profile & Mascot Robot (Ảnh 5 Divoom)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Pixel
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF9100), Color(0xFFFF1744))
                        )
                    )
                    .border(2.dp, DivoomGold, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DivoomGold)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Lv.1", color = DarkBackground, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Huy hiệu
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.LightGray.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userEmail?.let { "Email: $it" } ?: "Bio: Chủ nhân CarLED Matrix 64x32",
                    color = NeonGreen,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Mascot Robot Speech Bubble (Ảnh 5 Divoom)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DivoomCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Này! Hãy hoàn thành sáng tạo nghệ thuật pixel hôm nay!",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                // Mascot robot icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DivoomCardSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Robot",
                        tint = DivoomGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Banner Divoom eStore (Flow 1 E-commerce)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8BBD0)),
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .clickable { onOpenStore() }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF880E4F), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Car LED eStore — Phụ kiện & Hoạt ảnh xe",
                        color = Color(0xFF880E4F),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Stats Counter Bar (6 cột số liệu)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DivoomCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatColumn(Icons.Default.ThumbUp, "Thích", "0012")
                    StatColumn(Icons.Default.People, "Fan", "0005")
                    StatColumn(Icons.Default.PersonAdd, "Theo dõi", "0024")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = DivoomBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatColumn(Icons.Default.ConfirmationNumber, "Vé BH", "0001")
                    StatColumn(Icons.Default.MonetizationOn, "Điểm xe", "0500")
                    StatColumn(Icons.Default.Stars, "Huy hiệu", "0003")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Tabs: Thư viện trên máy | Tải lên | Yêu thích
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ProfileTab("Thư viện trên máy", selectedSubTab == 0) { selectedSubTab = 0 }
            ProfileTab("Tải lên", selectedSubTab == 1) { selectedSubTab = 1 }
            ProfileTab("Yêu thích", selectedSubTab == 2) { selectedSubTab = 2 }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nội dung danh sách theo tab
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DivoomCard),
            modifier = Modifier.fillMaxWidth().height(160.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = DivoomTextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedSubTab == 1) "Bạn đã tải 2 hoạt ảnh lên bảng LED xe" else "Chưa có nội dung",
                        color = DivoomTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Nút đăng xuất tài khoản rõ ràng
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DivoomCard),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLogoutDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = DivoomDanger, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Đăng Xuất Tài Khoản",
                    color = DivoomDanger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun StatColumn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = label, tint = DivoomGold, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(count, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun ProfileTab(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else DivoomTextMuted,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(if (isSelected) DivoomGold else Color.Transparent)
        )
    }
}
