package com.carled.app.presentation.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.R
import com.carled.app.core.di.ServiceLocator
import com.carled.app.domain.model.User
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.domain.model.device.isValidDeviceName
import com.carled.app.presentation.components.ChevronGlyph
import com.carled.app.presentation.components.DeviceSection
import com.carled.app.presentation.components.EmptyRow
import com.carled.app.presentation.components.HardButton
import com.carled.app.presentation.components.HardNavBar
import com.carled.app.presentation.components.HardStyle
import com.carled.app.presentation.components.MirageBackground
import com.carled.app.presentation.components.MirageMuted
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.components.flatSurface
import com.carled.app.presentation.components.hardSurface
import com.carled.app.presentation.screens.commission.CommissionScreen
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.screens.control.MirageControlScreen
import com.carled.app.presentation.screens.control.OwnedDevicesViewModel
import com.carled.app.presentation.screens.marquee.MarqueeViewModel
import com.carled.app.presentation.screens.marquee.TextProfilesState
import com.carled.app.presentation.screens.profile.ChangePasswordViewModel
import com.carled.app.presentation.screens.profile.EditProfileViewModel
import com.carled.app.presentation.screens.profile.MirageChangePasswordScreen
import com.carled.app.presentation.screens.profile.MirageEditProfileScreen
import com.carled.app.presentation.screens.profile.MirageProfileScreen
import com.carled.app.presentation.screens.scan.ScanViewModel
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.GreenLight
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

private const val PROFILE_PAGE_SIZE = 3

@Composable
fun MirageHomeScreen(
    control: ControlViewModel, scanner: ScanViewModel, ownedDevices: OwnedDevicesViewModel, user: User?,
    marquee: MarqueeViewModel?,
    openScan: () -> Unit, openControl: () -> Unit, openCanvas: (String?) -> Unit,
    openMarquee: () -> Unit, openUpload: () -> Unit, logout: () -> Unit,
    openEffect: () -> Unit = {},
    openEditProfile: () -> Unit = {},
    openClaimDevice: () -> Unit = {},
    openDeviceInfo: (String) -> Unit = {},
    initialTab: Int = HOME_TAB_HOME,
    selectedTab: Int? = null,
    onTabSelected: ((Int) -> Unit)? = null
) {
    var localTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val tab = selectedTab ?: localTab
    val setTab: (Int) -> Unit = { newTab ->
        localTab = newTab
        onTabSelected?.invoke(newTab)
    }

    BackHandler(enabled = tab == HOME_TAB_COMMISSION) { setTab(HOME_TAB_HOME) }
    var showChangePassword by remember { mutableStateOf(false) }
    BackHandler(enabled = showChangePassword) { showChangePassword = false }
    var showEditProfile by remember { mutableStateOf(false) }
    BackHandler(enabled = showEditProfile) { showEditProfile = false }
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

    val changePasswordViewModel: ChangePasswordViewModel? = remember(user?.email) {
        user?.email?.takeIf { it.isNotBlank() }?.let { email ->
            ChangePasswordViewModel(
                userEmail = email,
                forgotPasswordUseCase = ServiceLocator.forgotPasswordUseCase,
                resetPasswordUseCase = ServiceLocator.resetPasswordUseCase
            )
        }
    }

    val editProfileViewModel: EditProfileViewModel? = remember(user?.id) {
        user?.let {
            EditProfileViewModel(
                user = it,
                updateProfileUseCase = ServiceLocator.updateProfileUseCase,
                userPreferences = ServiceLocator.userPreferences
            )
        }
    }
    LaunchedEffect(user) {
        user?.let { editProfileViewModel?.syncFromUpstream(it) }
    }

    val profilesFlow = remember(marquee) { marquee?.profiles ?: MutableStateFlow(TextProfilesState()).asStateFlow() }
    val profilesState by profilesFlow.collectAsState()

    val allowedProfiles = remember(profilesState.entries, profilesState.plan.limit) {
        profilesState.entries.take(profilesState.plan.limit.coerceAtLeast(0))
    }
    val canCreateProfile = marquee != null && allowedProfiles.size < profilesState.plan.limit

    var profilePage by rememberSaveable { mutableIntStateOf(0) }
    val totalPages = ((allowedProfiles.size + PROFILE_PAGE_SIZE - 1) / PROFILE_PAGE_SIZE).coerceAtLeast(1)
    LaunchedEffect(allowedProfiles.size) {
        if (profilePage >= totalPages) {
            profilePage = (totalPages - 1).coerceAtLeast(0)
        }
    }
    val pagedProfiles = remember(allowedProfiles, profilePage) {
        val fromIndex = (profilePage * PROFILE_PAGE_SIZE).coerceAtMost(allowedProfiles.size)
        val toIndex = (fromIndex + PROFILE_PAGE_SIZE).coerceAtMost(allowedProfiles.size)
        allowedProfiles.subList(fromIndex, toIndex)
    }

    Scaffold(
        bottomBar = {
            HardNavBar(
                selectedIndex = tab,
                onTabSelected = setTab,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        },
        containerColor = MirageBackground
    ) { padding ->
        when (tab) {
            HOME_TAB_HOME -> {
                Box(Modifier.padding(padding).fillMaxSize()) {
                    WindowFrame(title = "Home", modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DeviceSection(title = "Shortcut các chức năng:") {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().flatSurface(color = Cream)
                                            .clickable(onClick = openUpload).padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                            Image(painterResource(R.drawable.ic_image_upload), contentDescription = null, modifier = Modifier.size(48.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text("Tải lên/Chiếu ảnh từ kho ảnh cá nhân.", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        ChevronGlyph()
                                    }
                                    Row(
                                        Modifier.fillMaxWidth().flatSurface(color = Cream)
                                            .clickable(onClick = openEffect).padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.width(78.dp).height(66.dp), contentAlignment = Alignment.Center) {
                                            val ledScreenBitmap = ImageBitmap.imageResource(R.drawable.ic_pixelperfect_ledscreen)
                                            Image(
                                                painter = BitmapPainter(ledScreenBitmap, filterQuality = FilterQuality.None),
                                                contentDescription = null,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text("Màu đơn sắc & hiệu ứng LED.", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
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
                                        Text("Kích hoạt thiết bị qua mã QR.", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        ChevronGlyph()
                                    }
                                    Row(
                                        Modifier.fillMaxWidth().flatSurface(color = Cream)
                                            .clickable { setTab(HOME_TAB_COMMISSION) }.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_com),
                                                contentDescription = null,
                                                modifier = Modifier.size(96.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text("Dịch vụ Commission thiết kế riêng. ", color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        ChevronGlyph()
                                    }
                                }
                            }

                            DeviceSection(title = "Profile chữ cá nhân  ${allowedProfiles.size}/${profilesState.plan.limit}") {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Profile chữ cá nhân (${allowedProfiles.size}/${profilesState.plan.limit})",
                                            color = Ink,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .alpha(if (canCreateProfile) 1f else 0.45f)
                                                .hardSurface(
                                                    color = if (canCreateProfile) Cream else MirageMuted.copy(alpha = 0.25f),
                                                    offset = 2.dp,
                                                    borderWidth = 2.dp
                                                )
                                                .clickable(enabled = canCreateProfile, onClick = openMarquee),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+",
                                                color = if (canCreateProfile) Ink else MirageMuted,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    when {
                                        marquee == null -> EmptyRow("Đăng nhập để xem profile đã lưu.")
                                        allowedProfiles.isEmpty() -> EmptyRow("Chưa có bản lưu nào. Nhấn + để tạo profile mới.")
                                        else -> {
                                            pagedProfiles.forEach { profile ->
                                                Row(
                                                    Modifier.fillMaxWidth().flatSurface(color = Cream)
                                                        .clickable {
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
                                                    TextButton(onClick = { openMarquee() }) { Text("Sửa", maxLines = 1, softWrap = false) }
                                                    TextButton(onClick = { marquee.deleteProfile(profile.id) }) { Text("Xóa", maxLines = 1, softWrap = false) }
                                                }
                                            }
                                            if (totalPages > 1) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    PagerButton(
                                                        label = "< Trước",
                                                        enabled = profilePage > 0,
                                                        onClick = { profilePage = (profilePage - 1).coerceAtLeast(0) },
                                                        modifier = Modifier.weight(1f).height(40.dp)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .flatSurface(color = GreenLight)
                                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = "Trang ${profilePage + 1}/$totalPages",
                                                            color = Ink,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                    PagerButton(
                                                        label = "Sau >",
                                                        enabled = profilePage < totalPages - 1,
                                                        onClick = { profilePage = (profilePage + 1).coerceAtMost(totalPages - 1) },
                                                        modifier = Modifier.weight(1f).height(40.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            feedbackMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                        }
                    }
                }
            }
            HOME_TAB_LIBRARY -> {
                Box(Modifier.padding(padding).fillMaxSize()) {
                    ContentLibraryScreen(user?.id ?: "", openCanvas)
                }
            }
            HOME_TAB_CONTROL -> {
                Box(Modifier.padding(padding).fillMaxSize()) {
                    MirageControlScreen(
                        vm = control,
                        ownedDevices = ownedDevices,
                        back = { setTab(HOME_TAB_HOME) },
                        scanner = openScan,
                        openClaimDevice = openClaimDevice,
                        openDeviceInfo = openDeviceInfo
                    )
                }
            }
            HOME_TAB_ACCOUNT -> {
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when {
                        showChangePassword && changePasswordViewModel != null -> {
                            MirageChangePasswordScreen(
                                vm = changePasswordViewModel,
                                back = { showChangePassword = false }
                            )
                        }
                        showEditProfile && editProfileViewModel != null -> {
                            MirageEditProfileScreen(
                                vm = editProfileViewModel,
                                back = { showEditProfile = false }
                            )
                        }
                        else -> {
                            MirageProfileScreen(
                                user = user,
                                control = control,
                                back = { setTab(HOME_TAB_HOME) },
                                onOpenControlTab = { setTab(HOME_TAB_CONTROL) },
                                onOpenPanelControl = { openControl() },
                                onOpenLibraryTab = { setTab(HOME_TAB_LIBRARY) },
                                onOpenAccountSecurity = {
                                    if (changePasswordViewModel != null) showChangePassword = true
                                },
                                onOpenEditProfile = {
                                    if (editProfileViewModel != null) {
                                        showEditProfile = true
                                    } else {
                                        openEditProfile()
                                    }
                                },
                                onLogout = { scanner.disconnectDevice(); logout() }
                            )
                        }
                    }
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

@Composable
private fun PagerButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .hardSurface(
                color = Cream,
                offset = HardStyle.CardOffset,
                borderWidth = 2.dp
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}