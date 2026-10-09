package com.carled.app.presentation.screens.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.LedMode
import com.carled.app.presentation.components.ColorPresetRow
import com.carled.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: ControlViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val led = uiState.ledState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Điều Khiển Đèn Xe", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStore) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cửa hàng", tint = PrimaryCyan)
                    }
                    IconButton(onClick = { viewModel.togglePower() }) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (led.isPowerOn) NeonGreen else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
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

            // Card mô phỏng màu sắc LED thời gian thực
            val currentColorCompose = Color(led.currentColor.red, led.currentColor.green, led.currentColor.blue)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(2.dp, currentColorCompose.copy(alpha = if (led.isPowerOn) 0.8f else 0.1f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (led.isPowerOn) "HIỆU ỨNG: ${led.currentMode.displayName.uppercase()}" else "HỆ THỐNG ĐANG TẮT",
                        color = if (led.isPowerOn) currentColorCompose else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mã màu: ${led.currentColor.toHex()} | Độ sáng: ${(led.brightness * 100 / 255)}%",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bảng chọn màu nhanh
            Text("BẢNG MÀU ÁNH SÁNG", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            ColorPresetRow(
                selectedColor = led.currentColor,
                onColorSelected = { color -> viewModel.setColor(color) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Chế độ hiệu ứng (Mode)
            Text("CHẾ ĐỘ HIỆU ỨNG (MODE)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LedMode.entries.filter { it != LedMode.OFF }) { mode ->
                    val isSelected = mode == led.currentMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryCyan else SurfaceDark)
                            .clickable { viewModel.setMode(mode) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) DarkBackground else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Thanh trượt độ sáng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ĐỘ SÁNG ĐÈN", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${(led.brightness * 100 / 255)}%", color = PrimaryCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Slider(
                value = led.brightness.toFloat(),
                onValueChange = { viewModel.setBrightness(it.toInt()) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryCyan,
                    activeTrackColor = PrimaryCyan,
                    inactiveTrackColor = SurfaceLightDark
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Thanh trượt tốc độ hiệu ứng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TỐC ĐỘ CHUYỂN MÀU", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${led.speed}%", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Slider(
                value = led.speed.toFloat(),
                onValueChange = { viewModel.setSpeed(it.toInt()) },
                valueRange = 1f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentPurple,
                    activeTrackColor = AccentPurple,
                    inactiveTrackColor = SurfaceLightDark
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
