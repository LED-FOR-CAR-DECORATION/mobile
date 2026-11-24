package com.carled.app.presentation.screens.main

import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.R
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.domain.model.User
import com.carled.app.domain.model.device.isValidDeviceName
import com.carled.app.presentation.components.ChevronGlyph
import com.carled.app.presentation.components.DeviceSection
import com.carled.app.presentation.components.EmptyRow
import com.carled.app.presentation.components.HardNavBar
import com.carled.app.presentation.components.MirageBackground
import com.carled.app.presentation.components.MirageBar
import com.carled.app.presentation.components.MirageMuted
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.components.flatSurface
import com.carled.app.presentation.components.MirageButton
import com.carled.app.presentation.screens.commission.CommissionScreen
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.screens.control.MirageControlScreen
import com.carled.app.presentation.screens.control.OwnedDevicesViewModel
import com.carled.app.presentation.screens.marquee.MarqueeViewModel
import com.carled.app.presentation.screens.marquee.TextProfilesState
import com.carled.app.presentation.screens.profile.MirageProfileScreen
import com.carled.app.presentation.screens.scan.ScanViewModel
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.Ink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val HOME_TAB_HOME = 0
const val HOME_TAB_LIBRARY = 1
const val HOME_TAB_CONTROL = 2
const val HOME_TAB_COMMISSION = 3
const val HOME_TAB_ACCOUNT = 4

// MockLetterProfileStore / LetterProfile removed — the Home tab now reads
// real, cache-backed profiles via MarqueeViewModel.profiles (the same
// TextProfileRepository-backed state the Marquee screen already uses),
// instead of two hardcoded fake entries that nothing could edit, delete,
// or send.

@Composable
fun MirageHomeScreen(
    control: ControlViewModel, scanner: ScanViewModel, ownedDevices: OwnedDevicesViewModel, user: User?,
    marquee: MarqueeViewModel?, // NEW — null only before an account id is available
    openScan: () -> Unit, openControl: () -> Unit, openCanvas: (String?) -> Unit,
    openMarquee: () -> Unit, openUpload: () -> Unit, logout: () -> Unit,
    openEffect: () -> Unit = {}, openCharacter: (String) -> Unit = {},
    openEditProfile: () -> Unit = {}, // kept — this is the USER/account "Edit" link below, unrelated to letter profiles
    openClaimDevice: () -> Unit = {},
    openDeviceInfo: (String) -> Unit = {},
    initialTab: Int = HOME_TAB_HOME
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    BackHandler(enabled = tab == HOME_TAB_COMMISSION) { tab = HOME_TAB_HOME }
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
    val titles = listOf("Trang chủ", "Nội dung LED", "Điều khiển", "Commission", "Tài khoản")

    val profilesFlow = remember(marquee) { marquee?.profiles ?: MutableStateFlow(TextProfilesState()).asStateFlow() }
    val profilesState by profilesFlow.collectAsState()

    Scaffold(topBar = {
        if (tab == HOME_TAB_LIBRARY) {
            MirageBar(titles[tab])
        }
    }, bottomBar = {
        HardNavBar(
            selectedIndex = tab,
            onTabSelected = { tab = it },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }, containerColor = MirageBackground) { padding ->
        when (tab) {
            HOME_TAB_HOME -> {
                Box(Modifier.padding(padding).fillMaxSize().padding(8.dp)) {
                    WindowFrame(title = "Home", modifier = Modifier.fillMaxSize()) {
                        // Welcome back — ic_user remains 24.dp as requested
                        DeviceSection(title = "Welcome back, ${user?.fullName ?: "New User"}!") {
                            Row(
                                Modifier.fillMaxWidth().flatSurface(color = Cream).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(48.dp).clip(CircleShape).background(Green),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_user),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(user?.fullName ?: "New User", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
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

                        // Quick start — icons scaled up to 48.dp (2x bigger)
                        DeviceSection(title = "Quick start") {
                            Row(
                                Modifier.fillMaxWidth().flatSurface(color = Cream)
                                    .clickable(onClick = openUpload).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                    Image(painterResource(R.drawable.ic_image_upload), contentDescription = null, modifier = Modifier.size(48.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Project image from your personal collection.", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                ChevronGlyph()
                            }
                            Row(
                                Modifier.fillMaxWidth().flatSurface(color = Cream)
                                    .clickable(onClick = openClaimDevice).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                    Image(painterResource(R.drawable.ic_qrcode), contentDescription = null, modifier = Modifier.size(48.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Bind new device with account with QRcode.", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                ChevronGlyph()
                            }
                            Row(
                                Modifier.fillMaxWidth().flatSurface(color = Cream)
                                    .clickable { tab = HOME_TAB_COMMISSION }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                    Image(painterResource(R.drawable.ic_qrcode), contentDescription = null, modifier = Modifier.size(48.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Commission a custom LED design from our team.", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                ChevronGlyph()
                            }
                        }

                        // Saved letter profile — now real, cache-backed data from
                        // MarqueeViewModel.profiles (TextProfileRepository), capped
                        // at profilesState.plan.limit (3, per TextProfilePlan.BASIC).
                        DeviceSection(title = "Saved letter profile  ${profilesState.entries.size}/${profilesState.plan.limit}") {
                            when {
                                marquee == null -> EmptyRow("Đăng nhập để xem profile đã lưu.")
                                profilesState.entries.isEmpty() -> EmptyRow("Chưa có bản lưu nào. Tạo profile mới bên dưới.")
                                else -> profilesState.entries.forEach { profile ->
                                    Row(
                                        Modifier.fillMaxWidth().flatSurface(color = Cream)
                                            .clickable {
                                                // Instant send: load this profile's text,
                                                // colors, speed and effect into state, then
                                                // send straight to the LED — no extra screen.
                                                marquee.selectProfile(profile.id)
                                                marquee.sendMarquee()
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_pixel_perfect_abc),
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(profile.name, color = Ink, fontSize = 14.sp)
                                            Text(profile.text, color = MirageMuted, fontSize = 12.sp, maxLines = 1)
                                        }
                                        // Edit opens the full Marquee editor. It does NOT yet
                                        // pre-load this specific profile there — openMarquee()
                                        // takes no profile id today. Flagged, not silently implied.
                                        TextButton(onClick = { openMarquee() }) { Text("Sửa") }
                                        TextButton(onClick = { marquee.deleteProfile(profile.id) }) { Text("Xóa") }
                                    }
                                }
                            }
                            if (marquee != null) {
                                MirageButton("+ Tạo profile mới", openMarquee, Modifier.fillMaxWidth())
                            }
                        }

                        feedbackMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    }
                }
            }
            HOME_TAB_LIBRARY -> {
                Box(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                    ContentLibraryScreen(user?.id ?: "", openCanvas, openCharacter)
                }
            }
            HOME_TAB_CONTROL -> {
                Box(Modifier.padding(padding).fillMaxSize()) {
                    MirageControlScreen(
                        vm = control,
                        ownedDevices = ownedDevices,
                        back = { tab = HOME_TAB_HOME },
                        scanner = openScan,
                        openClaimDevice = openClaimDevice,
                        openDeviceInfo = openDeviceInfo
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
            HOME_TAB_COMMISSION -> {
                user?.let { CommissionScreen(it, Modifier.padding(padding)) }
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