package com.carled.app.presentation.screens.main

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.carled.app.data.local.*
import com.carled.app.presentation.components.*

@Composable
fun ContentLibrary(userId: String, openCanvas: (String?) -> Unit, openCharacter: (String) -> Unit) {
    var inventory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    TabRow(if (inventory) 1 else 0, containerColor = MirageBackground) {
        Tab(!inventory, { inventory = false }, text = { Text("Gallery") })
        Tab(inventory, { inventory = true }, text = { Text("Inventory") })
    }
    if (!inventory) {
        Text("Bản DIY của bạn", style = MaterialTheme.typography.headlineSmall)
        Text("Bản vẽ và khối chữ lưu trên điện thoại này.", color = MirageMuted)
        MirageButton("+ Tạo thiết kế", { openCanvas(null) }, Modifier.fillMaxWidth())
        val result = remember { runCatching { PixelDraftStore(context).drafts() } }
        if (result.isFailure) Text("Không đọc được bản nháp.", color = MaterialTheme.colorScheme.error)
        else if (result.getOrDefault(emptyList()).isEmpty()) MirageCard { Text("Gallery chưa có thiết kế"); Text("Vẽ DIY rồi chọn Lưu nháp để thêm vào Gallery.") }
        result.getOrDefault(emptyList()).forEach { draft -> MirageCard(Modifier.fillMaxWidth().clickable { openCanvas(draft.id) }) {
            PixelPreview(draft.document, Modifier.fillMaxWidth().aspectRatio(draft.document.width.toFloat() / draft.document.height))
            Text(draft.name, style = MaterialTheme.typography.titleMedium)
            Text("${draft.document.width}×${draft.document.height} · Mở thiết kế", color = MirageMuted)
        } }
    } else {
        Text("Character đã mua", style = MaterialTheme.typography.headlineSmall)
        Text("Dữ liệu mẫu · Chưa đồng bộ giao dịch và quyền sở hữu từ BE.", color = MirageMuted)
        val owned = remember(userId) { MockCharacterInventory().listOwned(userId) }
        if (owned.isEmpty()) Text("Đăng nhập để xem Inventory.")
        owned.forEach { item -> MirageCard(Modifier.fillMaxWidth().clickable { openCharacter(item.characterId) }) {
            val image = remember(item.asset) { runCatching { context.assets.open(item.asset).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull() }
            image?.let { Image(it, item.name, Modifier.fillMaxWidth().height(140.dp)) }
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text("Đã mua (mẫu) · ${item.animationType}", color = MirageMuted)
            TextButton({ openCharacter(item.characterId) }) { Text("Xem và chiếu mẫu") }
        } }
    }
}
