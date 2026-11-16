package com.carled.app.presentation.screens.claim

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.carled.app.R
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Opens the camera, scans for the device's QR code, and claims it via
 * [ClaimDeviceViewModel]. Unless the person turns off "Tự động kết nối Bluetooth"
 * below, a successful claim is immediately followed by an attempt to pair over
 * BLE using the MAC address encoded in the QR code.
 *
 * [onFinished] fires once there is nothing left to show on this screen:
 * `connected = true` when BLE pairing also succeeded, `false` when the device
 * is claimed but not (yet) connected — the caller decides where to send the
 * person in each case (e.g. straight to Panel Control vs. back home).
 */
@Composable
fun ClaimDeviceScreen(
    vm: ClaimDeviceViewModel,
    back: () -> Unit,
    onFinished: (connected: Boolean) -> Unit
) {
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()
    val autoConnect by vm.autoConnect.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MirageBackground)
            .systemBarsPadding()
            .padding(8.dp)
    ) {
        WindowFrame(title = "Scan device", onClose = back, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val state = uiState

                // Only show the camera viewfinder box while actively scanning
                if (state is ClaimDeviceUiState.Scanning) {
                    Box(
                        Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .dashedBorder(color = Green, cornerRadius = 16.dp)
                    ) {
                        if (hasCameraPermission) {
                            CameraPreview(
                                enabled = true,
                                onBarcodeDetected = vm::onQrCodeDetected
                            )
                        } else {
                            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "Cần quyền Camera để quét mã QR thiết bị.",
                                    textAlign = TextAlign.Center,
                                    color = MirageMuted
                                )
                            }
                        }
                    }
                }

                // Matches the "Bind Device" CTA in the mockup...
                BindDeviceButton(
                    onClick = { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }
                )

                if (state is ClaimDeviceUiState.Scanning && hasCameraPermission) {
                    AutoConnectToggle(checked = autoConnect, onCheckedChange = vm::setAutoConnect)
                }

                when (state) {
                    is ClaimDeviceUiState.Claiming -> StatusBanner("Đang xác nhận thiết bị…")
                    is ClaimDeviceUiState.Connecting -> StatusBanner(
                        text = "Đã xác nhận \"${state.device.deviceName}\". Đang kết nối Bluetooth…",
                        secondaryLabel = "Bỏ qua, kết nối sau",
                        onSecondary = { vm.cancelAutoConnect(state.device) }
                    )
                    is ClaimDeviceUiState.Claimed -> StatusBanner(
                        text = "Đã thêm \"${state.device.deviceName}\" vào tài khoản.",
                        primaryLabel = "Hoàn tất",
                        onPrimary = { onFinished(false) },
                        secondaryLabel = "Thử kết nối Bluetooth",
                        onSecondary = { vm.retryConnect(state.device) }
                    )
                    is ClaimDeviceUiState.Connected -> {
                        LaunchedEffect(state.device.id) { onFinished(true) }
                        StatusBanner("Đã kết nối \"${state.device.deviceName}\" qua Bluetooth.")
                    }
                    is ClaimDeviceUiState.Error -> StatusBanner(
                        text = state.message,
                        isError = true,
                        primaryLabel = "Thử lại",
                        onPrimary = vm::retryScan
                    )
                    ClaimDeviceUiState.Scanning -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(enabled: Boolean, onBarcodeDetected: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { BarcodeScanning.getClient() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null || !enabled) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(input)
                        .addOnSuccessListener { barcodes ->
                            barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onBarcodeDetected)
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun BindDeviceButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .flatSurface(color = Cream)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(painterResource(R.drawable.ic_qrcode), contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text("Bind Device", color = Ink, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AutoConnectToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().flatSurface(color = Cream).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Tự động kết nối Bluetooth", color = Ink, style = MaterialTheme.typography.labelLarge)
            Text("Kết nối ngay sau khi xác nhận thiết bị", color = MirageMuted, style = MaterialTheme.typography.labelSmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatusBanner(
    text: String,
    isError: Boolean = false,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(text, color = if (isError) MaterialTheme.colorScheme.error else MirageMuted)
            if (onPrimary != null || onSecondary != null) {
                Row {
                    if (onSecondary != null) TextButton(onClick = onSecondary) { Text(secondaryLabel ?: "") }
                    if (onPrimary != null) TextButton(onClick = onPrimary) { Text(primaryLabel ?: "") }
                }
            }
        }
    }
}

/** Dashed rounded-rect border, matching the scan viewfinder in the mockup. */
private fun Modifier.dashedBorder(
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Dp = 3.dp,
    dashLength: Dp = 12.dp,
    gapLength: Dp = 8.dp,
    cornerRadius: Dp = 16.dp
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        drawRoundRect(
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), gapLength.toPx()), 0f)
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }
)