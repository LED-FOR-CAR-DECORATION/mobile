package com.carled.app.domain.model
import org.junit.Assert.*
import org.junit.Test
class PhotoRasterizerTest {
 @Test fun containAddsBlackMarginsAndStretchFillsCanvas() {
  val source = IntArray(4) { 0xffff0000.toInt() }
  val contain = PhotoRasterizer.process(source, 2, 2, PhotoFit.CONTAIN)
  assertEquals(0xff000000.toInt(), contain.pixels.first())
  assertEquals(0xffff0000.toInt(), contain.pixels[32])
  assertTrue(PhotoRasterizer.process(source,2,2).pixels.all { it == 0xffff0000.toInt() })
 }
 @Test fun coverCropsAndTransparentIsBlack() {
  val source = intArrayOf(0xffff0000.toInt(),0xff00ff00.toInt(),0xff0000ff.toInt(),0xffffffff.toInt())
  val cover = PhotoRasterizer.process(source,1,4,PhotoFit.COVER)
  assertEquals(0xff00ff00.toInt(),cover.pixels.first())
  assertEquals(0xff0000ff.toInt(),cover.pixels.last())
  assertTrue(PhotoRasterizer.process(intArrayOf(0x00ffffff),1,1).pixels.all { it == 0xff000000.toInt() })
 }
 @Test fun brightnessAndDitherKeepFirmwareDimensions() {
  val normal = PhotoRasterizer.process(intArrayOf(0xff808080.toInt()),1,1)
  val dim = PhotoRasterizer.process(intArrayOf(0xff808080.toInt()),1,1,brightness=50)
  assertEquals(128, normal.pixels.first() and 255)
  assertEquals(64, dim.pixels.first() and 255)
  val dither = PhotoRasterizer.process(intArrayOf(0xff808080.toInt()),1,1,dither=true)
  assertEquals(4096,dither.rgb565().size)
  assertTrue(dither.pixels.distinct().size > 1)
 }
 @Test fun pendingControlsKeepFinalValueForEachControl() {
  val queue=LatestControls<String,Int>()
  queue.offer("brightness",10); queue.offer("color",42); queue.offer("brightness",255)
  assertEquals(255,queue.poll()); assertEquals(42,queue.poll()); assertNull(queue.poll())
  queue.offer("color",7);queue.clear();assertNull(queue.poll())
 }
}
