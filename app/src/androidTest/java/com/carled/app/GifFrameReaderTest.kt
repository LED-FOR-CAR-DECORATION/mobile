package com.carled.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carled.app.data.media.GifFrameReader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GifFrameReaderTest {
    @Test fun importedGifProducesTwoDifferentFramesForLed() {
        val bytes = ("47494638396101000100800000ff00000000ff" +
            "21f904000a0000002c0000000001000100000202440100" +
            "21f904000a0000002c00000000010001000002024c01003b")
            .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val (frames, delay) = GifFrameReader.read(bytes)
        assertEquals(2, frames.size)
        assertEquals(100, delay)
        assertEquals(0xffff0000.toInt(), frames[0].getPixel(0, 0))
        assertEquals(0xff0000ff.toInt(), frames[1].getPixel(0, 0))
    }
}
