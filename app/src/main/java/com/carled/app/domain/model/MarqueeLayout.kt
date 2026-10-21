package com.carled.app.domain.model

/** Fixed 64×32 firmware output. Never discard text to make a layout fit. */
data class MarqueeLayout(val lines: List<String>, val scale: Int, val fits: Boolean) {
    val glyphLabel: String get() = if (scale == 2) "10×14" else "5×7"
    fun render(color: Int): PixelDocument {
        if (!fits) return PixelDocument(64, 32)
        val pixels = MutableList(64 * 32) { 0xff000000.toInt() }
        val height = lines.size * 8 * scale - scale
        val top = (32 - height) / 2
        lines.forEachIndexed { row, line ->
            val width = (line.length * 6 - 1).coerceAtLeast(0) * scale
            val left = (64 - width) / 2
            line.forEachIndexed { index, char ->
                for (x in 0..4) for (y in 0..6) if (MarqueeFont.column(char, x) and (1 shl y) != 0) {
                    for (dy in 0 until scale) for (dx in 0 until scale) {
                        val px = left + index * 6 * scale + x * scale + dx
                        val py = top + row * 8 * scale + y * scale + dy
                        if (px in 0..63 && py in 0..31) pixels[py * 64 + px] = color
                    }
                }
            }
        }
        return PixelDocument(64, 32, pixels)
    }
    /** Same fitted lit frame for static and blink; RGB565 zero is an all-black off frame. */
    fun projection(blink: Boolean, color: Int): ByteArray {
        require(fits) { "Text does not fit the LED canvas" }
        val lit = render(color).rgb565()
        return if (blink) lit + ByteArray(lit.size) else lit
    }
    companion object {
        const val BLINK_FRAME_DELAY_MS = 400
        fun clean(text: String): String = text.trim().replace(Regex("\\s+"), " ")
        fun fit(text: String, preferredSize: Int): MarqueeLayout {
            val clean = clean(text)
            if (clean.isEmpty()) return MarqueeLayout(emptyList(), preferredSize.coerceIn(1,2), true)
            for (scale in preferredSize.coerceIn(1,2) downTo 1) {
                val columns = (64 / scale + 1) / 6
                val rows = (32 / scale + 1) / 8
                val wrapped = mutableListOf<String>()
                var remaining = clean
                while (remaining.length > columns) {
                    val space = remaining.lastIndexOf(' ', columns)
                    val split = if (space > 0) space else columns
                    wrapped.add(remaining.substring(0,split))
                    remaining = remaining.substring(split).trimStart()
                }
                if (remaining.isNotEmpty()) wrapped.add(remaining)
                if (wrapped.size <= rows) return MarqueeLayout(wrapped, scale, true)
                // Prefer words, but use all available cells before declaring overflow.
                if (clean.length <= columns * rows) return MarqueeLayout(clean.chunked(columns), scale, true)
            }
            return MarqueeLayout(emptyList(), 1, false)
        }
        /** A centered first viewport, not a simulation of scroll timing. */
        fun movingPreview(text: String, size: Int, color: Int): PixelDocument {
            val scale = size.coerceIn(1,2)
            val count = (64 / scale + 1) / 6
            return MarqueeLayout(listOf(clean(text).take(count)), scale, true).render(color)
        }
    }
}
