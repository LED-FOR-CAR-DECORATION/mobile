package com.carled.app.presentation.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carled.app.core.common.Resource
import com.carled.app.data.local.PixelDraft
import com.carled.app.data.local.PixelDraftStore
import com.carled.app.domain.model.CustomerOwnedCharacterResponse
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.upload.DitherMode
import com.carled.app.presentation.screens.upload.FitMode
import com.carled.app.presentation.theme.Cream

private enum class LibraryTab { GALLERY, PICTURES }
private enum class PictureFilter(val label: String) {
    OWNED_CHARACTERS("Owned characters"),
    LOCAL_DRAFTS("Saved pictures")
}

private sealed interface PictureDetailItem {
    data class OwnedCharacter(val character: CustomerOwnedCharacterResponse) : PictureDetailItem
    data class LocalDraft(val draft: PixelDraft) : PictureDetailItem
}

@Composable
fun ContentLibraryScreen(userId: String, openCanvas: (String?) -> Unit) {
    var tab by remember { mutableStateOf(LibraryTab.GALLERY) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(if (tab == LibraryTab.PICTURES) 1 else 0) {
            Tab(tab == LibraryTab.GALLERY, { tab = LibraryTab.GALLERY }, text = { Text("Gallery") })
            Tab(tab == LibraryTab.PICTURES, { tab = LibraryTab.PICTURES }, text = { Text("Pictures") })
        }

        Spacer(Modifier.height(12.dp))

        when (tab) {
            LibraryTab.GALLERY -> GalleryTab(openCanvas, Modifier.weight(1f))
            // Quick projection now sends the character's GIF straight to the LED
            // inline (ContentLibraryViewModel), so this tab no longer navigates
            // anywhere and takes no openCharacter callback.
            LibraryTab.PICTURES -> PictureGridTab(userId, openCanvas, Modifier.weight(1f))
        }
    }
}

@Composable
private fun GalleryTab(openCanvas: (String?) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bản DIY của bạn", style = MaterialTheme.typography.headlineSmall)
        Text("Bản vẽ và khối chữ lưu trên điện thoại này.", color = MirageMuted)
        MirageButton("+ Tạo thiết kế", { openCanvas(null) }, Modifier.fillMaxWidth())
        val result = remember { runCatching { PixelDraftStore(context).drafts() } }
        if (result.isFailure) {
            Text("Không đọc được bản nháp.", color = MaterialTheme.colorScheme.error)
        } else if (result.getOrDefault(emptyList()).isEmpty()) {
            MirageCard {
                Text("Gallery chưa có thiết kế")
                Text("Vẽ DIY rồi chọn Lưu nháp để thêm vào Gallery.")
            }
        } else {
            result.getOrDefault(emptyList()).forEach { draft ->
                MirageCard(Modifier.fillMaxWidth().clickable { openCanvas(draft.id) }) {
                    PixelPreview(
                        draft.document,
                        Modifier.fillMaxWidth().aspectRatio(draft.document.width.toFloat() / draft.document.height)
                    )
                    Text(draft.name, style = MaterialTheme.typography.titleMedium)
                    Text("${draft.document.width}×${draft.document.height} · Mở thiết kế", color = MirageMuted)
                }
            }
        }
    }
}

@Composable
private fun PictureGridTab(
    userId: String,
    openCanvas: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ContentLibraryViewModel = viewModel(key = "contentLibrary:$userId")
    var filter by remember { mutableStateOf(PictureFilter.OWNED_CHARACTERS) }
    var selectedItem by remember { mutableStateOf<PictureDetailItem?>(null) }
    val ownedCharactersState by viewModel.ownedCharacters.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PictureFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.label) }
                )
            }
            if (filter == PictureFilter.OWNED_CHARACTERS) {
                Spacer(Modifier.weight(1f))
                val refreshing = ownedCharactersState is Resource.Loading
                // Re-fetches the owned-character list from the backend so an
                // item added by a purchase elsewhere, while this screen was
                // already open, shows up without leaving and reopening the tab.
                IconButton(onClick = viewModel::loadOwnedCharacters, enabled = !refreshing) {
                    if (refreshing) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Tải lại danh sách Character"
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (filter) {
                PictureFilter.OWNED_CHARACTERS -> {
                    when (val s = ownedCharactersState) {
                        is Resource.Loading -> Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        is Resource.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            MirageButton("Thử lại", viewModel::loadOwnedCharacters)
                        }

                        is Resource.Success -> {
                            if (s.data.isEmpty()) {
                                EmptyRow("Bạn chưa sở hữu character nào. Mua trong Store để xem tại đây.")
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(s.data, key = { it.characterId }) { character ->
                                        OwnedCharacterCell(character) {
                                            selectedItem =
                                                PictureDetailItem.OwnedCharacter(character)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                PictureFilter.LOCAL_DRAFTS -> {
                    val drafts = remember {
                        runCatching { PixelDraftStore(context).drafts() }.getOrDefault(emptyList())
                    }
                    if (drafts.isEmpty()) {
                        EmptyRow("Chưa có bản vẽ nào được lưu. Vẽ DIY ở tab Gallery rồi lưu nháp.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(drafts, key = { it.id }) { draft ->
                                Column(
                                    Modifier
                                        .flatSurface(color = Cream)
                                        .clickable {
                                            selectedItem = PictureDetailItem.LocalDraft(draft)
                                        }
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    PixelPreview(
                                        draft.document,
                                        Modifier.fillMaxWidth().aspectRatio(1f)
                                    )
                                    Text(
                                        draft.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        val editorState by viewModel.editorState.collectAsState()
        LaunchedEffect(item) {
            when (item) {
                is PictureDetailItem.OwnedCharacter -> viewModel.prepareCharacter(item.character)
                is PictureDetailItem.LocalDraft -> viewModel.prepareDraft(item.draft)
            }
        }
        PictureDetailModal(
            item = item,
            editorState = editorState,
            onFitModeChange = viewModel::setFitMode,
            onDitherChange = { enabled ->
                viewModel.setDitherMode(if (enabled) DitherMode.FLOYD_STEINBERG else DitherMode.NONE)
            },
            onBrightnessChange = viewModel::setBrightness,
            onContrastChange = viewModel::setContrast,
            onDismiss = {
                selectedItem = null
                viewModel.resetQuickProjection()
            },
            onQuickProjection = {
                when (item) {
                    is PictureDetailItem.OwnedCharacter -> viewModel.quickProjectCharacter(item.character)
                    is PictureDetailItem.LocalDraft -> viewModel.quickProjectDraft(item.draft)
                }
            }
        )
    }
}

@Composable
private fun PictureDetailModal(
    item: PictureDetailItem,
    editorState: LibraryProjectionEditorState,
    onFitModeChange: (FitMode) -> Unit,
    onDitherChange: (Boolean) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onContrastChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onQuickProjection: () -> Unit
) {
    val status = editorState.status
    val busy = status.isBusy || editorState.isPreparing

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Text(
                text = when (item) {
                    is PictureDetailItem.OwnedCharacter -> item.character.characterName
                    is PictureDetailItem.LocalDraft -> item.draft.name
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editorState.previewDocument != null) {
                    PixelPreview(
                        document = editorState.previewDocument,
                        modifier = Modifier.fillMaxWidth().aspectRatio(2f)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(2f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                when (item) {
                    is PictureDetailItem.OwnedCharacter -> {
                        val c = item.character
                        Text("ID: #${c.characterId}", color = MirageMuted, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Frames: ${editorState.frameCount.coerceAtLeast(1)} · Delay: ${editorState.frameDelayMs} ms",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is PictureDetailItem.LocalDraft -> {
                        val d = item.draft
                        Text(
                            "Resolution: ${d.document.width}×${d.document.height} · Frames: ${editorState.frameCount.coerceAtLeast(1)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text("Khung hình", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FitMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { onFitModeChange(mode) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = editorState.fitMode == mode,
                            onClick = { onFitModeChange(mode) },
                            enabled = !busy
                        )
                        Text(mode.displayName)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dithering · Floyd–Steinberg")
                    Switch(
                        checked = editorState.ditherMode != DitherMode.NONE,
                        onCheckedChange = onDitherChange,
                        enabled = !busy
                    )
                }

                Text("Độ sáng ảnh · ${editorState.brightnessPercent}%")
                Slider(
                    value = editorState.brightnessPercent.toFloat(),
                    onValueChange = { onBrightnessChange(it.toInt()) },
                    valueRange = 40f..220f,
                    enabled = !busy && editorState.previewDocument != null
                )

                Text("Độ tương phản · ${editorState.contrastPercent}%")
                Slider(
                    value = editorState.contrastPercent.toFloat(),
                    onValueChange = { onContrastChange(it.toInt()) },
                    valueRange = 40f..220f,
                    enabled = !busy && editorState.previewDocument != null
                )

                QuickProjectionStatusRow(status)
            }
        },
        confirmButton = {
            when (status) {
                QuickProjectionStatus.Success -> MirageButton("Xong", onDismiss, Modifier.fillMaxWidth())
                is QuickProjectionStatus.Error -> MirageButton("Thử lại", onQuickProjection, Modifier.fillMaxWidth())
                else -> MirageButton(
                    text = if (status.isBusy) "Đang chiếu…" else "Quick projection",
                    onClick = onQuickProjection,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && editorState.previewDocument != null
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun QuickProjectionStatusRow(status: QuickProjectionStatus) {
    val label = when (status) {
        QuickProjectionStatus.Idle -> null
        QuickProjectionStatus.Connecting -> "Đang kiểm tra kết nối LED…"
        QuickProjectionStatus.Downloading -> "Đang tải ảnh động…"
        QuickProjectionStatus.Decoding -> "Đang xử lý khung hình…"
        QuickProjectionStatus.Sending -> "Đang gửi lên LED…"
        QuickProjectionStatus.WaitingAck -> "Đang chờ LED xác nhận…"
        QuickProjectionStatus.Success -> "LED đã xác nhận chiếu thành công!"
        is QuickProjectionStatus.Error -> status.message
    } ?: return

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status.isBusy) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        }
        Text(
            label,
            color = when (status) {
                is QuickProjectionStatus.Error -> MaterialTheme.colorScheme.error
                QuickProjectionStatus.Success -> MaterialTheme.colorScheme.primary
                else -> MirageMuted
            }
        )
    }
}

@Composable
private fun OwnedCharacterCell(character: CustomerOwnedCharacterResponse, onClick: () -> Unit) {
    Column(
        Modifier.flatSurface(color = Cream).clickable(onClick = onClick).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RemoteThumbnail(character.previewGifUrl, character.characterName, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(character.characterName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("${character.frameCount} khung hình", color = MirageMuted, style = MaterialTheme.typography.labelSmall)
    }
}