package com.carled.app.presentation.screens.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carled.app.R
import com.carled.app.domain.model.PixelShape
import com.carled.app.domain.model.PixelTool
import com.carled.app.presentation.components.MirageBackground
import com.carled.app.presentation.components.MirageButton
import com.carled.app.presentation.components.MirageCard
import com.carled.app.presentation.components.MirageInk
import com.carled.app.presentation.components.MirageSage
import com.carled.app.presentation.theme.FieldGrey
import kotlin.math.roundToInt


@Composable
fun CanvasSidebar(
    state: StudioState,
    vm: StudioViewModel,
    onMenu: () -> Unit,
    onOptions: () -> Unit,
    onColor: () -> Unit,
    onText: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sideButtonSize = 52.dp
    val sideIconSize = 40.dp

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(56.dp)
            .background(FieldGrey)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SidebarIconButton(
            icon = Icons.Default.Menu,
            label = "Menu",
            onClick = onMenu,
            testTag = "canvasMenu"
        )
        HorizontalDivider(Modifier.width(36.dp), color = MirageInk.copy(alpha = .15f))

        SidebarAssetButton(
            R.drawable.ic_eyedropper,
            "Hút màu",
            state.tool == PixelTool.PICKER,
            size = sideButtonSize,
            iconSize = sideIconSize
        ) { vm.tool(PixelTool.PICKER) }
        SidebarAssetButton(
            R.drawable.ic_pixel_perfect_abc,
            "Chèn chữ",
            state.tool == PixelTool.TEXT,
            size = sideButtonSize,
            iconSize = sideIconSize
        ) { vm.tool(PixelTool.TEXT); onText() }
        SidebarAssetButton(
            R.drawable.ic_switch_pen_eraser,
            if (state.tool == PixelTool.ERASER) "Tẩy" else "Bút",
            state.tool == PixelTool.PEN || state.tool == PixelTool.ERASER,
            size = sideButtonSize,
            iconSize = sideIconSize
        ) { vm.togglePenEraser() }
        SidebarAssetButton(
            R.drawable.ic_move,
            "Di chuyển",
            state.tool == PixelTool.MOVE,
            enabled = true,
            size = sideButtonSize,
            iconSize = sideIconSize
        ) { vm.tool(PixelTool.MOVE) }
        SidebarAssetButton(
            R.drawable.ic_paint_bucket,
            "Đổ màu",
            state.tool == PixelTool.FILL,
            size = sideButtonSize,
            iconSize = sideIconSize
        ) { vm.tool(PixelTool.FILL) }
        SidebarAssetButton(
            R.drawable.ic_line,
            "Đường thẳng",
            state.tool == PixelTool.SHAPE && state.shape == PixelShape.LINE,
            size = sideButtonSize,
            iconSize = sideIconSize
        ) { vm.shape(PixelShape.LINE, false) }
        SidebarAssetButton(
            R.drawable.ic_square,
            "Hình chữ nhật / Elip",
            state.tool == PixelTool.SHAPE && state.shape in listOf(
                PixelShape.RECTANGLE,
                PixelShape.ELLIPSE
            ),
            size = sideButtonSize,
            iconSize = sideIconSize
        ) { vm.cycleRectShape() }
        SidebarAssetButton(
            R.drawable.ic_star,
            "Ngôi sao",
            state.tool == PixelTool.SHAPE && state.shape == PixelShape.STAR,
            size = sideButtonSize,
            iconSize = sideIconSize
        ) { vm.shape(PixelShape.STAR, true) }

        HorizontalDivider(Modifier.width(36.dp), color = MirageInk.copy(alpha = .15f))
        SizeSlider(state.brushSize, 1..12) { vm.brush(it) }

        HorizontalDivider(Modifier.width(36.dp), color = MirageInk.copy(alpha = .15f))
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(state.color))
                .border(1.dp, MirageInk, RoundedCornerShape(8.dp))
                .clickable(onClick = onColor)
                .testTag("colorSwatch")
        )

        HorizontalDivider(Modifier.width(36.dp), color = MirageInk.copy(alpha = .15f))
        SidebarAssetButton(
            R.drawable.ic_settings,
            "Tùy chọn canvas",
            false,
            size = sideButtonSize,
            iconSize = sideIconSize,
            onClick = onOptions
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun CanvasBottomBar(
    state: StudioState,
    vm: StudioViewModel,
    onImport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOpacity by remember { mutableStateOf(false) }
    val buttonSize = 56.dp
    val iconSize = 28.dp

    Column(modifier.fillMaxWidth()) {
        if (showOpacity) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(FieldGrey)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                OpacityPopover(state.opacity) { vm.opacity(it) }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FieldGrey)
                .border(width = 1.dp, color = MirageInk.copy(alpha = .15f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SidebarAssetButton(
                R.drawable.ic_undo,
                "Hoàn tác",
                false,
                enabled = state.canUndo,
                size = buttonSize,
                iconSize = iconSize
            ) { vm.undo() }
            Spacer(Modifier.width(10.dp))
            SidebarAssetButton(
                R.drawable.ic_redo,
                "Làm lại",
                false,
                enabled = state.canRedo,
                size = buttonSize,
                iconSize = iconSize
            ) { vm.redo() }
            Spacer(Modifier.width(10.dp))
            SidebarAssetButton(
                R.drawable.ic_opacity,
                "Độ đậm màu",
                showOpacity,
                size = 56.dp,
                iconSize = 56.dp
            ) { showOpacity = !showOpacity }
            Spacer(Modifier.width(10.dp))
            SidebarAssetButton(
                R.drawable.ic_select,
                "Chọn vùng",
                state.tool == PixelTool.SELECT,
                size = buttonSize,
                iconSize = iconSize
            ) { vm.tool(PixelTool.SELECT) }
            Spacer(Modifier.width(10.dp))
            SidebarAssetButton(
                R.drawable.ic_cut,
                "Cắt vùng chọn",
                false,
                enabled = state.selection != null,
                size = buttonSize,
                iconSize = iconSize
            ) { vm.cutSelection() }
            Spacer(Modifier.width(10.dp))
            SidebarAssetButton(
                R.drawable.ic_copy,
                "Sao chép (giữ để dán)",
                false,
                enabled = state.selection != null || state.hasClipboard,
                size = buttonSize,
                iconSize = iconSize
            ) { vm.copySelection() }

            Spacer(Modifier.width(24.dp))

            SidebarAssetButton(
                R.drawable.ic_image_upload,
                "Nhập ảnh",
                false,
                size = buttonSize,
                iconSize = iconSize,
                onClick = onImport
            )
            Spacer(Modifier.width(10.dp))
            SidebarAssetButton(
                R.drawable.ic_save,
                "Lưu bản nháp",
                false,
                enabled = !state.sending,
                size = buttonSize,
                iconSize = iconSize
            ) { vm.save() }
            Spacer(Modifier.width(10.dp))
            SidebarAssetButton(
                R.drawable.ic_gallery,
                "Về danh sách thiết kế",
                false,
                size = buttonSize,
                iconSize = iconSize,
                onClick = onBack
            )
        }
    }
}

@Composable
private fun SidebarIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    IconButton(onClick = onClick, modifier = Modifier
        .size(40.dp)
        .testTag(testTag)) {
        Icon(icon, contentDescription = label, tint = MirageInk)
    }
}

@Composable
private fun SidebarAssetButton(
    iconRes: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MirageSage else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MirageInk) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = Color.Unspecified,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun SizeSlider(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${value}px", style = MaterialTheme.typography.labelSmall, color = MirageInk)
        Box(Modifier
            .width(32.dp)
            .height(96.dp), contentAlignment = Alignment.Center) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first - 1).coerceAtLeast(0),
                modifier = Modifier
                    .graphicsLayer { rotationZ = -90f }
                    .width(96.dp)
                    .testTag("brushSizeSlider")
            )
        }
    }
}

@Composable
private fun OpacityPopover(value: Int, onChange: (Int) -> Unit) {
    MirageCard(Modifier.padding(horizontal = 4.dp)) {
        Text(
            "Độ đậm $value%",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 10f..100f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavMenuSheet(
    state: StudioState,
    onSendToLed: () -> Unit,
    onSaveDraft: () -> Unit,
    onExportToGallery: () -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    close: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = close) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Menu", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = close) { Icon(Icons.Default.Close, "Đóng") }
            }

            val doc = state.document
            val isValidSize = doc != null && (
                    (doc.width == 32 && doc.height == 32) ||
                            (doc.width == 64 && doc.height == 32) ||
                            (doc.width == 64 && doc.height == 64)
                    )

            MirageButton(
                text = if (state.sending) "Đang lưu lên LED…" else "Lưu lên thiết bị LED",
                onClick = { close(); onSendToLed() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saveToLedMenu"),
                enabled = state.connected && !state.sending && isValidSize
            )

            MirageButton(
                text = "Lưu bản nháp",
                onClick = { close(); onSaveDraft() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saveDraftMenu"),
                enabled = !state.sending,
                secondary = true
            )

            MirageButton(
                text = "Xuất ảnh ra Gallery (PNG/JPEG)",
                onClick = { close(); onExportToGallery() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exportToGalleryMenu"),
                enabled = !state.sending,
                secondary = true
            )

            MirageButton(
                text = "Về danh sách thiết kế",
                onClick = { close(); onHome() },
                modifier = Modifier.fillMaxWidth(),
                secondary = true
            )

            MirageButton(
                text = "Hồ sơ",
                onClick = { close(); onProfile() },
                modifier = Modifier.fillMaxWidth(),
                secondary = true
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}