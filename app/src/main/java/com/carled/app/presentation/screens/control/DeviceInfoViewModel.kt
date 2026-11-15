package com.carled.app.presentation.screens.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.data.remote.dto.DeviceDetailDto
import com.carled.app.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DeviceInfoUiState {
    object Loading : DeviceInfoUiState()
    data class Loaded(val device: DeviceDetailDto) : DeviceInfoUiState()
    data class Error(val message: String) : DeviceInfoUiState()
}

class DeviceInfoViewModel(
    private val repository: DeviceRepository,
    private val panelId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeviceInfoUiState>(DeviceInfoUiState.Loading)
    val uiState: StateFlow<DeviceInfoUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        val id = panelId?.toLongOrNull() ?: -1L
        if (id <= 0) {
            _uiState.value = DeviceInfoUiState.Error("ID thiết bị không hợp lệ.")
            return
        }

        viewModelScope.launch {
            _uiState.value = DeviceInfoUiState.Loading
            when (val res = repository.getDeviceDetail(id)) {
                is Resource.Success -> _uiState.value = DeviceInfoUiState.Loaded(res.data)
                is Resource.Error -> _uiState.value = DeviceInfoUiState.Error(res.message)
                Resource.Loading -> Unit
            }
        }
    }
}