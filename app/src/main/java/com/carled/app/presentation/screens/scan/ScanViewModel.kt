package com.carled.app.presentation.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanViewModel(
    private val bleUseCases: BleUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        // Lắng nghe cập nhật danh sách thiết bị quét được từ repository
        viewModelScope.launch {
            // Quan sát thiết bị được phát hiện
            // Trong kiến trúc MVVM, ViewModel tổng hợp dữ liệu từ domain/data và xuất ra UiState duy nhất
        }
    }

    fun startScan() {
        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
        bleUseCases.scanDevices()
    }

    fun stopScan() {
        _uiState.update { it.copy(isScanning = false) }
        bleUseCases.stopScan()
    }

    fun connectDevice(macAddress: String) {
        bleUseCases.connectDevice(macAddress)
    }

    fun disconnectDevice() {
        bleUseCases.disconnectDevice()
    }

    fun updateScannedDevices(devices: List<com.carled.app.domain.model.BleDeviceModel>) {
        _uiState.update { it.copy(devices = devices) }
    }

    fun updateConnectionState(state: ConnectionState) {
        _uiState.update {
            it.copy(
                connectionState = state,
                isScanning = state is ConnectionState.Scanning,
                errorMessage = (state as? ConnectionState.Error)?.message
            )
        }
    }
}
