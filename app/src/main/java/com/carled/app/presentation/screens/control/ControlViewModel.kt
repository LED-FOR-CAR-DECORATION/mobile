package com.carled.app.presentation.screens.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.LedMode
import com.carled.app.domain.model.RgbColor
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ControlViewModel(
    private val bleUseCases: BleUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bleUseCases.getLedState().collect { state ->
                _uiState.update { it.copy(ledState = state) }
            }
        }
        viewModelScope.launch {
            bleUseCases.getConnectionState().collect { conn ->
                _uiState.update { it.copy(connectionState = conn) }
            }
        }
    }

    fun togglePower() {
        val newPowerState = !_uiState.value.ledState.isPowerOn
        viewModelScope.launch {
            bleUseCases.sendPowerCommand(newPowerState)
            _uiState.update {
                it.copy(ledState = it.ledState.copy(isPowerOn = newPowerState))
            }
        }
    }

    fun setBrightness(brightness: Int) {
        val clamped = brightness.coerceIn(0, 255)
        _uiState.update {
            it.copy(ledState = it.ledState.copy(brightness = clamped))
        }
        viewModelScope.launch {
            bleUseCases.sendBrightnessCommand(clamped)
        }
    }

    fun setMode(mode: LedMode) {
        val currentSpeed = _uiState.value.ledState.speed
        _uiState.update {
            it.copy(ledState = it.ledState.copy(currentMode = mode))
        }
        viewModelScope.launch {
            bleUseCases.sendModeCommand(mode, currentSpeed)
        }
    }

    fun setColor(color: RgbColor, throttled: Boolean = true) {
        _uiState.update {
            it.copy(ledState = it.ledState.copy(currentColor = color))
        }
        viewModelScope.launch {
            bleUseCases.sendColorCommand(color, throttled)
        }
    }

    fun setSpeed(speed: Int) {
        val clamped = speed.coerceIn(1, 100)
        val mode = _uiState.value.ledState.currentMode
        _uiState.update {
            it.copy(ledState = it.ledState.copy(speed = clamped))
        }
        viewModelScope.launch {
            bleUseCases.sendModeCommand(mode, clamped)
        }
    }
}
