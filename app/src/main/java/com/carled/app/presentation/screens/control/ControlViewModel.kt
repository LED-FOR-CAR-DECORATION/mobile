package com.carled.app.presentation.screens.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.domain.model.*
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ControlViewModel(private val bleUseCases: BleUseCases) : ViewModel() {
    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState = _uiState.asStateFlow()
    private val pending = LatestControls<String, suspend () -> Unit>()
    private val signal = Channel<Unit>(Channel.CONFLATED)
    init {
        // Settle repository updates after the send queue, preserving immediate local drag feedback.
        // This also reflects mode changes sent from the marquee/content screens.
        viewModelScope.launch { bleUseCases.getLedState().collectLatest { value ->
            delay(300)
            _uiState.update { it.copy(ledState = value) }
        } }
        viewModelScope.launch { bleUseCases.getConnectionState().collect { value ->
            if (value !is ConnectionState.Connected) pending.clear()
            _uiState.update { it.copy(connectionState = value) }
        } }
        viewModelScope.launch { bleUseCases.getTelemetryStatus().collect { value -> _uiState.update { it.copy(telemetryStatus = value) } } }
        viewModelScope.launch { bleUseCases.getTelemetryEvent().collect { value -> _uiState.update { it.copy(lastEvent = value) } } }
        viewModelScope.launch {
            for (ignored in signal) {
                while (true) {
                    val action = pending.poll() ?: break
                    execute(action)
                    delay(45)
                }
            }
        }
    }
    private suspend fun execute(action: suspend () -> Unit) {
        if (_uiState.value.connectionState !is ConnectionState.Connected) return
        try { action(); _uiState.update { it.copy(feedbackMessage = null) } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { _uiState.update { it.copy(feedbackMessage = "Không gửi được lệnh: ${e.message}") } }
    }
    private fun continuous(key: String, action: suspend () -> Unit) {
        if (_uiState.value.connectionState !is ConnectionState.Connected) return
        pending.offer(key, action); signal.trySend(Unit)
    }
    private fun command(action: suspend () -> Unit) {
        if (_uiState.value.connectionState !is ConnectionState.Connected) { _uiState.update { it.copy(feedbackMessage = "Kết nối LED trước khi điều khiển.") }; return }
        viewModelScope.launch {
            try { action(); _uiState.update { it.copy(feedbackMessage = "Đã gửi yêu cầu tới thiết bị.") } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(feedbackMessage = "Không gửi được lệnh: ${e.message}") } }
        }
    }
    suspend fun renameDevice(name: String) { bleUseCases.renameDevice(name) }

    fun togglePower() {
        if (_uiState.value.connectionState !is ConnectionState.Connected) return
        val on = !_uiState.value.ledState.isPowerOn
        _uiState.update { it.copy(ledState = it.ledState.copy(isPowerOn = on)) }
        continuous("power") { bleUseCases.sendPowerCommand(on) }
    }
    fun setBrightness(brightness: Int) {
        if (_uiState.value.connectionState !is ConnectionState.Connected) return
        val level = brightness.coerceIn(0, 255)
        _uiState.update { it.copy(ledState = it.ledState.copy(brightness = level)) }
        continuous("brightness") { bleUseCases.sendBrightnessCommand(level) }
    }
    fun setMode(mode: LedMode) {
        if (_uiState.value.connectionState !is ConnectionState.Connected) return
        _uiState.update { it.copy(ledState = it.ledState.copy(currentMode = mode)) }
        continuous("mode") { bleUseCases.sendModeCommand(mode, _uiState.value.ledState.speed) }
    }
    fun setColor(color: RgbColor, throttled: Boolean = true) {
        if (_uiState.value.connectionState !is ConnectionState.Connected) return
        _uiState.update { it.copy(ledState = it.ledState.copy(currentColor = color)) }
        continuous("color") { bleUseCases.sendColorCommand(color, false) }
    }
    fun setSpeed(speed: Int) {
        if (_uiState.value.connectionState !is ConnectionState.Connected) return
        val level = speed.coerceIn(1, 100)
        _uiState.update { it.copy(ledState = it.ledState.copy(speed = level)) }
        continuous("mode") { bleUseCases.sendModeCommand(_uiState.value.ledState.currentMode, level) }
    }
    fun setSensitivity(sens: Int) {
        val level = sens.coerceIn(1, 100)
        _uiState.update { it.copy(ledState = it.ledState.copy(sensitivity = level)) }
        continuous("sensitivity") { bleUseCases.sendSensitivityCommand(level) }
    }
    fun sendWifiConfig(ssid: String, pass: String) = command { bleUseCases.sendWifiConfig(ssid, pass) }
}
