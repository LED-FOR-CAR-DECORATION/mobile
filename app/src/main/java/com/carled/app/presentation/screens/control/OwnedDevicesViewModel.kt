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

    /**
     * On cold start: read whatever is on disk and show it immediately — no
     * network call, no token needed, no Loading flash for returning users.
     *
     * Network is triggered automatically only when the cache is empty, which
     * happens exactly twice in normal usage:
     *   1. First time the account is used on this device (fresh install / first login).
     *   2. After an explicit [refresh] call (post-claim).
     *
     * Logout wipes [UserPreferences], so logging back in counts as "first time"
     * and correctly triggers an automatic fetch.
     */
    private suspend fun initialize() {
        when (val cached = repository.getOwnedDevicesFromCache()) {
            is Resource.Success -> {
                _uiState.value = OwnedDevicesUiState.Loaded(cached.data)
                // Empty cache = account has never had its device list fetched on
                // this device (new install or first login). Fetch once automatically.
                if (cached.data.isEmpty()) fetchFromServer()
            }
            is Resource.Error -> {
                // No cache at all — first time this account is used here.
                fetchFromServer()
            }
            Resource.Loading -> Unit
        }
    }

    // -------------------------------------------------------------------------
    // Explicit refresh — called after a successful device claim
    // -------------------------------------------------------------------------

    /**
     * Fetches a fresh list from the server and updates the cache.
     * Only call this when you know the server state has changed:
     * after [ClaimDeviceScreen] reports a successful claim.
     *
     * If the fetch fails, any already-displayed cached data is preserved — the
     * user will not see a regression to an error screen just because a background
     * refresh hit a transient network issue.
     */
    fun refresh() {
        viewModelScope.launch { fetchFromServer() }
    }

    private suspend fun fetchFromServer() {
        // Show Loading only if there is nothing to display yet.
        if (_uiState.value !is OwnedDevicesUiState.Loaded) {
            _uiState.value = OwnedDevicesUiState.Loading
        }
        when (val res = repository.getOwnedDevices()) {
            is Resource.Success -> _uiState.value = OwnedDevicesUiState.Loaded(res.data)
            is Resource.Error -> {
                // If we already have good cached data on screen, silently swallow
                // the error — no need to show a banner for a background refresh failure.
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

    /**
     * Shows any cached detail for [deviceId] immediately (no Loading flash),
     * then silently refreshes from the server in the background.
     *
     * If the network call fails but cached data is already displayed, the error
     * is swallowed — the user keeps seeing their last-known detail rather than
     * a blank error screen.
     */
    fun loadDeviceDetail(deviceId: Long) {
        viewModelScope.launch {
            // Surface cache instantly — eliminates the blank-screen flash on re-open.
            val cached = repository.getDeviceDetailFromCache(deviceId)
            if (cached is Resource.Success) {
                _detailState.update { it + (deviceId to DeviceDetailUiState.Loaded(cached.data)) }
            } else {
                _detailState.update { it + (deviceId to DeviceDetailUiState.Loading) }
            }

            // Then silently refresh from the server.
            when (val fresh = repository.getDeviceDetail(deviceId)) {
                is Resource.Success ->
                    _detailState.update { it + (deviceId to DeviceDetailUiState.Loaded(fresh.data)) }
                is Resource.Error -> {
                    // Don't replace a good loaded state with a network error.
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