package com.carled.app.presentation.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.BleDeviceModel
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
        // Quan sát danh sách thiết bị quét được từ repository
        viewModelScope.launch {
            bleUseCases.getScannedDevices().collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }

        // Quan sát trạng thái kết nối Bluetooth (Scanning, Connecting, Connected, Disconnected, Error)
        viewModelScope.launch {
            bleUseCases.getConnectionState().collect { state ->
                _uiState.update {
                    it.copy(
                        connectionState = state,
                        isScanning = state is ConnectionState.Scanning,
                        errorMessage = (state as? ConnectionState.Error)?.message
                    )
                }
            }
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
}
