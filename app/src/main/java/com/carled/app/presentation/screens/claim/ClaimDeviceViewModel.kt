package com.carled.app.presentation.screens.claim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.domain.model.ClaimedDevice
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.QrDecodeResult
import com.carled.app.domain.model.parseDeviceClaimPayload
import com.carled.app.domain.repository.DeviceClaimRepository
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ClaimDeviceUiState {
    object Scanning : ClaimDeviceUiState()
    object Claiming : ClaimDeviceUiState()


    data class Connecting(val device: ClaimedDevice) : ClaimDeviceUiState()


    data class Claimed(val device: ClaimedDevice) : ClaimDeviceUiState()


    data class Connected(val device: ClaimedDevice) : ClaimDeviceUiState()

    data class Error(val message: String) : ClaimDeviceUiState()
}

class ClaimDeviceViewModel(
    private val repository: DeviceClaimRepository,
    private val bleUseCases: BleUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow<ClaimDeviceUiState>(ClaimDeviceUiState.Scanning)
    val uiState: StateFlow<ClaimDeviceUiState> = _uiState.asStateFlow()

    private val _autoConnect = MutableStateFlow(true)
    val autoConnect: StateFlow<Boolean> = _autoConnect.asStateFlow()
    fun setAutoConnect(enabled: Boolean) {
        _autoConnect.value = enabled
    }
    private var hasHandledCode = false
    private var connectJob: Job? = null

    fun onQrCodeDetected(raw: String) {
        if (hasHandledCode) return
        when (val result = parseDeviceClaimPayload(raw)) {
            is QrDecodeResult.Success -> {
                hasHandledCode = true
                _uiState.value = ClaimDeviceUiState.Claiming
                viewModelScope.launch {
                    when (val res = repository.claimDevice(result.payload)) {
                        is Resource.Success -> {
                            val device = res.data
                            if (_autoConnect.value) connectOverBle(device) else _uiState.value = ClaimDeviceUiState.Claimed(device)
                        }
                        is Resource.Error -> {
                            _uiState.value = ClaimDeviceUiState.Error(res.message)
                            hasHandledCode = false
                        }
                        Resource.Loading -> Unit
                    }
                }
            }
            QrDecodeResult.NotADevicePayload -> Unit
            is QrDecodeResult.Malformed -> _uiState.value = ClaimDeviceUiState.Error(result.reason)
        }
    }

    private fun connectOverBle(device: ClaimedDevice) {
        _uiState.value = ClaimDeviceUiState.Connecting(device)
        bleUseCases.connectDevice(device.macAddress)
        connectJob = viewModelScope.launch {
            bleUseCases.getConnectionState().collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        _uiState.value = ClaimDeviceUiState.Connected(device)
                        connectJob?.cancel()
                    }
                    is ConnectionState.Error -> {
                        _uiState.value = ClaimDeviceUiState.Claimed(device)
                        connectJob?.cancel()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun cancelAutoConnect(device: ClaimedDevice) {
        connectJob?.cancel()
        bleUseCases.disconnectDevice()
        _uiState.value = ClaimDeviceUiState.Claimed(device)
    }

    fun retryConnect(device: ClaimedDevice) {
        connectOverBle(device)
    }

    fun retryScan() {
        hasHandledCode = false
        _uiState.value = ClaimDeviceUiState.Scanning
    }

    override fun onCleared() {
        super.onCleared()
        connectJob?.cancel()
    }
}