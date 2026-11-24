package com.carled.app.presentation.screens.control

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.carled.app.presentation.components.HardButton
import com.carled.app.presentation.theme.Ink

@Composable
fun FirmwareUpdateSection(vm: FirmwareUpdateViewModel, connected: Boolean) {
    val state by vm.state.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val target = state.check?.targetFirmware

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Phiên bản hiện tại · ${state.info?.version ?: state.check?.currentVersion ?: "Chưa xác định"}", color = Ink)
        state.check?.currentVersionStatus?.let { Text("Trạng thái bản phát hành · $it", color = Ink) }
        if (target != null) {
            Text(
                if (state.check?.isRollback == true) "Khôi phục về ${target.version}" else "Có bản cập nhật ${target.version}",
                color = Ink
            )
            target.changelog?.takeIf { it.isNotBlank() }?.let { Text(it, color = Ink) }
            Text("Dung lượng · ${target.fileSizeBytes / 1024} KB", color = Ink)
        } else if (state.check != null && !state.check!!.updateAvailable) {
            Text("Thiết bị đang dùng bản firmware được phát hành.", color = Ink)
        }
        state.message?.let { Text(it, color = Ink) }
        if (state.working) {
            val progress = state.progress
            if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            else {
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                Text("$progress%", color = Ink)
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.pending) {
            Text("Giữ thiết bị có nguồn. Điện thoại sẽ kết nối lại để xác minh kết quả cập nhật.", color = Ink)
        }
        if (!connected && !state.pending) {
            Text("Kết nối Bluetooth để đọc phiên bản và bắt đầu cập nhật.", color = Ink)
        }
        if (connected && !state.pending && state.info?.wifi == false) {
            Text("Thiết bị cần Wi-Fi có Internet để tải firmware.", color = Ink)
            OutlinedTextField(
                value = ssid, onValueChange = { ssid = it }, label = { Text("Tên Wi-Fi / SSID") },
                enabled = !state.working, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Mật khẩu Wi-Fi") },
                enabled = !state.working, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            HardButton(
                text = "Kết nối Wi-Fi", enabled = !state.working && ssid.isNotBlank(),
                onClick = { vm.configureWifi(ssid, password); password = "" },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            )
        }
        HardButton(
            text = if (state.pending) "Xác minh kết quả cập nhật" else "Kiểm tra bản cập nhật",
            enabled = !state.working, onClick = vm::checkForUpdate, filled = false,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        )
        if (target != null && !state.pending) {
            HardButton(
                text = if (state.check?.isRollback == true) "Khôi phục firmware" else "Cập nhật firmware",
                enabled = connected && !state.working && state.info?.wifi == true && state.info?.configured == true,
                onClick = { confirm = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            )
            if (state.info?.configured == false) {
                Text("Firmware trên thiết bị chưa được cấu hình máy chủ OTA và chứng chỉ HTTPS.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirm && target != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(if (state.check?.isRollback == true) "Khôi phục về ${target.version}?" else "Cập nhật lên ${target.version}?") },
            text = { Text("Giữ nguồn điện và Wi-Fi cho thiết bị trong lúc cập nhật. Thiết bị sẽ tự khởi động lại.") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.update() }) { Text("Bắt đầu") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Hủy") } }
        )
    }
}
