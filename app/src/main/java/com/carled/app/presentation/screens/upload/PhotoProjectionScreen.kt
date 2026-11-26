package com.carled.app.presentation.screens.upload

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carled.app.domain.model.CustomerOwnedCharacterResponse
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.Ink

private val ActionButtonHeight = 56.dp
private val ActionButtonModifier = Modifier
    .fillMaxWidth()
    .height(ActionButtonHeight)

@Composable
fun PhotoProjectionScreen(
    vm: UploadViewModel,
    back: () -> Unit,
    scan: () -> Unit,
    character: CustomerOwnedCharacterResponse? = null
) {
    val state by vm.uiState.collectAsState()
    val connection by vm.connection.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { vm.onImageSelected(context, it) } }

    LaunchedEffect(character?.characterId) {
        character?.let { vm.loadRemoteCharacter(context, it.previewGifUrl) }
    }

    BackHandler(state.isUploading) { }
    val editable = !state.isUploading && !state.isLoadingImage
    val isConnected = connection is ConnectionState.Connected
    val canProject = isConnected &&
            state.previewDocument != null &&
            editable &&
            !state.isProcessing

    val radioColors = RadioButtonDefaults.colors(
        selectedColor = Green,
        unselectedColor = Ink,
        disabledSelectedColor = Ink.copy(alpha = 0.4f),
        disabledUnselectedColor = Ink.copy(alpha = 0.4f)
    )
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = Ink,
        checkedTrackColor = Green,
        checkedBorderColor = Ink,
        uncheckedThumbColor = Ink,
        uncheckedTrackColor = Cream,
        uncheckedBorderColor = Ink
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MirageBackground)
            .padding(8.dp)
    ) {
        WindowFrame(
            title = character?.characterName ?: "Chiếu ảnh",
            modifier = Modifier.fillMaxSize(),
            onClose = if (!state.isUploading) back else null
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (character == null) "Ảnh từ điện thoại → LED" else "Inventory · Character đã mua",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )

                if (character == null) {
                    HardButton(
                        text = if (state.previewDocument == null) "Chọn ảnh từ điện thoại" else "Đổi ảnh",
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = editable,
                        filled = false,
                        shadowOffset = HardStyle.CardOffset,
                        modifier = ActionButtonModifier
                    )
                } else {
                    Text(
                        text = "ID: #${character.characterId} · ${character.frameCount} khung hình · ${character.frameDelayMs} ms/khung",
                        color = MirageMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!isConnected) {
                    HardButton(
                        text = "Kết nối LED",
                        onClick = scan,
                        enabled = !state.isUploading,
                        filled = false,
                        shadowOffset = HardStyle.CardOffset,
                        modifier = ActionButtonModifier
                    )
                }

                HardButton(
                    text = if (state.isUploading) "Đang chiếu…" else "Chiếu lên LED",
                    onClick = vm::startUpload,
                    enabled = canProject,
                    filled = true,
                    shadowOffset = HardStyle.CardOffset,
                    modifier = ActionButtonModifier
                )

                if (state.isUploading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Green,
                        trackColor = Cream
                    )
                }

                state.feedbackMessage?.let {
                    Text(
                        text = it,
                        color = if (state.uploadStatus == UploadStatus.Error) MaterialTheme.colorScheme.error else Ink
                    )
                }
            }

            DeviceSection(title = "Xem trước LED · 64×32") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .flatSurface(color = Cream)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.previewDocument != null) {
                        PixelPreview(
                            document = state.previewDocument,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f)
                                .background(MirageInk)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.isLoadingImage) "Đang đọc ảnh…" else "Chọn ảnh để xem trước",
                                color = Color.White
                            )
                        }
                    }

                    if (state.isProcessing || state.isLoadingImage) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Green,
                            trackColor = Cream
                        )
                    }

                    state.imageNote?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MirageMuted)
                    }
                }
            }

            DeviceSection(title = "Khung hình") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .flatSurface(color = Cream)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FitMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = editable) { vm.setFitMode(mode) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.fitMode == mode,
                                onClick = { vm.setFitMode(mode) },
                                enabled = editable,
                                colors = radioColors
                            )
                            Text(mode.displayName, color = Ink)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .flatSurface(color = Cream)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dithering · Floyd–Steinberg", color = Ink, fontWeight = FontWeight.Bold)
                Switch(
                    checked = state.ditherMode != DitherMode.NONE,
                    onCheckedChange = { vm.setDitherMode(if (it) DitherMode.FLOYD_STEINBERG else DitherMode.NONE) },
                    enabled = editable,
                    colors = switchColors
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .flatSurface(color = Cream)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Độ sáng ảnh · ${state.brightnessPercent}%", color = Ink, fontWeight = FontWeight.Bold)
                HardSlider(
                    value = state.brightnessPercent.toFloat(),
                    onValueChange = { vm.setBrightness(it.toInt()) },
                    valueRange = 40f..220f,
                    enabled = editable && state.previewDocument != null
                )

                Text("Độ tương phản · ${state.contrastPercent}%", color = Ink, fontWeight = FontWeight.Bold)
                HardSlider(
                    value = state.contrastPercent.toFloat(),
                    onValueChange = { vm.setContrast(it.toInt()) },
                    valueRange = 40f..220f,
                    enabled = editable && state.previewDocument != null
                )

                Text(
                    text = "Các thanh này chỉnh ảnh xem trước; độ sáng thiết bị chỉnh trong Điều khiển LED.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MirageMuted
                )
            }
        }
    }
}
