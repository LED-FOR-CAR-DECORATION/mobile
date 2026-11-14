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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.carled.app.presentation.components.MirageBar
import com.carled.app.presentation.theme.Cream
import com.carled.app.presentation.theme.FieldGrey
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Opens the camera, scans for the device's QR code, and claims it via
 * [ClaimDeviceViewModel]. Unless the person turns off "Auto-connect" below,
 * a successful claim is immediately followed by an attempt to pair over BLE
 * using the MAC address encoded in the QR code.
 *
 * [onFinished] fires once there is nothing left to show on this screen:
 * `connected = true` when BLE pairing also succeeded, `false` when the
 * device is claimed but not (yet) connected — the caller decides where to
 * send the person in each case (e.g. straight to Panel Control vs. back home).
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

    Scaffold(topBar = { MirageBar("Quét mã QR thiết bị", back) }, containerColor = Cream) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val state = uiState
            if (!hasCameraPermission) {
                PermissionRationale(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
            } else {
                CameraPreview(
                    enabled = state is ClaimDeviceUiState.Scanning,
                    onBarcodeDetected = vm::onQrCodeDetected
                )
                ScanOverlay()
            }

            if (state is ClaimDeviceUiState.Scanning && hasCameraPermission) {
                AutoConnectToggle(
                    checked = autoConnect,
                    onCheckedChange = vm::setAutoConnect,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                )
            }

            when (state) {
                is ClaimDeviceUiState.Claiming -> StatusBanner("Đang xác nhận thiết bị…")
                is ClaimDeviceUiState.Connecting -> StatusBanner(
                    text = "Đã xác nhận \"${state.device.deviceName}\". Đang kết nối Bluetooth…",
                    onSecondary = { vm.cancelAutoConnect(state.device) },
                    secondaryLabel = "Bỏ qua, kết nối sau"
                )
                is ClaimDeviceUiState.Claimed -> StatusBanner(
                    text = "Đã thêm \"${state.device.deviceName}\" vào tài khoản.",
                    onPrimary = { onFinished(false) },
                    primaryLabel = "Hoàn tất",
                    onSecondary = { vm.retryConnect(state.device) },
                    secondaryLabel = "Thử kết nối Bluetooth"
                )
                is ClaimDeviceUiState.Connected -> {
                    LaunchedEffect(state.device.id) { onFinished(true) }
                    StatusBanner("Đã kết nối \"${state.device.deviceName}\" qua Bluetooth.")
                }
                is ClaimDeviceUiState.Error -> StatusBanner(state.message, isError = true, onPrimary = vm::retryScan, primaryLabel = "Thử lại")
                ClaimDeviceUiState.Scanning -> Unit
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(enabled: Boolean, onBarcodeDetected: (String) -> Unit) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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
private fun ScanOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(240.dp)
                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
        )
    }
    Box(Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
        Text("Đưa mã QR trên thiết bị vào khung hình", color = Color.White)
    }
}

@Composable
private fun AutoConnectToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Tự động kết nối Bluetooth", style = MaterialTheme.typography.labelLarge)
                Text("Kết nối ngay sau khi xác nhận thiết bị", color = FieldGrey, style = MaterialTheme.typography.labelSmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun PermissionRationale(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Cần quyền Camera để quét mã QR thiết bị.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRequest) { Text("Cấp quyền Camera") }
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
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(text, color = if (isError) MaterialTheme.colorScheme.error else FieldGrey)
                Row {
                    if (onSecondary != null) TextButton(onClick = onSecondary) { Text(secondaryLabel ?: "") }
                    if (onPrimary != null) TextButton(onClick = onPrimary) { Text(primaryLabel ?: "") }
                }
            }
        }
    }
}