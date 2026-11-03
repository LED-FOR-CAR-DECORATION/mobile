package com.carled.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.GreenDark
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink

private val WindowCornerRadius = 8.dp

@Composable
fun WindowFrame(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(WindowCornerRadius))
            .background(Cream)
            .border(1.5.dp, Ink, RoundedCornerShape(WindowCornerRadius))
    ) {
        WindowTitleBar(title, onClose)
        Divider()
        Column(
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (compact) 16.dp else 20.dp,
                    vertical = if (compact) 12.dp else 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            content = content
        )
        if (footer != null) {
            Divider()
            Column(Modifier.fillMaxWidth().background(GreenLight).padding(12.dp), content = footer)
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ink))
}

@Composable
private fun WindowTitleBar(title: String, onClose: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .padding(start = 8.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Green)
                .border(1.dp, Ink, CircleShape)
        )
        Text(
            title,
            color = Ink,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 6.dp).weight(1f)
        )
        WindowButton(Green, "Maximize", width = 52.dp, onClick = null) {
            Box(Modifier.size(width = 18.dp, height = 9.dp).border(1.5.dp, Cream))
        }
        WindowButton(GreenDark, "Close", width = 72.dp, onClick = onClose) { CloseGlyph() }
    }
}

@Composable
private fun WindowButton(
    color: Color,
    label: String,
    width: Dp,
    onClick: (() -> Unit)?,
    glyph: @Composable () -> Unit
) {
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(color)
            .border(0.75.dp, Ink)
            .clickable(enabled = onClick != null, role = Role.Button, onClickLabel = label) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) { glyph() }
}