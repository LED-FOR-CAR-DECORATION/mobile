package com.carled.app.presentation.screens.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Tự ẩn thông báo feedback sau 3.5s
    LaunchedEffect(uiState.feedbackMessage) {
        if (uiState.feedbackMessage != null) {
            delay(3500)
            viewModel.clearFeedback()
        }
    }

    val topTabs = listOf("Bộ sưu tập", "Chuyên gia", "Tập ảnh")
    val subCategories = listOf("Haunted2026", "Đề xuất", "MỚI", "Nhân vật", "Ngày lễ", "Anime", "Xe hơi")

    val filteredItems = remember(uiState.pixelItems, uiState.selectedCategory, uiState.searchQuery) {
        uiState.pixelItems.filter { item ->
            val matchCategory = when (uiState.selectedCategory) {
                "MỚI" -> true // Hiển thị toàn bộ tác phẩm mới nhất
                "Đề xuất" -> item.likes >= 50
                else -> item.category.equals(uiState.selectedCategory, ignoreCase = true)
            }
            val matchSearch = if (uiState.searchQuery.isBlank()) true else {
                item.title.contains(uiState.searchQuery, ignoreCase = true) ||
                        item.author.contains(uiState.searchQuery, ignoreCase = true) ||
                        item.category.contains(uiState.searchQuery, ignoreCase = true)
            }
            matchCategory && matchSearch
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(DarkBackground)) {
                // Top Bar với Tabs & Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }

                    // 3 Tab chính: Bộ sưu tập, Chuyên gia, Tập ảnh
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        topTabs.forEachIndexed { index, title ->
                            val isSelected = uiState.selectedTopTab == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .clickable { viewModel.selectTopTab(index) }
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else Color(0xFF888E9B),
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .width(28.dp)
                                        .background(if (isSelected) Color(0xFFFFA726) else Color.Transparent)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = Color.White
                        )
                    }
                }

                // Thanh tìm kiếm mở rộng (nếu được kích hoạt)
                if (uiState.isSearchOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Tìm tên tác phẩm, tác giả...", fontSize = 13.sp, color = DivoomTextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DivoomCard,
                                unfocusedContainerColor = DivoomCard,
                                focusedBorderColor = DivoomGold,
                                unfocusedBorderColor = DivoomBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = DivoomTextMuted)
                                    }
                                }
                            }
                        )
                    }
                }

                // Sub-Category Bar cuộn ngang
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(subCategories) { cat ->
                            val isSelected = uiState.selectedCategory == cat
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.selectCategory(cat) }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color(0xFFFFCA28) else Color(0xFF888E9B),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .height(2.dp)
                                            .width(16.dp)
                                            .background(Color(0xFFFFCA28))
                                    )
                                }
                            }
                        }
                    }

                    // Nút Bộ lọc (Tune icon)
                    IconButton(
                        onClick = { /* Mở bộ lọc nâng cao */ },
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Bộ lọc",
                            tint = Color(0xFF888E9B),
                            modifier = Modifier.size(20.dp)
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                // Banner Quảng Cáo Tím Gradient phong cách Divoom
                if (uiState.showBanner) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF6B1170),
                                        Color(0xFF901C98),
                                        Color(0xFF5D1294)
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Creating with Divoom",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Get Your First Divoom Product With 10% OFF",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "www.divoom.com",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Icon thiết bị Pixel & Nút tắt banner
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.dismissBanner() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tắt banner",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Lưới 3 Cột Pixel Art Grid (3-column layout như hình mẫu)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredItems) { item ->
                        PixelArtCardItem(
                            item = item,
                            onClick = { viewModel.selectItemDetail(item) }
                        )
                    }
                }
            }

            // Thông báo Floating Banner ở đáy màn hình
            val bannerText = uiState.feedbackMessage
            AnimatedVisibility(
                visible = bannerText != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                if (bannerText != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F201B))
                            .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("● ", color = NeonGreen, fontWeight = FontWeight.Bold)
                        Text(bannerText, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Modal Chi Tiết Tác Phẩm & Tùy Chọn Chiếu Lên Xe
    if (uiState.selectedItemDetail != null) {
        val item = uiState.selectedItemDetail!!
        AlertDialog(
            onDismissRequest = { viewModel.selectItemDetail(null) },
            containerColor = DivoomCard,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tác giả: @${item.author} • ${item.category}", color = DivoomTextMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = { viewModel.toggleLike(item.id) }) {
                        Icon(
                            imageVector = if (item.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (item.isLiked) Color(0xFFFF1744) else Color.White
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Preview lớn 1:1 của Pixel Art
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, DivoomBorder, RoundedCornerShape(8.dp))
                    ) {
                        PixelCanvasDrawing(
                            item = item,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("❤️ ${item.likes} lượt thích", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("💬 ${item.comments} bình luận", color = DivoomTextMuted, fontSize = 12.sp)
                        if (item.hasAudio) {
                            Text("🔊 Có âm thanh", color = NeonGreen, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nút Chiếu Ngay lên Xe
                    Button(
                        onClick = { viewModel.applyPixelArtToCar(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !uiState.isApplying
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chiếu Lên Xe Ngay", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Nút Lưu Vĩnh Viễn vào Flash Xe
                    Button(
                        onClick = { viewModel.burnPixelArtToFlash(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DivoomGold),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !uiState.isApplying
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lưu Vĩnh Viễn Vào Xe (Flash)", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.selectItemDetail(null) }) {
                    Text("Đóng", color = DivoomTextMuted)
                }
            }
        )
    }
}

/**
 * Thẻ hiển thị từng tác phẩm Pixel Art trong lưới 3 cột (chuẩn 100% hình Divoom)
 */
@Composable
private fun PixelArtCardItem(
    item: PixelArtStoreItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151F))
    ) {
        Column {
            // Khung vuông hiển thị tranh Pixel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black)
            ) {
                PixelCanvasDrawing(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )

                // Biểu tượng loa nếu có âm thanh
                if (item.hasAudio) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Audio",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Thanh số liệu: Tim ♥ và Bình luận 💬 dưới mỗi tranh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF171A26))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Likes",
                        tint = if (item.isLiked) Color(0xFFFF3366) else Color(0xFF888E9B),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.likes.toString(),
                        color = Color(0xFF888E9B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = Color(0xFF888E9B),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.comments.toString(),
                        color = Color(0xFF888E9B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Vẽ trực tiếp lưới Pixel Art 16×16 lên Canvas theo chuẩn pixel sắc nét
 */
@Composable
private fun PixelCanvasDrawing(
    item: PixelArtStoreItem,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cellW = size.width / item.width.toFloat()
        val cellH = size.height / item.height.toFloat()

        for (row in 0 until item.height) {
            val rowStr = item.pattern.getOrElse(row) { "".padEnd(item.width, '.') }
            for (col in 0 until item.width) {
                val char = rowStr.getOrElse(col) { '.' }
                val color = item.colorPalette[char] ?: Color.Black
                drawRect(
                    color = color,
                    topLeft = Offset(col * cellW, row * cellH),
                    size = Size(cellW, cellH)
                )
            }
        }
    }
}
