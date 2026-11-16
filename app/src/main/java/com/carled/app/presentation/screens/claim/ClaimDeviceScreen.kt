package com.carled.app.presentation.screens.claim

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.carled.app.presentation.components.*
import com.carled.app.presentation.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

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
            // Removed verticalScroll to prevent infinite max height constraint crashes
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val state = uiState

                // Only show the camera viewfinder box while actively scanning
                if (state is ClaimDeviceUiState.Scanning) {
                    Box(
                        Modifier
                            .size(280.dp) // Adjusted to fit safely without overflowing height
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

                    // Add this log to check if frames are hitting the analyzer
                    Log.d("CameraScan", "Processing frame at rotation: ${imageProxy.imageInfo.rotationDegrees}")

                    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(input)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                Log.i("CameraScan", "Barcode detected raw value: ${barcode.rawValue}")
                            }
                            barcodes.firstNotNullOfOrNull { it.rawValue }?.let(onBarcodeDetected)
                        }
                        .addOnFailureListener { e ->
                            Log.e("CameraScan", "Barcode scan failed: ${e.message}")
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