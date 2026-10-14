package com.carled.app.presentation.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.BleDeviceModel
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.usecase.ble.BleUseCases
import com.carled.app.domain.model.TelemetryEvent
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

        // Lắng nghe các event Wi-Fi từ ESP32 qua BLE
        viewModelScope.launch {
            bleUseCases.getTelemetryEvent().collect { event ->
                when (event) {
                    is TelemetryEvent.WifiConnected -> {
                        _uiState.update {
                            it.copy(
                                wifiConnectedIp = event.ip,
                                isSendingWifi = false,
                                wifiMessage = "ESP32 đã kết nối Wi-Fi thành công! IP: ${event.ip}"
                            )
                        }
                    }
                    is TelemetryEvent.WifiDisconnected -> {
                        _uiState.update {
                            it.copy(
                                wifiConnectedIp = null,
                                isSendingWifi = false,
                                wifiMessage = "ESP32 đã ngắt kết nối Wi-Fi"
                            )
                        }
                    }
                    is TelemetryEvent.WifiConnecting -> {
                        _uiState.update {
                            it.copy(
                                isSendingWifi = true,
                                wifiMessage = "ESP32 đang kết nối vào ${event.ssid}..."
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        // Cập nhật IP từ telemetry định kỳ nếu đã kết nối trước đó
        viewModelScope.launch {
            bleUseCases.getTelemetryStatus().collect { status ->
                if (status.wifiIp.isNotBlank() && status.wifiIp != "0.0.0.0") {
                    _uiState.update { it.copy(wifiConnectedIp = status.wifiIp) }
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

    fun sendWifiConfig(ssid: String, pass: String) {
        if (ssid.isBlank()) {
            _uiState.update { it.copy(wifiMessage = "Vui lòng nhập tên Wi-Fi (SSID)") }
            return
        }
        _uiState.update {
            it.copy(
                isSendingWifi = true,
                wifiMessage = "Đang gửi thông tin Wi-Fi tới ESP32..."
            )
        }
        viewModelScope.launch {
            bleUseCases.sendWifiConfig(ssid.trim(), pass)
        }
    }

    fun clearWifiMessage() {
        _uiState.update { it.copy(wifiMessage = null) }
    }
}
