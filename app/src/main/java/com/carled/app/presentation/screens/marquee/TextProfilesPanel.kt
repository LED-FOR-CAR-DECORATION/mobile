package com.carled.app.presentation.screens.marquee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*

@Composable
fun TextProfilesPanel(vm:MarqueeViewModel, contentValid:Boolean) {
    val profiles by vm.profiles.collectAsState()
    val textState by vm.uiState.collectAsState()
    var choosing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var updating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<TextProfile?>(null) }
    var upgrade by remember { mutableStateOf(false) }
    val editable=profiles.loaded && !profiles.busy && !textState.isSending
    val selected=profiles.entries.firstOrNull { it.id==profiles.selectedId }
    MirageCard(Modifier.fillMaxWidth().testTag("textProfiles")) {
        Text("Profile chữ · ${profiles.entries.size}/${profiles.plan.limit}", style=MaterialTheme.typography.titleMedium)
        Text("Gói ${profiles.plan.title} · Lưu trên điện thoại theo tài khoản", style=MaterialTheme.typography.bodySmall,color=MirageMuted)
        selected?.let { Text("Đang dùng: ${it.name}",color=MirageMuted) }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            MirageButton("Chọn profile",{ choosing=true;vm.clearProfileError() },Modifier.weight(1f),enabled=editable && profiles.entries.isNotEmpty(),secondary=true)
            MirageButton("Lưu mới",{ updating=false;name="";saving=true;vm.clearProfileError() },Modifier.weight(1f),enabled=editable && contentValid && profiles.entries.size < profiles.plan.limit)
        }
        if(selected!=null) MirageButton("Cập nhật profile",{ updating=true;name=selected.name;saving=true;vm.clearProfileError() },Modifier.fillMaxWidth(),enabled=editable && contentValid,secondary=true)
        if(profiles.plan==TextProfilePlan.BASIC) TextButton({ upgrade=true }) { Text("Mở rộng lên 10 profile") }
        if(profiles.entries.size>=profiles.plan.limit) Text("Đã đủ slot. Bạn vẫn có thể cập nhật hoặc xóa profile cũ.",style=MaterialTheme.typography.bodySmall,color=MirageMuted)
        if(profiles.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        profiles.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        if(!profiles.loaded && !profiles.busy) TextButton(vm::reloadProfiles) { Text("Thử tải lại profile") }
    }
    if(choosing) AlertDialog(onDismissRequest={ if(!profiles.busy) choosing=false },title={ Text("Profile đã lưu") },text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Chọn để nạp cấu hình, sau đó nhấn Hiển thị trên LED để phát.",color=MirageMuted)
            profiles.entries.forEach { profile -> MirageCard(Modifier.fillMaxWidth()) {
                Text(profile.name,style=MaterialTheme.typography.titleMedium)
                Text(profile.text,maxLines=2)
                Text("${if(profile.size==2) "10×14" else "5×7"} · ${when(profile.mode) { "static"->"Đứng yên";"blink"->"Nhấp nháy";else->"Cuộn trái" }}",color=MirageMuted)
                Row { TextButton({ vm.selectProfile(profile.id);choosing=false },enabled=editable,modifier=Modifier.weight(1f)) { Text("Dùng ${profile.name}") }
                    TextButton({ deleting=profile },enabled=editable,modifier=Modifier.weight(1f)) { Text("Xóa ${profile.name}") } }
            } }
            if(profiles.entries.isEmpty()) Text("Chưa có profile chữ.")
            profiles.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        }
    },confirmButton={ TextButton({ choosing=false },enabled=!profiles.busy) { Text("Đóng") } })
    if(saving) AlertDialog(onDismissRequest={ if(!profiles.busy) saving=false },title={ Text(if(updating) "Cập nhật profile" else "Lưu profile mới") },text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name,{ name=it.take(40) },label={ Text("Tên profile") },singleLine=true,enabled=!profiles.busy)
            Text("Lưu nội dung, màu, cỡ chữ, hiệu ứng và tốc độ hiện tại.",color=MirageMuted)
            profiles.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        }
    },confirmButton={ TextButton({ vm.saveProfile(name,updating) { saving=false } },enabled=name.isNotBlank() && !profiles.busy) { Text("Lưu profile") } },dismissButton={ TextButton({ saving=false },enabled=!profiles.busy) { Text("Hủy") } })
    deleting?.let { profile -> AlertDialog(onDismissRequest={ if(!profiles.busy) deleting=null },title={ Text("Xóa profile?") },text={ Text("Xóa ${profile.name} khỏi các profile đã lưu trên điện thoại?") },
        confirmButton={ TextButton({ vm.deleteProfile(profile.id) { deleting=null } },enabled=!profiles.busy) { Text("Xác nhận xóa") } },dismissButton={ TextButton({ deleting=null },enabled=!profiles.busy) { Text("Hủy") } }) }
    if(upgrade) AlertDialog(onDismissRequest={ upgrade=false },title={ Text("Mở rộng profile chữ") },text={ Text("Gói mặc định: 3 profile. Gói mở rộng: 10 profile.\n\nGói mở rộng chưa được mở bán trong phiên bản này. Các profile hiện có vẫn được giữ lại khi thay đổi gói.") },confirmButton={ TextButton({ upgrade=false }) { Text("Đã hiểu") } })
}
