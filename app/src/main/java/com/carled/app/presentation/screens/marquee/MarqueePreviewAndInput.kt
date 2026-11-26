package com.carled.app.presentation.screens.marquee

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.MarqueeLayout
import com.carled.app.domain.model.PixelDocument
import com.carled.app.domain.model.TextProfilePlan
import com.carled.app.presentation.components.HardButton
import com.carled.app.presentation.components.hardSurface
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.ErrorRed
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
internal fun MarqueePreviewSection(
    previewDoc: PixelDocument,
    previewAspect: Float,
    layout: MarqueeLayout,
    staticMode: Boolean,
    scrollMode: String,
    size: Int,
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF050706))
            .border(1.5.dp, Color(0xFF3A4134))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CrispMatrixCanvas(
            document = previewDoc,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(previewAspect)
                .testTag("marqueeLivePreview")
        )
        Text(
            text = if (staticMode) {
                if (!layout.fits) "Không đủ chỗ ở cỡ 5×7. Rút ngắn còn tối đa 40 ký tự hoặc chọn Cuộn trái."
                else "Tự vừa màn hình · ${layout.glyphLabel} · ${layout.lines.size} dòng · căn giữa${if (scrollMode == "blink") " · Nhấp nháy" else ""}"
            } else "Xem trước phần đầu · ${if (size == 2) "10×14" else "${size * 5}×${size * 7}"} · Chữ dài sẽ cuộn trên LED.",
            color = if (staticMode && !layout.fits) ErrorRed else Cream.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
        if (!connected) {
            Text(
                text = "Kết nối LED trước khi gửi.",
                color = Cream.copy(alpha = 0.65f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
internal fun MarqueeInputSection(
    text: String,
    size: Int,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var sizeInput by remember(size) { mutableStateOf(size.toString()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF050706))
            .border(1.5.dp, Color(0xFF3A4134))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp)) {
            if (text.isEmpty()) {
                Text(
                    text = "Input field",
                    color = Cream.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                textStyle = TextStyle(
                    color = Cream,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(GreenLight),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .testTag("marqueeTextInput")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Cỡ chữ (${MarqueeLayout.SCALE_RANGE.first}..${MarqueeLayout.SCALE_RANGE.last}):",
                color = Cream,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = sizeInput,
                onValueChange = { raw ->
                    if (raw.length <= 2 && raw.all(Char::isDigit)) {
                        sizeInput = raw
                        raw.toIntOrNull()?.let(onSizeChange)
                    }
                },
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    focusedBorderColor = GreenLight,
                    unfocusedBorderColor = Color(0xFF4A5446)
                ),
                modifier = Modifier.width(72.dp).testTag("letterSizeInput")
            )
            Text(
                text = "${size * 5}×${size * 7}",
                color = GreenLight,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = "Dùng ký tự tiếng Anh không dấu.",
            color = Cream.copy(alpha = 0.65f),
            fontSize = 12.sp
        )
    }
}

@Composable
internal fun MarqueeProfilesSection(
    profiles: TextProfilesState,
    editable: Boolean,
    contentValid: Boolean,
    onChoose: () -> Unit,
    onSaveNew: () -> Unit,
    onUpdateSelected: () -> Unit,
    onUpgrade: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = profiles.entries.firstOrNull { it.id == profiles.selectedId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("textProfiles")
            .background(Color(0xFF141812))
            .border(1.5.dp, Color(0xFF3A4134))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Profile chữ · ${profiles.entries.size}/${profiles.plan.limit}",
            color = Cream,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Gói ${profiles.plan.title} · Lưu trên điện thoại theo tài khoản",
            color = Cream.copy(alpha = 0.75f),
            fontSize = 12.sp
        )
        selected?.let {
            Box(
                modifier = Modifier
                    .hardSurface(color = GreenLight, offset = 2.dp, borderWidth = 1.5.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Đang dùng: ${it.name}",
                    color = Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HardButton(
                text = "Chọn profile",
                onClick = onChoose,
                enabled = editable && profiles.entries.isNotEmpty(),
                filled = false,
                shadowOffset = 2.dp,
                modifier = Modifier.weight(1f).height(42.dp)
            )
            HardButton(
                text = "Lưu mới",
                onClick = onSaveNew,
                enabled = editable && contentValid && profiles.entries.size < profiles.plan.limit,
                shadowOffset = 2.dp,
                modifier = Modifier.weight(1f).height(42.dp)
            )
        }
        if (selected != null) {
            HardButton(
                text = "Cập nhật profile",
                onClick = onUpdateSelected,
                enabled = editable && contentValid,
                filled = false,
                shadowOffset = 2.dp,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            )
        }
        if (profiles.plan == TextProfilePlan.BASIC) {
            TextButton(onClick = onUpgrade) {
                Text("Mở rộng lên 10 profile", color = GreenLight)
            }
        }
        if (profiles.entries.size >= profiles.plan.limit) {
            Text(
                text = "Đã đủ slot. Bạn vẫn có thể cập nhật hoặc xóa profile cũ.",
                color = Cream.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
        }
        if (profiles.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GreenLight)
        }
        profiles.error?.let {
            Text(it, color = ErrorRed, fontSize = 12.sp)
        }
        if (!profiles.loaded && !profiles.busy) {
            TextButton(onClick = onReload) {
                Text("Thử tải lại profile", color = GreenLight)
            }
        }
    }
}

@Composable
private fun CrispMatrixCanvas(
    document: PixelDocument,
    modifier: Modifier = Modifier
) {
    val pixels = remember(document) { document.composite() }
    val bitmap = remember(document.width, document.height) {
        Bitmap.createBitmap(document.width, document.height, Bitmap.Config.ARGB_8888)
    }
    remember(pixels, document.width, document.height) {
        val argb = IntArray(pixels.size) { idx ->
            val p = pixels[idx]
            if (p == 0 || (p ushr 24) == 0) 0xFF0A0E0C.toInt() else p
        }
        bitmap.setPixels(argb, 0, document.width, 0, 0, document.width, document.height)
        true
    }
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Canvas(modifier = modifier) {
        val aspect = document.width.toFloat() / max(1, document.height).toFloat()
        val drawW: Float
        val drawH: Float
        if (size.width / max(1f, size.height) > aspect) {
            drawH = size.height
            drawW = drawH * aspect
        } else {
            drawW = size.width
            drawH = drawW / aspect
        }
        val left = ((size.width - drawW) / 2f).roundToInt()
        val top = ((size.height - drawH) / 2f).roundToInt()

        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}