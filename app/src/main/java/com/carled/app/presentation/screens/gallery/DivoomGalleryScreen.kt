package com.carled.app.presentation.screens.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import com.carled.app.domain.model.LedMode
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.theme.*

data class CharacterPreset(
    val id: String,
    val name: String,
    val resolution: String,
    val author: String,
    val previewGradient: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DivoomGalleryScreen(
    viewModel: ControlViewModel,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(0) }
    var activeMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Tất cả", "Xe hơi HUD", "Anime Pixel", "Cyberpunk", "Meme")

    val characters = listOf(
        CharacterPreset("1", "Neon Cyberpunk HUD", "64x32", "QuangKhanh", listOf(Color(0xFF00D2FF), Color(0xFF7928CA))),
        CharacterPreset("2", "Pixel Cat Driving", "64x32", "Divoom_Art", listOf(Color(0xFFFFA726), Color(0xFFFF5252))),
        CharacterPreset("3", "Retro Car 8-Bit", "64x16", "PhuocNguyen", listOf(Color(0xFF00E676), Color(0xFF1DE9B6))),
        CharacterPreset("4", "Digital Speedometer", "64x32", "CarHUD_Pro", listOf(Color(0xFFFF4081), Color(0xFFE040FB))),
        CharacterPreset("5", "Fire Flame Boost", "64x16", "DriftMaster", listOf(Color(0xFFFF9100), Color(0xFFFF1744))),
        CharacterPreset("6", "Pacman City Race", "64x32", "RetroGamer", listOf(Color(0xFFFFEA00), Color(0xFF2979FF)))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kho Nhân Vật Chiếu Kính Lái", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Category filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEachIndexed { index, cat ->
                    val isSelected = selectedCategory == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) DivoomGold else DivoomCard)
                            .clickable { selectedCategory = index }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            cat,
                            color = if (isSelected) DarkBackground else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeMessage ?: "",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Grid các nhân vật Pixel Art
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(characters) { char ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DivoomCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Ô mô phỏng khung hình pixel
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(char.previewGradient)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "[ ${char.resolution} ]",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = char.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )

                            Text(
                                text = "Bởi: ${char.author}",
                                color = DivoomTextMuted,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.setMode(LedMode.CUSTOM_PRESET)
                                    activeMessage = "Đang chiếu '${char.name}' lên kính lái xe!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DivoomGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth().height(34.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chiếu lên xe", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
