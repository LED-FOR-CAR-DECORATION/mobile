package com.carled.app.presentation.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carled.app.core.common.Resource
import com.carled.app.data.local.PixelDraft
import com.carled.app.data.local.PixelDraftStore
import com.carled.app.domain.model.CustomerOwnedCharacterResponse
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.upload.DitherMode
import com.carled.app.presentation.screens.upload.FitMode
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink
import kotlin.math.ceil

private val LibraryCardBorder = 3.dp
private const val PageSize = 8

private enum class LibraryTab(val label: String) {
    GALLERY("Tự thiết kế"),
    PICTURES("Kho ảnh")
}

private enum class PictureFilter(val label: String) {
    OWNED_CHARACTERS("Kho ảnh web"),
    LOCAL_DRAFTS("Ảnh tự thiết kế đã lưu")
}

private sealed interface PictureDetailItem {
    data class OwnedCharacter(val character: CustomerOwnedCharacterResponse) : PictureDetailItem
    data class LocalDraft(val draft: PixelDraft) : PictureDetailItem
}

@Composable
fun ContentLibraryScreen(userId: String, openCanvas: (String?) -> Unit) {
    var tab by remember { mutableStateOf(LibraryTab.GALLERY) }
    val tabLabels = remember { LibraryTab.entries.map { it.label } }

    WindowFrame(
        title = "Thư viện nội dung",
        modifier = Modifier.fillMaxSize()
    ) {
        HardTabRow(
            tabs = tabLabels,
            selectedIndex = tab.ordinal,
            onSelectTab = { index -> tab = LibraryTab.entries[index] }
        )

        when (tab) {
            LibraryTab.GALLERY -> GalleryTab(openCanvas = openCanvas)
            LibraryTab.PICTURES -> PictureGridContent(userId = userId)
        }
    }
}

@Composable
private fun GalleryTab(
    openCanvas: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val draftsResult = remember { runCatching { PixelDraftStore(context).drafts() } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HardButton(
            text = "+ Tạo thiết kế",
            onClick = { openCanvas(null) },
            shadowOffset = HardStyle.CardOffset,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        DeviceSection(title = "Bản nháp đã lưu") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    draftsResult.isFailure -> {
                        EmptyRow("Không đọc được bản nháp.")
                    }
                    draftsResult.getOrDefault(emptyList()).isEmpty() -> {
                        EmptyRow("Chưa có thiết kế nào. Vẽ DIY rồi chọn Lưu nháp để thêm vào đây.")
                    }
                    else -> {
                        draftsResult.getOrDefault(emptyList()).forEach { draft ->
                            SavedDraftCard(
                                draft = draft,
                                onOpen = { openCanvas(draft.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedDraftCard(
    draft: PixelDraft,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeHeight = draft.document.height.coerceAtLeast(1).toFloat()
    val safeWidth = draft.document.width.coerceAtLeast(1).toFloat()
    val ratio = (safeWidth / safeHeight).coerceIn(0.5f, 3f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .flatSurface(color = Cream, borderWidth = LibraryCardBorder)
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .flatSurface(color = Cream, borderWidth = HardStyle.Border)
                .padding(6.dp)
        ) {
            PixelPreview(
                draft.document,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.name,
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${draft.document.width}×${draft.document.height} · Bản nháp DIY",
                    color = MirageMuted,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            HardButton(
                text = "Mở sửa",
                onClick = onOpen,
                modifier = Modifier.width(104.dp),
                filled = true,
                shadowOffset = 4.dp
            )
        }
    }
}

@Composable
private fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HardButton(
            text = "Trước",
            onClick = onPrevious,
            enabled = currentPage > 0,
            filled = false,
            shadowOffset = 4.dp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .flatSurface(color = Cream, borderWidth = LibraryCardBorder)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${currentPage + 1} / $totalPages",
                color = Ink,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
        HardButton(
            text = "Sau",
            onClick = onNext,
            enabled = currentPage < totalPages - 1,
            filled = true,
            shadowOffset = 4.dp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PictureGridContent(
    userId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ContentLibraryViewModel = viewModel(key = "contentLibrary:$userId")
    var filter by remember { mutableStateOf(PictureFilter.OWNED_CHARACTERS) }
    var page by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableStateOf<PictureDetailItem?>(null) }
    val ownedCharactersState by viewModel.ownedCharacters.collectAsState()

    LaunchedEffect(filter) { page = 0 }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PictureFilter.entries.forEach { option ->
                LibraryFilterChip(
                    label = option.label,
                    selected = filter == option,
                    onClick = { filter = option }
                )
            }
            if (filter == PictureFilter.OWNED_CHARACTERS) {
                Spacer(Modifier.weight(1f))
                val refreshing = ownedCharactersState is Resource.Loading
                Box(
                    Modifier
                        .size(40.dp)
                        .flatSurface(color = Cream, borderWidth = LibraryCardBorder)
                        .clickable(
                            enabled = !refreshing,
                            role = Role.Button,
                            onClickLabel = "Tải lại danh sách Character",
                            onClick = {
                                page = 0
                                viewModel.loadOwnedCharacters()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Tải lại danh sách Character",
                            tint = Ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        when (filter) {
            PictureFilter.OWNED_CHARACTERS -> {
                when (val s = ownedCharactersState) {
                    is Resource.Loading -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Ink)
                    }

                    is Resource.Error -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .flatSurface(color = Cream, borderWidth = LibraryCardBorder)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        MirageButton("Thử lại", viewModel::loadOwnedCharacters, Modifier.fillMaxWidth())
                    }

                    is Resource.Success -> {
                        if (s.data.isEmpty()) {
                            EmptyRow("Bạn chưa sở hữu character nào. Mua trong Store để xem tại đây.")
                        } else {
                            val totalPages = ceil(s.data.size.toFloat() / PageSize).toInt().coerceAtLeast(1)
                            val safePage = page.coerceIn(0, totalPages - 1)
                            val pageItems = remember(s.data, safePage) {
                                s.data.drop(safePage * PageSize).take(PageSize)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pageItems.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { character ->
                                            OwnedCharacterCell(
                                                character = character,
                                                onClick = {
                                                    selectedItem = PictureDetailItem.OwnedCharacter(character)
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }

                                PaginationControls(
                                    currentPage = safePage,
                                    totalPages = totalPages,
                                    onPrevious = { page = (safePage - 1).coerceAtLeast(0) },
                                    onNext = { page = (safePage + 1).coerceAtMost(totalPages - 1) }
                                )
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
                    EmptyRow("Chưa có bản vẽ nào được lưu. Vẽ DIY ở tab Tự thiết kế rồi lưu nháp.")
                } else {
                    val totalPages = ceil(drafts.size.toFloat() / PageSize).toInt().coerceAtLeast(1)
                    val safePage = page.coerceIn(0, totalPages - 1)
                    val pageItems = remember(drafts, safePage) {
                        drafts.drop(safePage * PageSize).take(PageSize)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pageItems.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { draft ->
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .flatSurface(color = Cream, borderWidth = LibraryCardBorder)
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
                                            color = Ink,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        PaginationControls(
                            currentPage = safePage,
                            totalPages = totalPages,
                            onPrevious = { page = (safePage - 1).coerceAtLeast(0) },
                            onNext = { page = (safePage + 1).coerceAtMost(totalPages - 1) }
                        )
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
private fun LibraryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .flatSurface(
                color = if (selected) GreenLight else Cream,
                borderWidth = LibraryCardBorder
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Ink,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelMedium
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
    val sliderColors = SliderDefaults.colors(
        thumbColor = Ink,
        activeTrackColor = Green,
        inactiveTrackColor = Cream,
        disabledThumbColor = Ink.copy(alpha = 0.4f),
        disabledActiveTrackColor = Green.copy(alpha = 0.4f),
        disabledInactiveTrackColor = Cream
    )

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        WindowFrame(
            title = when (item) {
                is PictureDetailItem.OwnedCharacter -> item.character.characterName
                is PictureDetailItem.LocalDraft -> item.draft.name
            },
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            onClose = if (!busy) onDismiss else null,
            compact = true,
            footer = {
                when (status) {
                    QuickProjectionStatus.Success -> MirageButton(
                        text = "Xong",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                    is QuickProjectionStatus.Error -> MirageButton(
                        text = "Thử lại",
                        onClick = onQuickProjection,
                        modifier = Modifier.fillMaxWidth()
                    )
                    else -> MirageButton(
                        text = if (status.isBusy) "Đang chiếu…" else "Quick projection",
                        onClick = onQuickProjection,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && editorState.previewDocument != null
                    )
                }
            }
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
                    CircularProgressIndicator(color = Ink)
                }
            }

            when (item) {
                is PictureDetailItem.OwnedCharacter -> {
                    val c = item.character
                    Text("ID: #${c.characterId}", color = MirageMuted, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Frames: ${editorState.frameCount.coerceAtLeast(1)} · Delay: ${editorState.frameDelayMs} ms",
                        color = Ink,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is PictureDetailItem.LocalDraft -> {
                    val d = item.draft
                    Text(
                        "Resolution: ${d.document.width}×${d.document.height} · Frames: ${editorState.frameCount.coerceAtLeast(1)}",
                        color = Ink,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text("Khung hình", color = Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            FitMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { onFitModeChange(mode) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = editorState.fitMode == mode,
                        onClick = { onFitModeChange(mode) },
                        enabled = !busy,
                        colors = radioColors
                    )
                    Text(mode.displayName, color = Ink)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dithering · Floyd–Steinberg", color = Ink)
                Switch(
                    checked = editorState.ditherMode != DitherMode.NONE,
                    onCheckedChange = onDitherChange,
                    enabled = !busy,
                    colors = switchColors
                )
            }

            Text("Độ sáng ảnh · ${editorState.brightnessPercent}%", color = Ink)
            HardSlider(
                value = editorState.brightnessPercent.toFloat(),
                onValueChange = { onBrightnessChange(it.toInt()) },
                valueRange = 40f..220f,
                enabled = !busy && editorState.previewDocument != null
            )

            Text("Độ tương phản · ${editorState.contrastPercent}%", color = Ink)
            HardSlider(
                value = editorState.contrastPercent.toFloat(),
                onValueChange = { onContrastChange(it.toInt()) },
                valueRange = 40f..220f,
                enabled = !busy && editorState.previewDocument != null
            )

            QuickProjectionStatusRow(status)
        }
    }
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
            CircularProgressIndicator(Modifier.size(16.dp), color = Ink, strokeWidth = 2.dp)
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
private fun OwnedCharacterCell(
    character: CustomerOwnedCharacterResponse,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .flatSurface(color = Cream, borderWidth = LibraryCardBorder)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RemoteThumbnail(character.previewGifUrl, character.characterName, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(
            character.characterName,
            color = Ink,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text("${character.frameCount} khung hình", color = MirageMuted, style = MaterialTheme.typography.labelSmall)
    }
}