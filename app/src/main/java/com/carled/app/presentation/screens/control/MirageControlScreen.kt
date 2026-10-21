package com.carled.app.presentation.screens.control

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.carled.app.domain.model.ConnectionState
import com.carled.app.presentation.components.*

@Composable
fun MirageControlScreen(vm: ControlViewModel, back: () -> Unit, scanner: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val connected = state.connectionState is ConnectionState.Connected
    val led = state.ledState
    Scaffold(topBar = { MirageBar("Điều khiển LED", back) }, containerColor = MirageBackground) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text((state.connectionState as? ConnectionState.Connected)?.deviceName ?: "Thiết bị LED", style = MaterialTheme.typography.headlineSmall)
            Text(if (connected) "● Đã kết nối Bluetooth" else "Chưa kết nối LED", color = MirageMuted)
            if (!connected) MirageButton("Kết nối LED", scanner, Modifier.fillMaxWidth())
            MirageCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Nguồn LED", style = MaterialTheme.typography.titleMedium)
                        Text(if (led.isPowerOn) "Đang bật" else "Đang tắt", color = MirageMuted)
                    }
                    Switch(led.isPowerOn, { vm.togglePower() }, enabled = connected, modifier = Modifier.testTag("ledPower"))
                }
            }
            MirageCard {
                Text("Độ sáng đèn · ${(led.brightness * 100f / 255).roundToInt()}%", style = MaterialTheme.typography.titleMedium)
                Slider(led.brightness * 100f / 255, { vm.setBrightness((it * 255 / 100).roundToInt()) }, valueRange = 0f..100f, enabled = connected, modifier = Modifier.testTag("ledBrightness"))
            }
            state.feedbackMessage?.let { Text(it, color = MirageMuted) }
        }
    }
}
