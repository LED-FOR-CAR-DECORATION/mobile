package com.carled.app.presentation.screens.main

import androidx.compose.foundation.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carled.app.data.local.PixelDraftStore
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.screens.scan.ScanViewModel

@Composable
fun MirageHomeScreen(control: ControlViewModel, scanner: ScanViewModel, user: User?,
    openScan: () -> Unit, openControl: () -> Unit, openCanvas: (String?) -> Unit,
    openMarquee: () -> Unit, openUpload: () -> Unit, logout: () -> Unit, openEffect: () -> Unit = {}, openCharacter: (String) -> Unit = {}, initialTab: Int = 0) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    var firmware by remember { mutableStateOf(false) }
    if (firmware) {
        com.carled.app.presentation.screens.firmware.FirmwareUpdateScreen(onClose={firmware=false})
        return
    }
    val context = LocalContext.current
    val renameScope = rememberCoroutineScope()
    var renaming by remember { mutableStateOf(false) }
    var renameError by remember { mutableStateOf<String?>(null) }
    val state by control.uiState.collectAsState()
    val device = state.connectionState as? ConnectionState.Connected
    val alias = device?.deviceName ?: "CarLED-Ambient"
    var draftName by remember { mutableStateOf(alias) }
    val titles = listOf("Thiết bị của tôi", "Nội dung LED", "Tài khoản")
    val icons = listOf(Icons.Default.Devices, Icons.Default.GridView, Icons.Default.PersonOutline)
    Scaffold(topBar = { MirageBar(titles[tab], actions = {
        if (tab == 0) IconButton(onClick = openScan) { Icon(Icons.Default.Add, "Thêm thiết bị") }
    }) }, bottomBar = {
        NavigationBar(containerColor = Color.White) {
            listOf("Thiết bị", "Nội dung", "Tài khoản").forEachIndexed { index, name ->
                NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(icons[index], name) }, label = { Text(name) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MirageSage, selectedIconColor = MirageInk))
            }
        }
    }, containerColor = MirageBackground) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(remember(tab) { ScrollState(0) }).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when (tab) {
                0 -> {
                    Text("MIRAGE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Không gian của bạn.\nSắc màu của bạn.", style = MaterialTheme.typography.headlineMedium)
                    if (device != null) MirageCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(alias, style = MaterialTheme.typography.titleLarge); Text("● Đã kết nối Bluetooth", color = MaterialTheme.colorScheme.primary) }
                            Switch(state.ledState.isPowerOn, { control.togglePower() })
                        }
                        PixelPreview(modifier = Modifier.fillMaxWidth().aspectRatio(2f))
                        Text("Xem trước minh họa · ${state.ledState.currentMode.displayName}", style = MaterialTheme.typography.bodySmall, color = MirageMuted)
                        MirageButton("Điều khiển LED", openControl, Modifier.fillMaxWidth())
                    } else MirageCard {
                        PixelPreview(modifier = Modifier.fillMaxWidth().aspectRatio(2f))
                        Text("${if (state.connectionState is ConnectionState.Connecting) "Đang kết nối…" else "Chưa có thiết bị kết nối"}", style = MaterialTheme.typography.titleLarge)
                        Text("Bật Bluetooth và thêm thiết bị LED để bắt đầu. Bạn vẫn có thể tạo bản vẽ ngoại tuyến.", color = MirageMuted)
                        MirageButton("Thêm thiết bị", openScan, Modifier.fillMaxWidth())
                    }
                    Text("Tạo theo cách của bạn", style = MaterialTheme.typography.titleLarge)
                    Column(Modifier.fillMaxWidth().testTag("creationLauncher"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CreationFunction("DIY pixel", Icons.Default.Edit, MirageSage.copy(alpha = .3f), { openCanvas(null) }, Modifier.weight(1f))
                            CreationFunction("Chạy chữ", Icons.Default.TextFields, Color(0xffffedb4), openMarquee, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CreationFunction("Nhập ảnh", Icons.Default.PhotoLibrary, Color(0xffdceefe), openUpload, Modifier.weight(1f))
                            CreationFunction("Effect", Icons.Default.AutoAwesome, Color(0xffe9ddfa), openEffect, Modifier.weight(1f))
                        }
                    }
                    state.feedbackMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                1 -> ContentLibrary(user?.id ?: "", openCanvas, openCharacter)
                2 -> {
                    MirageCard {
                        Icon(Icons.Default.PersonOutline, null, Modifier.size(48.dp))
                        Text(user?.fullName?.ifBlank { "Tài khoản Mirage" } ?: "Tài khoản Mirage", style = MaterialTheme.typography.headlineSmall)
                        Text(user?.email ?: "", color = MirageMuted)
                    }
                    MirageCard {
                        Text("Thiết bị đã kết nối", style = MaterialTheme.typography.titleMedium)
                        Text(if (device == null) "Chưa kết nối thiết bị" else alias)
                        if (device != null) {
                            TextButton(onClick = { draftName = alias; renameError = null; rename = true }) { Text("Đổi tên Bluetooth") }
                            TextButton(onClick = { confirmDisconnect = true }) { Text("Ngắt kết nối") }
                            TextButton(onClick = { firmware = true }) { Text("Cập nhật firmware") }
                        } else MirageButton("Kết nối thiết bị", openScan, Modifier.fillMaxWidth())
                    }
                    MirageButton("Hướng dẫn Bluetooth", { help = true }, Modifier.fillMaxWidth(), secondary = true)
                    MirageButton("Đăng xuất", { confirmLogout = true }, Modifier.fillMaxWidth(), secondary = true)
                }
            }
        }
    }
    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false }, title = { Text("Đăng xuất?") }, text = { Text("Bản vẽ đã lưu vẫn ở trên điện thoại này.") },
        confirmButton = { TextButton(onClick = { scanner.disconnectDevice(); logout() }) { Text("Đăng xuất") } }, dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Hủy") } })
    if (confirmDisconnect) AlertDialog(onDismissRequest = { confirmDisconnect = false }, title = { Text("Ngắt kết nối LED?") },
        confirmButton = { TextButton(onClick = { scanner.disconnectDevice(); confirmDisconnect = false }) { Text("Ngắt kết nối") } }, dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Hủy") } })
    if (rename) AlertDialog(onDismissRequest = { if (!renaming) rename = false }, title = { Text("Tên Bluetooth của LED") }, text = {
        Column {
            OutlinedTextField(draftName, { draftName = it; renameError = null }, enabled = !renaming, singleLine = true,
                label = { Text("Tên thiết bị") }, supportingText = { Text("Tối đa 29 byte UTF-8 · Lưu trực tiếp trên LED") })
            renameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !renaming && device != null && isValidDeviceName(draftName), onClick = {
        renaming = true
        renameScope.launch {
            try { control.renameDevice(draftName); rename = false }
            catch (e: CancellationException) { if (e is kotlinx.coroutines.TimeoutCancellationException) renameError = "LED chưa xác nhận. Kiểm tra kết nối và cập nhật firmware LED." else throw e }
            catch (e: Exception) { renameError = if (e is kotlinx.coroutines.TimeoutCancellationException) "LED chưa xác nhận. Kiểm tra kết nối và cập nhật firmware LED." else e.message ?: "Không đổi được tên" }
            finally { renaming = false }
        }
    }) { Text(if (renaming) "Đang lưu…" else "Lưu trên LED") } }, dismissButton = { TextButton(enabled = !renaming, onClick = { rename = false }) { Text("Hủy") } })
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text("Kết nối Bluetooth") }, text = { Text("Bật nguồn LED và Bluetooth trên điện thoại. Android 12 trở lên cần quyền Thiết bị ở gần; Android cũ hơn cần quyền Vị trí để quét BLE. Đặt điện thoại gần LED rồi chọn Thêm thiết bị. Điều khiển BLE không cần Internet.") }, confirmButton = { TextButton(onClick = { help = false }) { Text("Đã hiểu") } })
}

@Composable
private fun CreationFunction(label: String, icon: ImageVector, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 116.dp), shape = RoundedCornerShape(12.dp),
        color = Color.White, border = BorderStroke(1.dp, MirageInk)) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(48.dp).background(accent, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MirageInk)
            }
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}
