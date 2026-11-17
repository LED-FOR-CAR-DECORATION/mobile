package com.carled.app.presentation.screens.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.data.remote.dto.DeviceDetailDto
import com.carled.app.domain.model.OwnedDevice
import com.carled.app.domain.repository.DeviceRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class OwnedDevicesUiState {
    object Loading : OwnedDevicesUiState()
    data class Loaded(val devices: List<OwnedDevice>) : OwnedDevicesUiState()
    data class Error(val message: String) : OwnedDevicesUiState()
}

sealed class DeviceDetailUiState {
    object Loading : DeviceDetailUiState()
    data class Loaded(val detail: DeviceDetailDto) : DeviceDetailUiState()
    data class Error(val message: String) : DeviceDetailUiState()
}

class OwnedDevicesViewModel(private val repository: DeviceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<OwnedDevicesUiState>(OwnedDevicesUiState.Loading)
    val uiState: StateFlow<OwnedDevicesUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<Map<Long, DeviceDetailUiState>>(emptyMap())
    val detailState: StateFlow<Map<Long, DeviceDetailUiState>> = _detailState.asStateFlow()

    init {
        viewModelScope.launch { initialize() }
    }

    // -------------------------------------------------------------------------
    // Init — cache first, network only when necessary
    // -------------------------------------------------------------------------

    private suspend fun initialize() {
        when (val cached = repository.getOwnedDevicesFromCache()) {
            is Resource.Success -> {
                _uiState.value = OwnedDevicesUiState.Loaded(cached.data)
                if (cached.data.isEmpty()) fetchFromServer()
            }
            is Resource.Error -> {
                fetchFromServer()
            }
            Resource.Loading -> Unit
        }
    }

    // -------------------------------------------------------------------------
    // Explicit refresh — called after a successful device claim
    // -------------------------------------------------------------------------

    fun refresh() {
        viewModelScope.launch { fetchFromServer() }
    }

    fun onUserLoggedIn() {
        _detailState.value = emptyMap()
        _uiState.value = OwnedDevicesUiState.Loading
        viewModelScope.launch { fetchFromServer() }
    }

    private suspend fun fetchFromServer() {
        if (_uiState.value !is OwnedDevicesUiState.Loaded) {
            _uiState.value = OwnedDevicesUiState.Loading
        }
        when (val res = repository.getOwnedDevices()) {
            is Resource.Success -> _uiState.value = OwnedDevicesUiState.Loaded(res.data)
            is Resource.Error -> {
                if (_uiState.value !is OwnedDevicesUiState.Loaded) {
                    _uiState.value = OwnedDevicesUiState.Error(res.message)
                }
            }
            Resource.Loading -> Unit
        }
    }

    // -------------------------------------------------------------------------
    // Device detail — stale-while-revalidate
    // -------------------------------------------------------------------------

    fun loadDeviceDetail(deviceId: Long) {
        viewModelScope.launch {
            val cached = repository.getDeviceDetailFromCache(deviceId)
            if (cached is Resource.Success) {
                _detailState.update { it + (deviceId to DeviceDetailUiState.Loaded(cached.data)) }
            } else {
                _detailState.update { it + (deviceId to DeviceDetailUiState.Loading) }
            }

            when (val fresh = repository.getDeviceDetail(deviceId)) {
                is Resource.Success ->
                    _detailState.update { it + (deviceId to DeviceDetailUiState.Loaded(fresh.data)) }
                is Resource.Error -> {
                    if (_detailState.value[deviceId] !is DeviceDetailUiState.Loaded) {
                        _detailState.update { it + (deviceId to DeviceDetailUiState.Error(fresh.message)) }
                    }
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun deviceById(id: String?): OwnedDevice? =
        (uiState.value as? OwnedDevicesUiState.Loaded)?.devices?.firstOrNull { it.id == id }
}