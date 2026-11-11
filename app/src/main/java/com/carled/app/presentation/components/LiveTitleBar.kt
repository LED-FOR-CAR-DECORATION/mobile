package com.carled.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.Ink

@Composable
private fun LiveEditorTitleBar(syncing: Boolean, onClose: () -> Unit) {
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
            "Live editor",
            color = Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp).weight(1f)
        )
        Icon(
            Icons.Default.Sync,
            contentDescription = if (syncing) "Đang đồng bộ" else null,
            tint = Green.copy(alpha = if (syncing) 1f else 0.4f),
            modifier = Modifier.padding(end = 10.dp).size(18.dp)
        )
        Box(
            Modifier
                .fillMaxHeight()
                .width(48.dp)
                .background(Green)
                .border(1.5.dp, Ink)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text("X", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}