package com.carled.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class PixelDocumentTest {
    @Test fun allSupportedCanvasSizesHaveCorrectCellCount() {
        PixelDocument.sizes.forEach { (w, h) -> assertEquals(w * h, PixelDocument(w, h).pixels.size) }
    }
    @Test fun brushIsExactlyOneToFourPixelsWide() {
        (1..4).forEach { size ->
            val result = PixelDocument(32, 32).brush(8, 8, 8, 8, -1, size, MirrorMode.NONE)
            assertEquals(size * size, result.pixels.count { it != 0 })
        }
    }
    @Test fun fastStrokeHasNoGapsAndDoesNotMutatePreviousDocument() {
        val before = PixelDocument(32, 32)
        val after = before.brush(1, 5, 25, 5, -1, 1, MirrorMode.NONE)
        assertTrue(before.pixels.all { it == 0 })
        (1..25).forEach { assertEquals(-1, after.pixels[5 * 32 + it]) }
    }
    @Test fun eraserRemovesColorWithoutTurningItIntoPaintedBlack() {
        val painted = PixelDocument(32, 32).brush(3, 3, 3, 3, 0xff000000.toInt(), 1, MirrorMode.NONE)
        assertNotEquals(0, painted.pixels[99])
        assertEquals(0, painted.brush(3, 3, 3, 3, 0, 1, MirrorMode.NONE).pixels[99])
    }
    @Test fun fillRespectsClosedBoundaryAndFourNeighborConnectivity() {
        val boundary = PixelDocument(32, 32).shape(2, 2, 6, 6, -1, PixelShape.RECTANGLE, false, MirrorMode.NONE)
        val filled = boundary.fill(4, 4, 0xff89c89b.toInt())
        assertEquals(9, filled.pixels.count { it == 0xff89c89b.toInt() })
        assertEquals(-1, filled.pixels[2 * 32 + 4])
        assertEquals(0, filled.pixels[1 * 32 + 4])
    }
    @Test fun mirrorCreatesOriginalAndAllReflections() {
        val d = PixelDocument(64, 32).brush(5, 6, 5, 6, -1, 1, MirrorMode.BOTH)
        listOf(5 to 6, 58 to 6, 5 to 25, 58 to 25).forEach { (x, y) -> assertEquals(-1, d.pixels[y * 64 + x]) }
        assertEquals(4, d.pixels.count { it != 0 })
    }
    @Test fun textIsSeparateUntilCompositedAndEraserCannotChangeIt() {
        val text = PixelText("one", "A", 0, 0, color = -1)
        val d = PixelDocument(32, 32, texts = listOf(text)).brush(0, 0, 4, 4, 0, 4, MirrorMode.NONE)
        assertTrue(d.pixels.all { it == 0 })
        assertEquals(listOf(text), d.texts)
        assertEquals(-1, d.composite()[1])
    }
    @Test fun textRejectsAccentsAndOverflow() {
        assertFalse(PixelFont.supports("Tiếng Việt"))
        assertTrue(PixelFont.supports("Hello, LED! 123"))
        assertFalse(PixelFont.fits(PixelText("one", "HELLO", 20, 0), 32, 32))
        assertTrue(PixelFont.fits(PixelText("one", "LED", 0, 0), 32, 32))
    }
    @Test fun rgb565UsesCorrectLittleEndianChannels() {
        val pixels = List(1024) { when(it) { 0 -> 0xffff0000.toInt(); 1 -> 0xff00ff00.toInt(); 2 -> 0xff0000ff.toInt(); else -> 0 } }
        val bytes = PixelDocument(32, 32, pixels).rgb565()
        assertArrayEquals(byteArrayOf(0, 0xf8.toByte(), 0xe0.toByte(), 7, 31, 0), bytes.take(6).toByteArray())
    }
}
