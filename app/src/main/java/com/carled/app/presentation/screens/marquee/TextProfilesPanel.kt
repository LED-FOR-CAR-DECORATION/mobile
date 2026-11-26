package com.carled.app.presentation.screens.marquee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.*

@Composable
fun TextProfilesPanel(vm: MarqueeViewModel, contentValid: Boolean) {
    val profiles by vm.profiles.collectAsState()
    val textState by vm.uiState.collectAsState()
    var choosing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var updating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<TextProfile?>(null) }
    var upgrade by remember { mutableStateOf(false) }
    val editable = profiles.loaded && !profiles.busy && !textState.isSending
    val selected = profiles.entries.firstOrNull { it.id == profiles.selectedId }

    DeviceSection(title = "Profile chữ · ${profiles.entries.size}/${profiles.plan.limit}") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("textProfiles")
                .flatSurface(color = Cream)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Gói ${profiles.plan.title} · Lưu trên điện thoại theo tài khoản",
                color = MirageMuted,
                fontSize = 12.sp
            )
            selected?.let {
                Box(
                    Modifier
                        .hardSurface(color = GreenLight, offset = 3.dp, borderWidth = 2.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Đang dùng: ${it.name}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HardButton(
                    text = "Chọn profile",
                    onClick = { choosing = true; vm.clearProfileError() },
                    enabled = editable && profiles.entries.isNotEmpty(),
                    filled = false,
                    shadowOffset = HardStyle.CardOffset,
                    modifier = Modifier.weight(1f).height(46.dp)
                )
                HardButton(
                    text = "Lưu mới",
                    onClick = { updating = false; name = ""; saving = true; vm.clearProfileError() },
                    enabled = editable && contentValid && profiles.entries.size < profiles.plan.limit,
                    shadowOffset = HardStyle.CardOffset,
                    modifier = Modifier.weight(1f).height(46.dp)
                )
            }
            if (selected != null) {
                HardButton(
                    text = "Cập nhật profile",
                    onClick = { updating = true; name = selected.name; saving = true; vm.clearProfileError() },
                    enabled = editable && contentValid,
                    filled = false,
                    shadowOffset = HardStyle.CardOffset,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                )
            }
            if (profiles.plan == TextProfilePlan.BASIC) {
                TextButton({ upgrade = true }) { Text("Mở rộng lên 10 profile") }
            }
            if (profiles.entries.size >= profiles.plan.limit) {
                Text("Đã đủ slot. Bạn vẫn có thể cập nhật hoặc xóa profile cũ.", color = MirageMuted, fontSize = 12.sp)
            }
            if (profiles.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            profiles.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            if (!profiles.loaded && !profiles.busy) {
                TextButton(vm::reloadProfiles) { Text("Thử tải lại profile") }
            }
        }
    }

    if (choosing) AlertDialog(
        onDismissRequest = { if (!profiles.busy) choosing = false },
        title = { Text("Profile đã lưu", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Chọn để nạp cấu hình, sau đó nhấn Hiển thị trên LED để phát.", color = MirageMuted)
                profiles.entries.forEach { profile ->
                    Column(
                        Modifier.fillMaxWidth().flatSurface(color = Cream).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(profile.name, color = Ink, fontWeight = FontWeight.Bold)
                        Text(profile.text, maxLines = 2, color = Ink)
                        Text(
                            "${if (profile.size == 2) "10×14" else "5×7"} · ${when (profile.mode) { "static" -> "Đứng yên"; "blink" -> "Nhấp nháy"; else -> "Cuộn trái" }}",
                            color = MirageMuted,
                            fontSize = 12.sp
                        )
                        Row {
                            TextButton({ vm.selectProfile(profile.id); choosing = false }, enabled = editable, modifier = Modifier.weight(1f)) { Text("Dùng ${profile.name}") }
                            TextButton({ deleting = profile }, enabled = editable, modifier = Modifier.weight(1f)) { Text("Xóa ${profile.name}") }
                        }
                    }
                }
                if (profiles.entries.isEmpty()) Text("Chưa có profile chữ.")
                profiles.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton({ choosing = false }, enabled = !profiles.busy) { Text("Đóng") } }
    )

    if (saving) AlertDialog(
        onDismissRequest = { if (!profiles.busy) saving = false },
        title = { Text(if (updating) "Cập nhật profile" else "Lưu profile mới", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HardTextField(value = name, onValueChange = { name = it.take(40) }, label = "Tên profile", enabled = !profiles.busy, modifier = Modifier.fillMaxWidth())
                Text("Lưu nội dung, màu, cỡ chữ, hiệu ứng và tốc độ hiện tại.", color = MirageMuted)
                profiles.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton({ vm.saveProfile(name, updating) { saving = false } }, enabled = name.isNotBlank() && !profiles.busy) { Text("Lưu profile") } },
        dismissButton = { TextButton({ saving = false }, enabled = !profiles.busy) { Text("Hủy") } }
    )

    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { if (!profiles.busy) deleting = null },
            title = { Text("Xóa profile?", color = Ink, fontWeight = FontWeight.Bold) },
            text = { Text("Xóa ${profile.name} khỏi các profile đã lưu trên điện thoại?") },
            confirmButton = { TextButton({ vm.deleteProfile(profile.id) { deleting = null } }, enabled = !profiles.busy) { Text("Xác nhận xóa") } },
            dismissButton = { TextButton({ deleting = null }, enabled = !profiles.busy) { Text("Hủy") } }
        )
    }

    if (upgrade) AlertDialog(
        onDismissRequest = { upgrade = false },
        title = { Text("Mở rộng profile chữ", color = Ink, fontWeight = FontWeight.Bold) },
        text = { Text("Gói mặc định: 3 profile. Gói mở rộng: 10 profile.\n\nGói mở rộng chưa được mở bán trong phiên bản này. Các profile hiện có vẫn được giữ lại khi thay đổi gói.") },
        confirmButton = { TextButton({ upgrade = false }) { Text("Đã hiểu") } }
    )
}