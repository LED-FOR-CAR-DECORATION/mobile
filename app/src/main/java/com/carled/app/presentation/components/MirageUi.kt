package com.carled.app.presentation.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.carled.app.domain.model.PixelDocument
import androidx.compose.ui.platform.LocalContext
import com.carled.app.R

val MirageSage = Color(0xff89c89b)
val MirageInk = Color(0xff111111)
val MirageBackground = Color(0xfffdfdfd)
val MirageMuted = Color(0xff555555)

fun Modifier.miragePanel(color: Color = Color.White): Modifier = this
    .drawBehind {
        drawRoundRect(MirageInk, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
        // Translucent selected fills blend with the white card, not the black offset shadow.
        drawRoundRect(Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
    }
    .background(color, RoundedCornerShape(10.dp)).border(1.5.dp, MirageInk, RoundedCornerShape(10.dp))

@Composable
fun MirageCard(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.miragePanel(color).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun MirageButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, secondary: Boolean = false) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = RoundedCornerShape(8.dp), border = BorderStroke(1.5.dp, MirageInk),
        colors = ButtonDefaults.buttonColors(containerColor = if (secondary) Color.White else MirageSage, contentColor = MirageInk)) { Text(text) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirageBar(title: String, back: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = { if (back != null) IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } },
        actions = actions, colors = TopAppBarDefaults.topAppBarColors(containerColor = MirageBackground))
}

@Composable
fun PixelPreview(document: PixelDocument? = null, modifier: Modifier = Modifier, grid: Boolean = false) {
    val context = LocalContext.current
    val shown = document ?: remember(context) {
        // Exact first frame of IoT-Device/src/storage/DefaultCarPattern.h (little-endian RGB565).
        val data = context.resources.openRawResource(R.raw.default_led_car).use { it.readBytes() }
        require(data.size == 64 * 32 * 2 * 2)
        PixelDocument(64, 32, List(64 * 32) { index ->
            val color = (data[index * 2].toInt() and 255) or ((data[index * 2 + 1].toInt() and 255) shl 8)
            val r = (color ushr 11) and 31; val g = (color ushr 5) and 63; val b = color and 31
            (0xff shl 24) or (((r shl 3) or (r ushr 2)) shl 16) or (((g shl 2) or (g ushr 4)) shl 8) or ((b shl 3) or (b ushr 2))
        })
    }
    val pixels = remember(shown) { shown.composite() }
    Canvas(modifier.background(Color(0xff101913), RoundedCornerShape(8.dp)).padding(12.dp)) {
        val cell = minOf(size.width / shown.width, size.height / shown.height)
        val ox = (size.width - shown.width * cell) / 2; val oy = (size.height - shown.height * cell) / 2
        for (y in 0 until shown.height) for (x in 0 until shown.width) {
            val p = pixels[y * shown.width + x]
            drawRect(if (p == 0) Color(0xff1a2920) else Color(p), Offset(ox + x * cell, oy + y * cell), Size(cell, cell))
            if (grid) drawRect(Color(0xff39463c), Offset(ox + x * cell, oy + y * cell), Size(cell, cell), style = Stroke(.5f))
        }
    }
}
