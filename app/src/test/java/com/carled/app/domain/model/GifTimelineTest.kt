package com.carled.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class GifTimelineTest {
    private fun gif() = ("47494638396101000100800000ff00000000ff" +
        "21f904000a0000002c0000000001000100000202440100" +
        "21f904000a0000002c00000000010001000002024c01003b")
        .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    @Test fun retainsBothFramesAndTheirTiming() {
        val gif = GifTimeline.parse(gif())
        assertEquals(listOf(100, 100), gif.delays)
        assertEquals(listOf(0, 100) to 100, gif.sampling())
    }
    @Test fun boundsOutputButKeepsFullCycleDuration() {
        val timeline = GifTimeline(64, 32, List(100) { 100 })
        val (times, delay) = timeline.sampling()
        assertEquals(16, times.size)
        assertEquals(10_000, times.size * delay)
    }
    @Test fun unequalDelaysRepeatFramesOnUniformTimeline() {
        assertEquals(listOf(0, 100, 200, 300) to 100, GifTimeline(1, 1, listOf(100, 300)).sampling())
    }
    @Test fun truncatedGifDoesNotSilentlyBecomeStatic() {
        assertThrows(IllegalArgumentException::class.java) { GifTimeline.parse(gif().dropLast(3).toByteArray()) }
    }
    @Test fun pngIsNotTreatedAsGif() {
        assertFalse(GifTimeline.isGif(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47)))
    }
}
