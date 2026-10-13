package com.carled.app.presentation.screens.marquee

data class MarqueeUiState(
    val text: String = "XIN NHUONG DUONG - CAM ON!",
    val speed: Int = 60,
    val size: Int = 1,          // 1 = 5x7px, 2 = 10x14px
    val rainbow: Boolean = false,
    val scrollMode: String = "scroll_left",  // "scroll_left" | "static" | "blink"
    val r: Int = 255,
    val g: Int = 215,
    val b: Int = 0,
    val isSending: Boolean = false,
    val feedbackMessage: String? = null,
    val lastEventText: String? = null        // từ telemetry event "marquee_active"
)
