package com.carled.app.presentation.screens.control

import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.DeviceTelemetryStatus
import com.carled.app.domain.model.LedControlState
import com.carled.app.domain.model.TelemetryEvent

data class ControlUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val ledState: LedControlState = LedControlState(),
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null,
    val telemetryStatus: DeviceTelemetryStatus? = null,
    val lastEvent: TelemetryEvent? = null

)
