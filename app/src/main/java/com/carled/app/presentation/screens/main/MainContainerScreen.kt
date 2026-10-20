package com.carled.app.presentation.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.ConnectionState
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.screens.dashboard.DivoomDashboardScreen
import com.carled.app.presentation.screens.gallery.DivoomGalleryScreen
import com.carled.app.presentation.screens.messages.DivoomMessagesScreen
import com.carled.app.presentation.screens.profile.DivoomProfileScreen
import com.carled.app.presentation.theme.*

data class DivoomTabItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun MainContainerScreen(
    controlViewModel: ControlViewModel,
    connectionState: ConnectionState,
    currentUser: com.carled.app.domain.model.User? = null,
    onOpenScanner: () -> Unit,
    onOpenCanvas: () -> Unit,
    onOpenMarquee: () -> Unit,
    onOpenUpload: () -> Unit,
    onOpenStore: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabs = listOf(
        DivoomTabItem("Thiết bị", Icons.Default.Explore),
        DivoomTabItem("Khuôn mặt", Icons.Default.GridView),
        DivoomTabItem("Tin nhắn", Icons.Default.ChatBubbleOutline),
        DivoomTabItem("Hồ Sơ", Icons.Default.AccountCircle)
    )

    Scaffold(
        bottomBar = {
            // Thanh Bottom Navigation 4 tab phong cách Divoom
            Column {
                Divider(color = DivoomBorder, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTabIndex = index }
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) DivoomGold else DivoomTextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tab.title,
                                color = if (isSelected) DivoomGold else DivoomTextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTabIndex) {
                0 -> DivoomDashboardScreen(
                    viewModel = controlViewModel,
                    connectionState = connectionState,
                    onOpenScanner = onOpenScanner,
                    onOpenCanvas = onOpenCanvas,
                    onOpenMarquee = onOpenMarquee,
                    onOpenGallery = { selectedTabIndex = 1 },
                    onOpenUpload = onOpenUpload,
                    onOpenStore = onOpenStore
                )
                1 -> DivoomGalleryScreen(
                    viewModel = controlViewModel
                )
                2 -> DivoomMessagesScreen(
                    onOpenSupportChat = onOpenStore
                )
                3 -> DivoomProfileScreen(
                    userName = currentUser?.fullName ?: "Tài xế Car LED",
                    userEmail = currentUser?.email,
                    onOpenStore = onOpenStore,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun DivoomCommunityTab(onOpenCanvas: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Public, contentDescription = null, tint = DivoomGold, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Cộng Đồng Pixel Xe Hơi",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Nơi các chủ xe cùng chia sẻ các hiệu ứng LED và nhân vật chiếu kính lái độc đáo nhất.",
            color = DivoomTextMuted,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenCanvas,
            colors = ButtonDefaults.buttonColors(containerColor = DivoomGold),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text("Vào Bảng Vẽ Pixel Ngay", color = DarkBackground, fontWeight = FontWeight.Bold)
        }
    }
}
