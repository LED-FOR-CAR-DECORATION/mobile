package com.carled.app.presentation.screens.upload

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.carled.app.domain.model.ConnectionState
import com.carled.app.presentation.components.*

@Composable
fun PhotoProjectionScreen(vm: UploadViewModel, back: () -> Unit, scan: () -> Unit, character: com.carled.app.data.local.OwnedCharacter? = null) {
    val state by vm.uiState.collectAsState()
    val connection by vm.connection.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { vm.onImageSelected(context, it) } }
    LaunchedEffect(character?.characterId) { character?.let { vm.loadMockAsset(context, it.asset) } }
    BackHandler(state.isUploading) { }
    val editable = !state.isUploading && !state.isLoadingImage
    Scaffold(topBar = { MirageBar(character?.name ?: "Chiếu ảnh", { if (!state.isUploading) back() }) }, containerColor = MirageBackground) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (character == null) "Ảnh từ điện thoại → LED" else "Inventory · Character đã mua", style = MaterialTheme.typography.headlineSmall)
            if (character == null) MirageButton(if (state.previewDocument == null) "Chọn ảnh từ điện thoại" else "Đổi ảnh", { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.fillMaxWidth(), enabled = editable)
            else Text("Dữ liệu mẫu · ${character.entitlementId} · ${character.sourceType}", color = MirageMuted)
            MirageCard {
                Text("Xem trước LED · 64×32", style = MaterialTheme.typography.titleMedium)
                if (state.previewDocument != null) PixelPreview(state.previewDocument, Modifier.fillMaxWidth().aspectRatio(2f))
                else Box(Modifier.fillMaxWidth().aspectRatio(2f).background(MirageInk).padding(20.dp)) { Text(if (state.isLoadingImage) "Đang đọc ảnh…" else "Chọn ảnh để xem trước", color = androidx.compose.ui.graphics.Color.White) }
                if (state.isProcessing || state.isLoadingImage) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.imageNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MirageMuted) }
            }
            Text("Khung hình", style = MaterialTheme.typography.titleMedium)
            FitMode.entries.forEach { mode -> Row(Modifier.fillMaxWidth().clickable(enabled = editable) { vm.setFitMode(mode) }) { RadioButton(state.fitMode == mode, { vm.setFitMode(mode) }, enabled = editable); Text(mode.displayName, Modifier.padding(top = 12.dp)) } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Dithering · Floyd–Steinberg"); Switch(state.ditherMode != DitherMode.NONE, { vm.setDitherMode(if (it) DitherMode.FLOYD_STEINBERG else DitherMode.NONE) }, enabled = editable) }
            Text("Độ sáng ảnh · ${state.brightnessPercent}%")
            Slider(state.brightnessPercent.toFloat(), { vm.setBrightness(it.toInt()) }, valueRange = 40f..220f, enabled = editable && state.previewDocument != null)
            Text("Độ tương phản · ${state.contrastPercent}%")
            Slider(state.contrastPercent.toFloat(), { vm.setContrast(it.toInt()) }, valueRange = 40f..220f, enabled = editable && state.previewDocument != null)
            Text("Các thanh này chỉnh ảnh xem trước; độ sáng thiết bị chỉnh trong Điều khiển LED.", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
            if (connection !is ConnectionState.Connected) MirageButton("Kết nối LED", scan, Modifier.fillMaxWidth(), enabled = !state.isUploading, secondary = true)
            MirageButton(if (state.isUploading) "Đang chiếu…" else "Chiếu lên LED", vm::startUpload, Modifier.fillMaxWidth(), enabled = connection is ConnectionState.Connected && state.previewDocument != null && editable && !state.isProcessing)
            if (state.isUploading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.feedbackMessage?.let { Text(it, color = if (state.uploadStatus == UploadStatus.Error) MaterialTheme.colorScheme.error else MirageInk) }
        }
    }
}
