package com.carled.app.presentation.screens.main

import androidx.compose.foundation.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.R
import com.carled.app.domain.model.*
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.Ink
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.screens.control.MirageControlScreen
import com.carled.app.presentation.screens.scan.ScanViewModel
import com.carled.app.presentation.screens.profile.MirageProfileScreen

const val HOME_TAB_HOME = 0
const val HOME_TAB_LIBRARY = 1
const val HOME_TAB_CONTROL = 2
const val HOME_TAB_ACCOUNT = 3

data class LetterProfile(val id: String, val name: String, val preview: String = "Abc")

private object MockLetterProfileStore {
    const val CAPACITY = 3
    fun listSaved(userId: String): List<LetterProfile> =
        if (userId.isBlank()) emptyList()
        else listOf(LetterProfile("p1", "Profile 1"), LetterProfile("p2", "Profile 2"))
}

@Composable
fun MirageHomeScreen(control: ControlViewModel, scanner: ScanViewModel, user: User?,
                     openScan: () -> Unit, openControl: () -> Unit, openCanvas: (String?) -> Unit,
                     openMarquee: () -> Unit, openUpload: () -> Unit, logout: () -> Unit,
                     openEffect: () -> Unit = {}, openCharacter: (String) -> Unit = {},
                     openEditProfile: () -> Unit = {}, openLetterProfile: (String) -> Unit = {},
                     initialTab: Int = HOME_TAB_HOME) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val renameScope = rememberCoroutineScope()
    var renaming by remember { mutableStateOf(false) }
    var renameError by remember { mutableStateOf<String?>(null) }
    val connectionState by control.connectionState.collectAsState()
    val feedbackMessage by control.feedbackMessage.collectAsState()
    val device = connectionState as? ConnectionState.Connected
    val alias = device?.deviceName ?: "CarLED-Ambient"
    var draftName by remember { mutableStateOf(alias) }
    val titles = listOf("Trang chủ", "Nội dung LED", "Điều khiển", "Tài khoản")
    Scaffold(topBar = {
        if (tab == HOME_TAB_LIBRARY) {
            MirageBar(titles[tab])
        }
    }, bottomBar = {
        HardNavBar(selectedIndex = tab, onTabSelected = { tab = it }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))    }, containerColor = MirageBackground) { padding ->
        when (tab) {
            HOME_TAB_HOME -> {
                val profiles = remember(user) { MockLetterProfileStore.listSaved(user?.id ?: "") }
                Box(Modifier.padding(padding).fillMaxSize().padding(8.dp)) {
                    WindowFrame(title = "Home", modifier = Modifier.fillMaxSize()) {
                        // Welcome back — reuses DeviceSection for the card shell and
                        // PersonGlyph for the avatar; everything else is plain Row/Column.
                        DeviceSection(title = "Welcome back, ${user?.id ?: "Username"}!") {
                            Row(
                                Modifier.fillMaxWidth().flatSurface(color = Cream).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(48.dp).clip(CircleShape).background(Green)
                                        .border(HardStyle.Border, Ink, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { PersonGlyph(size = 26.dp) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(user?.id ?: "Username", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                    if (!user?.email.isNullOrBlank()) {
                                        Text("Email: ${user?.email}", color = MirageMuted, fontSize = 13.sp)
                                    }
                                    Text(
                                        "Edit",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable(onClick = openEditProfile)
                                    )
                                }
                            }
                        }

                        // Quick start — two rows built from the same flatSurface + ChevronGlyph
                        // shell ConnectedPanelCard/FirmwareReleaseCard already use.
                        DeviceSection(title = "Quick start") {
                            Row(
                                Modifier.fillMaxWidth().flatSurface(color = Cream)
                                    .clickable(onClick = openUpload).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp).background(Ink, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Image(painterResource(R.drawable.ic_image_upload), contentDescription = null, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Project image from your personal collection.", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                ChevronGlyph()
                            }
                            Row(
                                Modifier.fillMaxWidth().flatSurface(color = Cream)
                                    .clickable(onClick = openScan).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp).background(Ink, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Image(painterResource(R.drawable.ic_qrcode), contentDescription = null, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Bind new device with account with QRcode.", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                ChevronGlyph()
                            }
                        }

                        // Saved letter profile — same row shell again; EmptyRow is reused as-is for the empty state.
                        DeviceSection(title = "Saved letter profile  ${profiles.size}/${MockLetterProfileStore.CAPACITY}") {
                            if (profiles.isEmpty()) {
                                EmptyRow("Chưa có bản lưu nào.")
                            } else {
                                profiles.forEach { profile ->
                                    Row(
                                        Modifier.fillMaxWidth().flatSurface(color = Cream)
                                            .clickable { openLetterProfile(profile.id) }.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.size(44.dp).background(Color.Black, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                            Text(profile.preview, color = Green, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(profile.name, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        ChevronGlyph()
                                    }
                                }
                            }
                        }

                        feedbackMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    }
                }
            }
            HOME_TAB_LIBRARY -> {
                Column(Modifier.padding(padding).fillMaxSize().verticalScroll(remember(tab) { ScrollState(0) }).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ContentLibrary(user?.id ?: "", openCanvas, openCharacter)
                }
            }
            HOME_TAB_CONTROL -> {
                Box(Modifier.padding(padding).fillMaxSize()) {
                    MirageControlScreen(
                        vm = control,
                        back = { tab = HOME_TAB_HOME },
                        scanner = openScan,
                        openPanelControl = { openControl() },
                        userId = user?.id ?: ""
                    )
                }
            }
            HOME_TAB_ACCOUNT -> {
                Box(Modifier.padding(padding).fillMaxSize()) {
                    MirageProfileScreen(
                        user = user,
                        control = control,
                        back = { tab = HOME_TAB_HOME },
                        onOpenControlTab = { tab = HOME_TAB_CONTROL },
                        onOpenPanelControl = { openControl() },
                        onOpenLibraryTab = { tab = HOME_TAB_LIBRARY },
                        onLogout = { scanner.disconnectDevice(); logout() }
                    )
                }
            }
        }
    }
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