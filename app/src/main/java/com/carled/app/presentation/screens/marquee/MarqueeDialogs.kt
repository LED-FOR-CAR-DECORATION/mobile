package com.carled.app.presentation.screens.marquee

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carled.app.domain.model.RgbColor
import com.carled.app.domain.model.TextProfile
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.canvas.ColorEditor
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink

internal data class CanvasSizeOption(val width: Int, val height: Int, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarqueeMenuSheet(
    isSending: Boolean,
    canSend: Boolean,
    connected: Boolean,
    enabled: Boolean,
    menuAnchor: MenuAnchor,
    onToggleMenuAnchor: () -> Unit,
    onOpenDeviceInfo: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onOpenSize: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Menu chạy chữ", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Ink)
                }
            }
            HardButton(
                text = if (connected) "Kết nối LED (Đã kết nối)" else "Kết nối LED",
                onClick = { onDismiss(); onOpenDeviceInfo() },
                enabled = enabled,
                shadowOffset = 3.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("connectLedButton")
            )
            HardButton(
                text = if (menuAnchor == MenuAnchor.TOP) "Chuyển menu xuống dưới" else "Chuyển menu lên trên",
                onClick = { onToggleMenuAnchor(); onDismiss() },
                filled = false,
                shadowOffset = 3.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
            HardButton(
                text = if (isSending) "Đang gửi…" else "Hiển thị trên LED",
                onClick = { onDismiss(); onSend() },
                enabled = canSend,
                shadowOffset = 3.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
            HardButton(
                text = "Dừng chạy chữ (Màu tĩnh)",
                onClick = { onDismiss(); onStop() },
                enabled = connected && enabled,
                filled = false,
                shadowOffset = 3.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
            HardButton(
                text = "Kích thước canvas & tỷ lệ",
                onClick = { onDismiss(); onOpenSize() },
                enabled = enabled,
                filled = false,
                shadowOffset = 3.dp,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
internal fun CanvasSizeDialog(
    options: List<CanvasSizeOption>,
    selectedCanvas: CanvasSizeOption,
    customRatioText: String?,
    onDismiss: () -> Unit,
    onSelectCanvas: (CanvasSizeOption) -> Unit,
    onApplyRatio: (Int, Int) -> Unit
) {
    var ratioW by remember { mutableStateOf("64") }
    var ratioH by remember { mutableStateOf("32") }
    val w = ratioW.toIntOrNull()
    val h = ratioH.toIntOrNull()
    val valid = w != null && h != null && w > 0 && h > 0

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WindowFrame(
            title = "Kích thước canvas",
            onClose = onDismiss,
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Cream)
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { opt ->
                    val isSel = customRatioText == null && opt == selectedCanvas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSel) Modifier.hardSurface(color = GreenLight, offset = 3.dp, borderWidth = 2.dp)
                                else Modifier.flatSurface(color = Color.White, borderWidth = 1.5.dp)
                            )
                            .clickable { onSelectCanvas(opt) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(opt.label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = if (opt.width == opt.height) "Vuông" else "Chữ nhật",
                            color = MirageMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                Text("Tỷ lệ khung xem trước tùy ý:", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = ratioW,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) ratioW = it },
                        label = { Text("W") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedTextField(
                        value = ratioH,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) ratioH = it },
                        label = { Text("H") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                HardButton(
                    text = "Áp dụng tỷ lệ",
                    onClick = { if (valid) onApplyRatio(w!!, h!!) },
                    enabled = valid,
                    shadowOffset = 3.dp,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                )
            }
        }
    }
}

@Composable
internal fun MarqueeColorWheelDialog(
    r: Int,
    g: Int,
    b: Int,
    rainbow: Boolean,
    onToggleRainbow: () -> Unit,
    onSelectColor: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var draftColor by remember { mutableIntStateOf(AndroidColor.rgb(r, g, b)) }
    var editorTab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WindowFrame(
            title = "Màu sắc & bảng màu",
            onClose = onDismiss,
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Cream)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ColorPresetRow(
                    selectedColor = RgbColor(
                        red = AndroidColor.red(draftColor),
                        green = AndroidColor.green(draftColor),
                        blue = AndroidColor.blue(draftColor)
                    ),
                    onColorSelected = { rgb -> draftColor = AndroidColor.rgb(rgb.red, rgb.green, rgb.blue) }
                )
                HardTabRow(
                    tabs = listOf("Wheel", "RGB", "HSV"),
                    selectedIndex = editorTab,
                    onSelectTab = { editorTab = it }
                )
                ColorEditor(value = draftColor, onChange = { draftColor = it }, tab = editorTab)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hiệu ứng cầu vồng", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = rainbow, onCheckedChange = { onToggleRainbow() })
                }
                HardButton(
                    text = "Áp dụng màu",
                    onClick = {
                        onSelectColor(
                            AndroidColor.red(draftColor),
                            AndroidColor.green(draftColor),
                            AndroidColor.blue(draftColor)
                        )
                        onDismiss()
                    },
                    shadowOffset = 3.dp,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                )
            }
        }
    }
}

@Composable
internal fun MarqueeProfileDialogs(
    profiles: TextProfilesState,
    editable: Boolean,
    choosing: Boolean,
    saving: Boolean,
    updating: Boolean,
    dialogNameInput: String,
    deleting: TextProfile?,
    upgrade: Boolean,
    onDialogNameChange: (String) -> Unit,
    onDismissChoosing: () -> Unit,
    onSelectProfile: (TextProfile) -> Unit,
    onRequestDelete: (TextProfile) -> Unit,
    onDismissSaving: () -> Unit,
    onConfirmSave: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (TextProfile) -> Unit,
    onDismissUpgrade: () -> Unit
) {
    if (choosing) {
        AlertDialog(
            onDismissRequest = { if (!profiles.busy) onDismissChoosing() },
            title = { Text("Profile đã lưu", color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Chọn để nạp cấu hình, sau đó nhấn Hiển thị trên LED để phát.", color = MirageMuted)
                    profiles.entries.forEach { profile ->
                        Column(
                            modifier = Modifier.fillMaxWidth().flatSurface(color = Cream).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(profile.name, color = Ink, fontWeight = FontWeight.Bold)
                            Text(profile.text, maxLines = 2, color = Ink)
                            Text(
                                "${if (profile.size == 2) "10×14" else "${profile.size * 5}×${profile.size * 7}"} · ${
                                    when (profile.mode) {
                                        "static" -> "Đứng yên"
                                        "blink" -> "Nhấp nháy"
                                        else -> "Cuộn trái"
                                    }
                                }",
                                color = MirageMuted,
                                fontSize = 12.sp
                            )
                            Row {
                                TextButton(
                                    onClick = { onSelectProfile(profile) },
                                    enabled = editable,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Dùng ${profile.name}") }
                                TextButton(
                                    onClick = { onRequestDelete(profile) },
                                    enabled = editable,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Xóa ${profile.name}") }
                            }
                        }
                    }
                    if (profiles.entries.isEmpty()) Text("Chưa có profile chữ.")
                    profiles.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = onDismissChoosing, enabled = !profiles.busy) { Text("Đóng") } }
        )
    }

    if (saving) {
        AlertDialog(
            onDismissRequest = { if (!profiles.busy) onDismissSaving() },
            title = { Text(if (updating) "Cập nhật profile" else "Lưu profile mới", color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HardTextField(
                        value = dialogNameInput,
                        onValueChange = onDialogNameChange,
                        label = "Tên profile",
                        enabled = !profiles.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Lưu nội dung, màu, cỡ chữ, hiệu ứng và tốc độ hiện tại.", color = MirageMuted)
                    profiles.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmSave, enabled = dialogNameInput.isNotBlank() && !profiles.busy) {
                    Text("Lưu profile")
                }
            },
            dismissButton = { TextButton(onClick = onDismissSaving, enabled = !profiles.busy) { Text("Hủy") } }
        )
    }

    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { if (!profiles.busy) onDismissDelete() },
            title = { Text("Xóa profile?", color = Ink, fontWeight = FontWeight.Bold) },
            text = { Text("Xóa ${profile.name} khỏi các profile đã lưu trên điện thoại?") },
            confirmButton = {
                TextButton(onClick = { onConfirmDelete(profile) }, enabled = !profiles.busy) { Text("Xác nhận xóa") }
            },
            dismissButton = { TextButton(onClick = onDismissDelete, enabled = !profiles.busy) { Text("Hủy") } }
        )
    }

    if (upgrade) {
        AlertDialog(
            onDismissRequest = onDismissUpgrade,
            title = { Text("Mở rộng profile chữ", color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Text("Gói mặc định: 3 profile. Gói mở rộng: 10 profile.\n\nGói mở rộng chưa được mở bán trong phiên bản này. Các profile hiện có vẫn được giữ lại khi thay đổi gói.")
            },
            confirmButton = { TextButton(onClick = onDismissUpgrade) { Text("Đã hiểu") } }
        )
    }
}