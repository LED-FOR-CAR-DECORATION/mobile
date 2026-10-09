package com.carled.app.domain.model

enum class LedMode(val code: Int, val displayName: String) {
    OFF(0, "Tắt"),
    STATIC_COLOR(1, "Màu tĩnh"),
    BREATHING(2, "Nhịp thở (Breathing)"),
    RAINBOW_WAVE(3, "Sóng cầu vồng"),
    CHASE_FLOW(4, "Dòng chảy đuổi màu"),
    MUSIC_REACTIVE(5, "Nháy theo nhạc xe"),
    CUSTOM_PRESET(6, "Chiếu hoạt ảnh Character");

    companion object {
        fun fromCode(code: Int): LedMode = entries.firstOrNull { it.code == code } ?: STATIC_COLOR
    }
}

data class RgbColor(
    val red: Int = 255,
    val green: Int = 255,
    val blue: Int = 255
) {
    fun toHex(): String = String.format("#%02X%02X%02X", red, green, blue)
}

data class LedControlState(
    val isPowerOn: Boolean = true,
    val brightness: Int = 200,      // 0 - 255
    val currentMode: LedMode = LedMode.STATIC_COLOR,
    val speed: Int = 65,            // 1 - 100
    val sensitivity: Int = 75,      // 1 - 100
    val currentColor: RgbColor = RgbColor(0, 200, 255)
)
