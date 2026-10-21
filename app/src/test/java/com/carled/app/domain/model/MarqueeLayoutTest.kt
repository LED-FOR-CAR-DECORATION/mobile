package com.carled.app.domain.model
import org.junit.Assert.*
import org.junit.Test
class MarqueeLayoutTest {
 @Test fun largeTextFitsTwoLinesAtRequestedSize() {
  val result=MarqueeLayout.fit("HELLO LED",2)
  assertTrue(result.fits); assertEquals(2,result.scale)
  assertEquals(listOf("HELLO","LED"),result.lines)
 }
 @Test fun longStaticMessageFallsBackAndKeepsEveryWord() {
  val text="XIN NHUONG DUONG - CAM ON!"
  val result=MarqueeLayout.fit(text,2)
  assertTrue(result.fits); assertEquals(1,result.scale)
  assertTrue(result.lines.size <= 4)
  assertEquals(text,result.lines.joinToString(" "))
 }
 @Test fun boundaryFortyCharactersFitsWithoutTruncation() {
  val result=MarqueeLayout.fit("W".repeat(40),2)
  assertTrue(result.fits); assertEquals(1,result.scale)
  assertEquals(4,result.lines.size); assertEquals("W".repeat(40),result.lines.joinToString(""))
  assertFalse(MarqueeLayout.fit("W".repeat(41),2).fits)
 }
 @Test fun wordWrappingUsesAllCellsBeforeReportingOverflow() {
  val text="AAAAAA BBBBBB CCCCCC DDDDDD EEEEEE"
  val result=MarqueeLayout.fit(text,1)
  assertTrue(result.fits);assertEquals(text,result.lines.joinToString(""))
 }
 @Test fun rasterKeepsAllGlyphPixelsAtBothSizes() {
  val color=0xff89c89b.toInt()
  val small=MarqueeLayout.fit("A",1).render(color)
  val large=MarqueeLayout.fit("A",2).render(color)
  val count=(0..4).sumOf { x -> (0..6).count { y -> MarqueeFont.column('A',x) and (1 shl y) != 0 } }
  assertEquals(count,small.pixels.count { it==color })
  assertEquals(count*4,large.pixels.count { it==color })
  assertEquals(4096,large.rgb565().size)
  assertTrue(large.pixels.take(64).all { it==0xff000000.toInt() })
 }
 @Test fun usesRealFontAndHandlesBlankContent() {
  assertNotEquals(MarqueeFont.column('a',0),MarqueeFont.column('A',0))
  assertTrue(MarqueeLayout.fit("   ",2).lines.isEmpty())
  assertEquals("HELLO LED",MarqueeLayout.clean(" HELLO   LED "))
 }
 @Test fun blinkAlternatesCompleteStaticRasterWithBlackWithoutMovingText() {
  for(size in 1..2) for(text in listOf("LED","HELLO LED","XIN NHUONG DUONG - CAM ON!","W".repeat(40))) {
   val layout=MarqueeLayout.fit(text,size)
   val lit=layout.projection(false,0xff89c89b.toInt())
   val blink=layout.projection(true,0xff89c89b.toInt())
   assertEquals(8192,blink.size);assertArrayEquals(lit,blink.copyOfRange(0,4096))
   assertTrue(blink.copyOfRange(4096,8192).all { it==0.toByte() })
  }
 }
 @Test(expected=IllegalArgumentException::class) fun overflowNeverCreatesBlinkAnimation() {
  MarqueeLayout.fit("W".repeat(41),2).projection(true,0xffffffff.toInt())
 }

 @Test fun blinkHeaderDeclaresTwoFramesAndDelay() {
  val header=com.carled.app.data.ble.BleProtocolHelper.buildProjectionStartFrame(2,frameDelayMs=MarqueeLayout.BLINK_FRAME_DELAY_MS)
  assertEquals(8,header.size);assertEquals(2,header[2].toInt());assertEquals(64,header[3].toInt());assertEquals(32,header[4].toInt())
  assertEquals(400,((header[5].toInt() and 255) shl 8) or (header[6].toInt() and 255))
 }

}
