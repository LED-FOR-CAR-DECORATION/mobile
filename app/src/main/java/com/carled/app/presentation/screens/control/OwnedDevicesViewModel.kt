package com.carled.app.presentation.screens.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.data.remote.dto.DeviceDetailDto
import com.carled.app.domain.model.OwnedDevice
import com.carled.app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OwnedDevicesUiState {
    object Loading : OwnedDevicesUiState()
    data class Loaded(val devices: List<OwnedDevice>) : OwnedDevicesUiState()
    data class Error(val message: String) : OwnedDevicesUiState()
}

sealed class DeviceDetailUiState {
    object Idle : DeviceDetailUiState()
    object Loading : DeviceDetailUiState()
    data class Loaded(val detail: DeviceDetailDto) : DeviceDetailUiState()
    data class Error(val message: String) : DeviceDetailUiState()
}

class OwnedDevicesViewModel(private val repository: DeviceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<OwnedDevicesUiState>(OwnedDevicesUiState.Loading)
    val uiState: StateFlow<OwnedDevicesUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<DeviceDetailUiState>(DeviceDetailUiState.Idle)
    val detailState: StateFlow<DeviceDetailUiState> = _detailState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = OwnedDevicesUiState.Loading
            when (val res = repository.getOwnedDevices()) {
                is Resource.Success -> _uiState.value = OwnedDevicesUiState.Loaded(res.data)
                is Resource.Error -> _uiState.value = OwnedDevicesUiState.Error(res.message)
                Resource.Loading -> Unit
            }
        }
    }

    fun loadDeviceDetail(panelId: String?) {
        val numericId = panelId?.toLongOrNull() ?: return
        viewModelScope.launch {
            _detailState.value = DeviceDetailUiState.Loading
            when (val res = repository.getDeviceDetail(numericId)) {
                is Resource.Success -> _detailState.value = DeviceDetailUiState.Loaded(res.data)
                is Resource.Error -> _detailState.value = DeviceDetailUiState.Error(res.message)
                Resource.Loading -> Unit
            }
        }
    }

    fun deviceById(id: String?): OwnedDevice? =
        (uiState.value as? OwnedDevicesUiState.Loaded)?.devices?.firstOrNull { it.id == id }
}