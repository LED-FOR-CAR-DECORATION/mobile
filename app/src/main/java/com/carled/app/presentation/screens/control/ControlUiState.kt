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
    /** Trạng thái định kỳ nhận từ ESP32 telemetry */
    val telemetryStatus: DeviceTelemetryStatus? = null,
    /** Event gần nhất nhận từ ESP32 (để hiển thị toast/banner ngắn gọn) */
    val lastEvent: TelemetryEvent? = null
)
