package com.carled.app.presentation.screens.marquee

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarqueeScreen(
    viewModel: MarqueeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Hiển thị feedback tạm thời
    LaunchedEffect(uiState.feedbackMessage) {
        if (uiState.feedbackMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chạy Chữ LED", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Marquee Text", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Telemetry feedback card ──
            if (uiState.lastEventText != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("● ", color = NeonGreen, fontWeight = FontWeight.Bold)
                    Text(
                        "Đang phát: \"${uiState.lastEventText}\"",
                        color = NeonGreen,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Text Input ──
            Text("NỘI DUNG HIỂN THỊ", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.text,
                onValueChange = { viewModel.updateText(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập chữ chạy trên LED...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = SurfaceLightDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = PrimaryCyan,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3,
                supportingText = {
                    Text("* Ký tự tiếng Việt có dấu sẽ tự động được chuyển đổi", color = TextSecondary, fontSize = 11.sp)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Scroll Mode ──
            Text("CHẾ ĐỘ CUỘN", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("scroll_left" to "Cuộn trái", "static" to "Đứng yên", "blink" to "Nhấp nháy").forEach { (mode, label) ->
                    val selected = uiState.scrollMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) PrimaryCyan else SurfaceDark)
                            .border(1.dp, if (selected) PrimaryCyan else SurfaceLightDark, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateScrollMode(mode) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(label, color = if (selected) DarkBackground else TextPrimary, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Size ──
            Text("CỠ CHỮ", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "Nhỏ (5×7px)", 2 to "Lớn (10×14px)").forEach { (s, label) ->
                    val selected = uiState.size == s
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AccentPurple else SurfaceDark)
                            .border(1.dp, if (selected) AccentPurple else SurfaceLightDark, RoundedCornerShape(8.dp))
                            .clickable { viewModel.updateSize(s) }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(label, color = if (selected) Color.White else TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Speed ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TỐC ĐỘ CUỘN", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${uiState.speed}", color = PrimaryCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Slider(
                value = uiState.speed.toFloat(),
                onValueChange = { viewModel.updateSpeed(it.toInt()) },
                valueRange = 1f..100f,
                colors = SliderDefaults.colors(thumbColor = PrimaryCyan, activeTrackColor = PrimaryCyan, inactiveTrackColor = SurfaceLightDark)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Rainbow Toggle ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("HIỆU ỨNG CẦU VỒNG", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Chữ đổi màu 7 màu cầu vồng", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = uiState.rainbow,
                    onCheckedChange = { viewModel.toggleRainbow() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryCyan)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Màu chữ (nếu không rainbow) ──
            if (!uiState.rainbow) {
                Text("MÀU CHỮ", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val presetColors = listOf(
                    Triple(255, 215, 0) to "Vàng",
                    Triple(255, 255, 255) to "Trắng",
                    Triple(0, 200, 255) to "Cyan",
                    Triple(255, 50, 50) to "Đỏ",
                    Triple(50, 255, 100) to "Xanh lá",
                    Triple(200, 100, 255) to "Tím"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetColors.forEach { (rgb, name) ->
                        val color = Color(rgb.first / 255f, rgb.second / 255f, rgb.third / 255f)
                        val selected = uiState.r == rgb.first && uiState.g == rgb.second && uiState.b == rgb.third
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color)
                                .border(2.dp, if (selected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateColor(rgb.first, rgb.second, rgb.third) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Feedback Message ──
            if (uiState.feedbackMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonGreen.copy(alpha = 0.15f))
                        .padding(12.dp)
                ) {
                    Text(uiState.feedbackMessage!!, color = NeonGreen, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Send Button ──
            Button(
                onClick = { viewModel.sendMarquee() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isSending,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Phát Chạy Chữ", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
