package com.carled.app.domain.model

import kotlin.math.*

enum class PixelTool { PEN, ERASER, PICKER, FILL, SHAPE, TEXT }
enum class MirrorMode { NONE, HORIZONTAL, VERTICAL, BOTH }
enum class PixelShape { LINE, RECTANGLE, ELLIPSE }
data class PixelText(val id: String, val text: String, val x: Int, val y: Int,
    val glyphWidth: Int = 3, val glyphHeight: Int = 5, val color: Int = 0xff89c89b.toInt())

/** Zero is an empty cell. Opaque black remains a real painted cell in a draft. */
data class PixelDocument(
    val width: Int, val height: Int,
    val pixels: List<Int> = List(width * height) { 0 },
    val texts: List<PixelText> = emptyList()
) {
    init {
        require((width to height) in sizes)
        require(pixels.size == width * height)
    }
    companion object { val sizes = listOf(32 to 32, 64 to 32, 64 to 64) }

    fun brush(x0: Int, y0: Int, x1: Int, y1: Int, color: Int, size: Int, mirror: MirrorMode): PixelDocument {
        require(size in 1..4)
        val out = pixels.toMutableList()
        fun put(x: Int, y: Int) { if (x in 0 until width && y in 0 until height) out[y * width + x] = color }
        for ((x, y) in line(x0, y0, x1, y1)) {
            for (dy in 0 until size) for (dx in 0 until size) {
                val px = x + dx - (size - 1) / 2
                val py = y + dy - (size - 1) / 2
                put(px, py)
                if (mirror == MirrorMode.HORIZONTAL || mirror == MirrorMode.BOTH) put(width - 1 - px, py)
                if (mirror == MirrorMode.VERTICAL || mirror == MirrorMode.BOTH) put(px, height - 1 - py)
                if (mirror == MirrorMode.BOTH) put(width - 1 - px, height - 1 - py)
            }
        }
        return copy(pixels = out)
    }

    fun fill(x: Int, y: Int, color: Int): PixelDocument {
        if (x !in 0 until width || y !in 0 until height) return this
        val target = pixels[y * width + x]
        if (target == color) return this
        val out = pixels.toMutableList()
        val queue = ArrayDeque<Int>()
        fun visit(px: Int, py: Int) {
            if (px in 0 until width && py in 0 until height && out[py * width + px] == target) {
                out[py * width + px] = color; queue.addLast(py * width + px)
            }
        }
        visit(x, y)
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst(); val px = i % width; val py = i / width
            visit(px - 1, py); visit(px + 1, py); visit(px, py - 1); visit(px, py + 1)
        }
        return copy(pixels = out)
    }

    fun shape(x0: Int, y0: Int, x1: Int, y1: Int, color: Int, kind: PixelShape, filled: Boolean, mirror: MirrorMode): PixelDocument {
        val out = pixels.toMutableList()
        val left = min(x0, x1); val right = max(x0, x1)
        val top = min(y0, y1); val bottom = max(y0, y1)
        fun set(x: Int, y: Int) { if (x in 0 until width && y in 0 until height) out[y * width + x] = color }
        fun put(x: Int, y: Int) {
            set(x, y)
            if (mirror == MirrorMode.HORIZONTAL || mirror == MirrorMode.BOTH) set(width - 1 - x, y)
            if (mirror == MirrorMode.VERTICAL || mirror == MirrorMode.BOTH) set(x, height - 1 - y)
            if (mirror == MirrorMode.BOTH) set(width - 1 - x, height - 1 - y)
        }
        if (kind == PixelShape.LINE) return brush(x0, y0, x1, y1, color, 1, mirror)
        if (kind == PixelShape.RECTANGLE) {
            for (y in top..bottom) for (x in left..right)
                if (filled || x == left || x == right || y == top || y == bottom) put(x, y)
        } else {
            val rx = (right - left) / 2.0; val ry = (bottom - top) / 2.0
            if (rx == 0.0 || ry == 0.0) return brush(x0, y0, x1, y1, color, 1, mirror)
            val cx = (left + right) / 2.0; val cy = (top + bottom) / 2.0
            if (filled) for (y in top..bottom) for (x in left..right) {
                if (((x - cx) / (rx + .3)).pow(2) + ((y - cy) / (ry + .3)).pow(2) <= 1.0) put(x, y)
            } else {
                val steps = (max(rx, ry) * 16).toInt().coerceAtLeast(24)
                for (i in 0..steps) { val a = i * 2 * PI / steps; put((cx + rx * cos(a)).roundToInt(), (cy + ry * sin(a)).roundToInt()) }
            }
        }
        return copy(pixels = out)
    }

    fun composite(): List<Int> {
        val out = pixels.toMutableList()
        texts.forEach { block ->
            block.text.forEachIndexed { index, char ->
                val glyph = PixelFont.glyph(char)
                for (y in 0 until block.glyphHeight) for (x in 0 until block.glyphWidth) {
                    if (glyph[y * 5 / block.glyphHeight][x * 3 / block.glyphWidth] == '1') {
                        val px = block.x + index * (block.glyphWidth + 1) + x; val py = block.y + y
                        if (px in 0 until width && py in 0 until height) out[py * width + px] = block.color
                    }
                }
            }
        }
        return out
    }

    fun rgb565(): ByteArray = composite().flatMap { pixel ->
        val value = (((pixel ushr 19) and 31) shl 11) or (((pixel ushr 10) and 63) shl 5) or ((pixel ushr 3) and 31)
        listOf((value and 255).toByte(), (value ushr 8).toByte())
    }.toByteArray()

    private fun line(x0: Int, y0: Int, x1: Int, y1: Int): List<Pair<Int, Int>> {
        var x = x0; var y = y0; val dx = abs(x1 - x0); val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1; var error = dx + dy
        val points = mutableListOf<Pair<Int, Int>>()
        while (true) { points.add(x to y); if (x == x1 && y == y1) break
            val e = error * 2; if (e >= dy) { error += dy; x += sx }; if (e <= dx) { error += dx; y += sy }
        }
        return points
    }
}

/** Hand-authored 3×5 bitmap alphabet; larger presets use nearest-neighbor glyph scaling. */
object PixelFont {
    private val alphabet = mapOf(
        'A' to "010/101/111/101/101", 'B' to "110/101/110/101/110", 'C' to "011/100/100/100/011",
        'D' to "110/101/101/101/110", 'E' to "111/100/110/100/111", 'F' to "111/100/110/100/100",
        'G' to "011/100/101/101/011", 'H' to "101/101/111/101/101", 'I' to "111/010/010/010/111",
        'J' to "001/001/001/101/010", 'K' to "101/101/110/101/101", 'L' to "100/100/100/100/111",
        'M' to "101/111/111/101/101", 'N' to "101/111/111/111/101", 'O' to "010/101/101/101/010",
        'P' to "110/101/110/100/100", 'Q' to "010/101/101/111/011", 'R' to "110/101/110/101/101",
        'S' to "011/100/010/001/110", 'T' to "111/010/010/010/010", 'U' to "101/101/101/101/111",
        'V' to "101/101/101/101/010", 'W' to "101/101/111/111/101", 'X' to "101/101/010/101/101",
        'Y' to "101/101/010/010/010", 'Z' to "111/001/010/100/111",
        '0' to "111/101/101/101/111", '1' to "010/110/010/010/111", '2' to "110/001/010/100/111",
        '3' to "110/001/010/001/110", '4' to "101/101/111/001/001", '5' to "111/100/110/001/110",
        '6' to "011/100/111/101/111", '7' to "111/001/010/010/010", '8' to "111/101/111/101/111",
        '9' to "111/101/111/001/110", ' ' to "000/000/000/000/000", '.' to "000/000/000/000/010",
        ',' to "000/000/000/010/100", '!' to "010/010/010/000/010", '?' to "110/001/010/000/010",
        '-' to "000/000/111/000/000", '_' to "000/000/000/000/111", ':' to "000/010/000/010/000",
        ';' to "000/010/000/010/100", '+' to "000/010/111/010/000", '=' to "000/111/000/111/000",
        '/' to "001/001/010/100/100", '\\' to "100/100/010/001/001", '(' to "001/010/010/010/001",
        ')' to "100/010/010/010/100", '[' to "011/010/010/010/011", ']' to "110/010/010/010/110",
        '\'' to "010/010/000/000/000", '"' to "101/101/000/000/000", '#' to "101/111/101/111/101",
        '*' to "000/101/010/101/000", '%' to "101/001/010/100/101", '&' to "010/101/010/101/011",
        '@' to "111/101/111/100/011", '$' to "011/110/010/011/110", '<' to "001/010/100/010/001",
        '>' to "100/010/001/010/100", '^' to "010/101/000/000/000", '|' to "010/010/010/010/010",
        '~' to "000/011/110/000/000", '`' to "100/010/000/000/000", '{' to "011/010/110/010/011",
        '}' to "110/010/011/010/110"
    )
    fun supports(text: String) = text.all { it.code in 32..126 && alphabet.containsKey(it.uppercaseChar()) }
    fun glyph(char: Char) = (alphabet[char.uppercaseChar()] ?: alphabet.getValue('?')).split('/')
    fun fits(block: PixelText, width: Int, height: Int): Boolean = block.text.isNotBlank() && supports(block.text) &&
        block.x >= 0 && block.y >= 0 && block.x + block.text.length * (block.glyphWidth + 1) - 1 <= width && block.y + block.glyphHeight <= height
}
