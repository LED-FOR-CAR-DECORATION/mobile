package com.carled.app.presentation.screens.marquee

data class MarqueeUiState(
    val text: String = "XIN NHUONG DUONG - CAM ON!",
    val speed: Int = 60,
    val size: Int = 1,          // letter scale: 1 = 5x7px, 2 = 10x14px, up to 4 = 20x28px (freely typable in the editor)
    val rainbow: Boolean = false,
    val scrollMode: String = "scroll_left",  // "scroll_left" | "static" | "blink"
    val r: Int = 255,
    val g: Int = 215,
    val b: Int = 0,
    /** Nhấp nháy interval, 1..100 (higher = faster blink). Only used when scrollMode == "blink". */
    val blinkSpeed: Int = 50,
    val isSending: Boolean = false,
    val feedbackMessage: String? = null,
    val lastEventText: String? = null        // từ telemetry event "marquee_active"
)