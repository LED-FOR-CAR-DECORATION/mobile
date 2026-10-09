package com.carled.app.presentation.screens.scan

import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState

data class ScanUiState(
    val isScanning: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val devices: List<BleDeviceModel> = emptyList(),
    val errorMessage: String? = null
)
