package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.Ink

object HardStyle {
    val Border = 1.5.dp
    val CardOffset = 6.dp
    val SmallOffset = 4.dp
}

fun Modifier.hardSurface(
    color: Color = Green,
    offset: Dp = HardStyle.CardOffset
): Modifier = this
    .padding(end = offset, bottom = offset)   // reserves room, no manual padding in parents
    .background(color)
    .border(HardStyle.Border, Ink)
@Composable
fun HardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    filled: Boolean = true,
    textColor: Color = Ink,
    shadowOffset: Dp = 0.dp   // pass 0.dp for a flat button
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .hardSurface(color = if (filled) Green else Cream, offset = shadowOffset)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = textColor, strokeWidth = 2.dp)
        } else {
            Text(text, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
