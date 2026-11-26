package com.carled.app.presentation.screens.marquee

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carled.app.domain.model.MarqueeLayout
import com.carled.app.domain.model.PixelDocument
import com.carled.app.domain.model.TextProfile
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.presentation.components.WindowFrame
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.Green
import com.carled.app.presentation.theme.GreenLight
import com.carled.app.presentation.theme.Ink
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun MirageMarqueeScreen(
    vm: MarqueeViewModel,
    back: () -> Unit,
    onOpenDeviceInfo: () -> Unit = {}
) {
    val state by vm.uiState.collectAsState()
    val connection by vm.connection.collectAsState()
    val profiles by vm.profiles.collectAsState()

    val canvasOptions = remember {
        val base = PixelDocument.sizes.map { (w, h) -> CanvasSizeOption(w, h, "$w×$h") }
        if (base.none { it.width == 64 && it.height == 32 }) {
            listOf(CanvasSizeOption(64, 32, "64×32")) + base
        } else {
            base
        }
    }
    var selectedCanvas by remember { mutableStateOf(canvasOptions.first()) }
    var customRatioText by remember { mutableStateOf<String?>(null) }
    var menuAnchor by remember { mutableStateOf(MenuAnchor.TOP) }

    val layout = remember(state.text, state.size) { MarqueeLayout.fit(state.text, state.size) }
    val static = state.scrollMode != "scroll_left"
    val valid = state.text.isNotBlank() && state.text.all { it.code in 32..126 }
    val connected = connection is ConnectionState.Connected
    val canSend = connected && !state.isSending && valid && (!static || layout.fits)
    val enabled = !state.isSending

    var showMenuSheet by remember { mutableStateOf(false) }
    var showSizeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    var choosing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var updating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TextProfile?>(null) }
    var upgrade by remember { mutableStateOf(false) }

    val editable = profiles.loaded && !profiles.busy && !state.isSending
    val selected = profiles.entries.firstOrNull { it.id == profiles.selectedId }
    val profileIndex = profiles.entries.indexOfFirst { it.id == profiles.selectedId }.let { if (it >= 0) it + 1 else 1 }
    val contentValid = valid && (!static || layout.fits)

    var profileNameInput by remember(selected?.id) { mutableStateOf(selected?.name ?: "") }
    var dialogNameInput by remember { mutableStateOf("") }

    BackHandler(state.isSending) { }

    var scrollOffset by remember { mutableIntStateOf(0) }
    var blinkLit by remember { mutableStateOf(true) }
    var rainbowHue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.scrollMode, state.speed, state.text) {
        scrollOffset = 0
        if (state.scrollMode == "scroll_left") {
            while (true) {
                delay((130L - (state.speed.coerceIn(1, 100) * 1.1f).toLong()).coerceAtLeast(16L))
                scrollOffset += 1
            }
        }
    }

    LaunchedEffect(state.scrollMode, state.blinkSpeed) {
        blinkLit = true
        if (state.scrollMode == "blink") {
            val halfPeriod = (MarqueeViewModel.blinkDelayMs(state.blinkSpeed) / 2).coerceAtLeast(75)
            while (true) {
                delay(halfPeriod.toLong())
                blinkLit = !blinkLit
            }
        }
    }

    LaunchedEffect(state.rainbow) {
        if (state.rainbow) {
            while (true) {
                delay(40L)
                rainbowHue = (rainbowHue + 5f) % 360f
            }
        } else {
            rainbowHue = 0f
        }
    }

    val activeColorInt = remember(state.r, state.g, state.b, state.rainbow, rainbowHue) {
        if (state.rainbow) AndroidColor.HSVToColor(floatArrayOf(rainbowHue, 1f, 1f))
        else AndroidColor.rgb(state.r, state.g, state.b)
    }

    val previewDoc = remember(state.text, state.size, state.scrollMode, activeColorInt, scrollOffset, blinkLit, layout) {
        when (state.scrollMode) {
            "scroll_left" -> MarqueeLayout.scrollingPreview(state.text, state.size, activeColorInt, scrollOffset)
            "blink" -> if (blinkLit) layout.render(activeColorInt) else PixelDocument(64, 32)
            else -> layout.render(activeColorInt)
        }
    }

    val previewAspect = remember(selectedCanvas, customRatioText) {
        val parts = customRatioText?.split(":")
        if (parts != null && parts.size == 2) {
            val w = parts[0].toFloatOrNull()
            val h = parts[1].toFloatOrNull()
            if (w != null && h != null && w > 0f && h > 0f) return@remember (w / h).coerceIn(0.5f, 4f)
        }
        (selectedCanvas.width.toFloat() / max(1, selectedCanvas.height).toFloat()).coerceIn(0.5f, 4f)
    }

    val targetName = profileNameInput.trim().ifEmpty { selected?.name.orEmpty() }
    val canUpdateNow = editable && contentValid && selected != null && targetName.isNotBlank()
    val canCreateNow = editable && contentValid && profiles.entries.size < profiles.plan.limit

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            MarqueeBottomBar(
                r = state.r,
                g = state.g,
                b = state.b,
                scrollMode = state.scrollMode,
                speed = state.speed,
                blinkSpeed = state.blinkSpeed,
                isSending = state.isSending,
                enabled = enabled,
                canSend = canSend,
                sizeLabel = customRatioText ?: "${selectedCanvas.width}×${selectedCanvas.height}",
                menuAnchor = menuAnchor,
                hasProfiles = vm.hasProfiles,
                editable = editable,
                hasAnyProfile = profiles.entries.isNotEmpty(),
                canUpdateNow = canUpdateNow,
                canCreateNow = canCreateNow,
                profileNameInput = profileNameInput,
                fallbackProfileName = selected?.name.orEmpty(),
                onProfileNameChange = { profileNameInput = it.take(40) },
                onOpenColor = { showColorDialog = true },
                onSelectMode = vm::updateScrollMode,
                onSpeedChange = vm::updateSpeed,
                onBlinkSpeedChange = vm::updateBlinkSpeed,
                onSend = vm::sendMarquee,
                onOpenMenu = { showMenuSheet = true },
                onOpenSize = { showSizeDialog = true },
                onUpdateProfile = {
                    if (selected != null && targetName.isNotBlank()) {
                        vm.saveProfile(targetName, update = true) {
                            profileNameInput = targetName
                        }
                    } else {
                        updating = true
                        dialogNameInput = selected?.name.orEmpty()
                        vm.clearProfileError()
                        saving = true
                    }
                },
                onCreateProfile = {
                    updating = false
                    dialogNameInput = profileNameInput.trim()
                    vm.clearProfileError()
                    saving = true
                },
                onSwitchProfile = { vm.clearProfileError(); choosing = true }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Ink)
        ) {
            val bodyHeight = (maxHeight - 40.dp).coerceAtLeast(0.dp)

            WindowFrame(
                title = if (selected != null) "Text profile : $profileIndex" else "Chạy chữ",
                onClose = { if (enabled) back() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = bodyHeight)
                        .background(Color(0xFF23261F))
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MarqueePreviewSection(
                        previewDoc = previewDoc,
                        previewAspect = previewAspect,
                        layout = layout,
                        staticMode = static,
                        scrollMode = state.scrollMode,
                        size = state.size,
                        connected = connected
                    )
                    MarqueeInputSection(
                        text = state.text,
                        size = state.size,
                        enabled = enabled,
                        onTextChange = vm::updateText,
                        onSizeChange = vm::updateSize
                    )
                    if (state.isSending) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = GreenLight,
                            trackColor = Color(0xFF1A221B)
                        )
                    }
                    state.feedbackMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = Cream,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF121814))
                                .border(1.dp, Green.copy(alpha = 0.6f))
                                .padding(10.dp)
                        )
                    }
                }
            }
        }
    }

    if (showMenuSheet) {
        MarqueeMenuSheet(
            isSending = state.isSending,
            canSend = canSend,
            connected = connected,
            enabled = enabled,
            menuAnchor = menuAnchor,
            onToggleMenuAnchor = {
                menuAnchor = if (menuAnchor == MenuAnchor.TOP) MenuAnchor.BOTTOM else MenuAnchor.TOP
            },
            onOpenDeviceInfo = onOpenDeviceInfo,
            onSend = vm::sendMarquee,
            onStop = vm::stopMarquee,
            onOpenSize = { showSizeDialog = true },
            onDismiss = { showMenuSheet = false }
        )
    }

    if (showSizeDialog) {
        CanvasSizeDialog(
            options = canvasOptions,
            selectedCanvas = selectedCanvas,
            customRatioText = customRatioText,
            onDismiss = { showSizeDialog = false },
            onSelectCanvas = { opt -> selectedCanvas = opt; customRatioText = null; showSizeDialog = false },
            onApplyRatio = { w, h -> customRatioText = "$w:$h"; showSizeDialog = false }
        )
    }

    if (showColorDialog) {
        MarqueeColorWheelDialog(
            r = state.r,
            g = state.g,
            b = state.b,
            rainbow = state.rainbow,
            onToggleRainbow = vm::toggleRainbow,
            onSelectColor = { r, g, b -> vm.updateColor(r, g, b) },
            onDismiss = { showColorDialog = false }
        )
    }

    MarqueeProfileDialogs(
        profiles = profiles,
        editable = editable,
        choosing = choosing,
        saving = saving,
        updating = updating,
        dialogNameInput = dialogNameInput,
        deleting = deleting,
        upgrade = upgrade,
        onDialogNameChange = { dialogNameInput = it.take(40) },
        onDismissChoosing = { choosing = false },
        onSelectProfile = { profile ->
            vm.selectProfile(profile.id)
            profileNameInput = profile.name
            choosing = false
        },
        onRequestDelete = { deleting = it },
        onDismissSaving = { saving = false },
        onConfirmSave = {
            val cleanName = dialogNameInput.trim()
            vm.saveProfile(cleanName, updating) {
                profileNameInput = cleanName
                saving = false
            }
        },
        onDismissDelete = { deleting = null },
        onConfirmDelete = { profile ->
            vm.deleteProfile(profile.id) {
                if (selected?.id == profile.id) profileNameInput = ""
                deleting = null
            }
        },
        onDismissUpgrade = { upgrade = false }
    )
}