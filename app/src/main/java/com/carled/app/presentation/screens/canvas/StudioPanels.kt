package com.carled.app.presentation.screens.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.carled.app.domain.model.PixelDocument
import com.carled.app.presentation.components.MirageMuted
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.Ink

@Composable
fun CreateCanvasDialog(onDismiss: () -> Unit, onCreate: (Int, Int) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .background(Cream)
                .border(2.dp, Ink)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Cream)
                    .border(2.dp, Ink),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .padding(start = 10.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Green)
                        .border(1.5.dp, Ink, CircleShape)
                )
                Text(
                    "Create a canvas",
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp).weight(1f)
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(48.dp)
                        .background(Green)
                        .border(1.5.dp, Ink)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("X", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PixelDocument.sizes.forEach { (w, h) ->
                    val divisor = gcd(w, h)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Green)
                            .border(2.dp, Ink)
                            .clickable { onCreate(w, h) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Fixed aspect ratio box so rectangular configurations (e.g., 64x32) show up properly as rectangles
                        Box(
                            Modifier
                                .width(64.dp)
                                .aspectRatio(w.toFloat() / h)
                                .background(Cream)
                                .border(2.dp, Ink)
                        ) {
                            Text("$w", Modifier.align(Alignment.TopStart).padding(2.dp), fontSize = 10.sp, color = Ink)
                            Text("$h", Modifier.align(Alignment.BottomEnd).padding(2.dp), fontSize = 10.sp, color = Ink)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("$w×$h", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                            Text(
                                if (w == h) "Canvas vuông" else "Canvas chữ nhật ${w / divisor}:${h / divisor}",
                                color = Ink,
                                fontSize = 13.sp
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    "For the best display quality: Please choose the appropriate canvas size for your panel.",
                    color = MirageMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExportFormat) -> Unit
) {
    var format by remember { mutableStateOf(ExportFormat.PNG) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xuất ảnh ra Gallery") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Chọn định dạng tệp hình ảnh:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = format == ExportFormat.PNG, onClick = { format = ExportFormat.PNG })
                    Text("PNG (Chất lượng cao, hỗ trợ trong suốt)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = format == ExportFormat.JPEG, onClick = { format = ExportFormat.JPEG })
                    Text("JPEG (Dung lượng nhỏ hơn)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(format); onDismiss() }) {
                Text("Xuất ảnh")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun ImportImageDialog(
    bitmap: Bitmap,
    doc: PixelDocument,
    onImport: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    onSelectAnother: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nhập ảnh vào Canvas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ảnh sẽ được thu nhỏ và chuyển đổi thành điểm ảnh (rasterize) vừa khít với kích thước ${doc.width}×${doc.height}.")
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(bitmap) }) {
                Text("Đồng ý nhập")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSelectAnother) {
                    Text("Chọn ảnh khác")
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaletteDialog(
    state: StudioState,
    vm: StudioViewModel,
    onDismiss: () -> Unit
) {
    val presetColors = listOf(
        0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF23261F.toInt(), 0xFF7FC89E.toInt(),
        0xFF86D095.toInt(), 0xFF6D9A73.toInt(), 0xFFFF3366.toInt(), 0xFFFFA726.toInt(),
        0xFFFFEB3B.toInt(), 0xFF00D2FF.toInt(), 0xFF0891B2.toInt(), 0xFF7928CA.toInt(),
        0xFFEC4899.toInt(), 0xFFD9D9D9.toInt(), 0xFFFFFDF7.toInt(), 0xFF181B2A.toInt()
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 400.dp)
                .background(Cream)
                .border(2.dp, Ink)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Cream)
                    .border(2.dp, Ink),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .padding(start = 10.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Green)
                        .border(1.5.dp, Ink, CircleShape)
                )
                Text(
                    "Chọn màu",
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(48.dp)
                        .background(Green)
                        .border(1.5.dp, Ink)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("X", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Chọn màu vẽ cho cọ:", fontSize = 14.sp, color = Ink)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presetColors.forEach { colorInt ->
                        val isSelected = state.color == colorInt
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(colorInt))
                                .border(
                                    width = if (isSelected) 3.dp else 1.5.dp,
                                    color = if (isSelected) Green else Ink,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    vm.color(colorInt)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (Color(colorInt).luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)