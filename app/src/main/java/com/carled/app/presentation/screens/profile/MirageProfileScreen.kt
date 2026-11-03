package com.carled.app.presentation.screens.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.carled.app.domain.model.User
import com.carled.app.presentation.components.MirageButton
import com.carled.app.presentation.components.MirageCard
import com.carled.app.presentation.components.MirageMuted

@Composable
fun MirageProfileScreen(user: User?, onLogout: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }

    MirageCard {
        Icon(Icons.Default.PersonOutline, null, Modifier.size(48.dp))
        Text(user?.fullName?.ifBlank { "Tài khoản Mirage" } ?: "Tài khoản Mirage", style = MaterialTheme.typography.headlineSmall)
        Text(user?.email ?: "", color = MirageMuted)
    }
    MirageButton("Đăng xuất", { confirmLogout = true }, Modifier.fillMaxWidth(), secondary = true)

    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("Đăng xuất?") },
        text = { Text("Bản vẽ đã lưu vẫn ở trên điện thoại này.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Đăng xuất") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Hủy") } }
    )
}