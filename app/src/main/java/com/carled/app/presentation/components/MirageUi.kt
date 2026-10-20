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
    val pixels = remember(document) { document?.composite() }
    Canvas(modifier.background(Color(0xff101913), RoundedCornerShape(8.dp)).padding(12.dp)) {
        if (document != null && pixels != null) {
            val cell = minOf(size.width / document.width, size.height / document.height)
            val ox = (size.width - document.width * cell) / 2; val oy = (size.height - document.height * cell) / 2
            for (y in 0 until document.height) for (x in 0 until document.width) {
                val p = pixels[y * document.width + x]
                drawRect(if (p == 0) Color(0xff1a2920) else Color(p), Offset(ox + x * cell, oy + y * cell), Size(cell, cell))
                if (grid) drawRect(Color(0xff39463c), Offset(ox + x * cell, oy + y * cell), Size(cell, cell), style = Stroke(.5f))
            }
        } else {
            val pattern = listOf("11000000011", "11100000111", "11111111111", "11011111011", "11111111111", "01101110110", "00111111100", "00011111000")
            val unit = minOf(size.width / 15, size.height / 12)
            pattern.forEachIndexed { y, row -> row.forEachIndexed { x, c -> if (c == '1')
                drawRect(MirageSage, Offset((size.width - 11 * unit) / 2 + x * unit, (size.height - 8 * unit) / 2 + y * unit), Size(unit - 1, unit - 1)) } }
        }
    }
}
