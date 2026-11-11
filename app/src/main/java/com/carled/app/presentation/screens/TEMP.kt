package com.carled.app.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CreationButtonsGrid(
    openCanvas: (String?) -> Unit,
    openMarquee: () -> Unit,
    openUpload: () -> Unit,
    openEffect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("creationLauncher"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CreationFunction(
                label = "DIY pixel",
                icon = Icons.Default.Edit,
                accent = Color(0xFFC8D6AF),
                onClick = { openCanvas(null) },
                modifier = Modifier.weight(1f)
            )
            CreationFunction(
                label = "Chạy chữ",
                icon = Icons.Default.TextFields,
                accent = Color(0xffffedb4),
                onClick = openMarquee,
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CreationFunction(
                label = "Nhập ảnh",
                icon = Icons.Default.PhotoLibrary,
                accent = Color(0xffdceefe),
                onClick = openUpload,
                modifier = Modifier.weight(1f)
            )
            CreationFunction(
                label = "Effect",
                icon = Icons.Default.AutoAwesome,
                accent = Color(0xffe9ddfa),
                onClick = openEffect,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CreationFunction(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 116.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(accent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF1A1A1A)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}