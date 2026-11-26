package com.carled.app.domain.model

/** Fixed 64×32 firmware output. Never discard text to make a layout fit. */
data class MarqueeLayout(val lines: List<String>, val scale: Int, val fits: Boolean) {
    val glyphLabel: String get() = "${scale * 5}×${scale * 7}"
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

        /** Letter scale range the UI lets the person type freely. 4× (20×28px)
         *  is the largest integer multiple of the 5×7 glyph that still fits a
         *  single line inside the firmware's fixed 64×32 canvas (7*4 = 28 <= 32). */
        val SCALE_RANGE = 1..4

        fun clean(text: String): String = text.trim().replace(Regex("\\s+"), " ")
        fun fit(text: String, preferredSize: Int): MarqueeLayout {
            val clean = clean(text)
            if (clean.isEmpty()) return MarqueeLayout(emptyList(), preferredSize.coerceIn(SCALE_RANGE), true)
            for (scale in preferredSize.coerceIn(SCALE_RANGE) downTo 1) {
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
            val scale = size.coerceIn(SCALE_RANGE)
            val count = (64 / scale + 1) / 6
            return MarqueeLayout(listOf(clean(text).take(count)), scale, true).render(color)
        }

        /**
         * A single 64×32 "window" onto an endless left-scrolling belt made of
         * [text] repeated with a gap, offset by [offsetColumns] pixel-columns.
         * Purely a UI preview (what the live editor animates) — it does not
         * affect what gets uploaded to the device, which still goes through
         * [fit] + [projection]/the firmware's own scroll_left mode.
         */
        fun scrollingPreview(text: String, size: Int, color: Int, offsetColumns: Int): PixelDocument {
            val scale = size.coerceIn(SCALE_RANGE)
            val clean = clean(text).ifEmpty { " " }
            val belt = "$clean     " // trailing gap before the belt repeats
            val charWidth = 6 * scale
            val beltWidthPx = (charWidth * belt.length).coerceAtLeast(1)
            val top = ((32 - 7 * scale) / 2).coerceAtLeast(0)
            val pixels = MutableList(64 * 32) { 0xff000000.toInt() }
            val start = ((offsetColumns % beltWidthPx) + beltWidthPx) % beltWidthPx
            for (repeat in 0..1) {
                val baseX = -start + repeat * beltWidthPx
                belt.forEachIndexed { index, char ->
                    val left = baseX + index * charWidth
                    if (left + charWidth < 0 || left >= 64) return@forEachIndexed
                    for (x in 0..4) for (y in 0..6) if (MarqueeFont.column(char, x) and (1 shl y) != 0) {
                        for (dy in 0 until scale) for (dx in 0 until scale) {
                            val px = left + x * scale + dx
                            val py = top + y * scale + dy
                            if (px in 0..63 && py in 0..31) pixels[py * 64 + px] = color
                        }
                    }
                }
            }
            return PixelDocument(64, 32, pixels)
        }
    }
}