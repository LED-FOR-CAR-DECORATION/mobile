package com.carled.app.domain.model

import com.carled.app.data.ble.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.*
import org.junit.Test

class ConfirmedCanvasSenderTest {
 private fun sequence(frame:ByteArray)=((frame[2].toInt() and 255) shl 8) or (frame[3].toInt() and 255)
 @Test fun sendsEveryPixelAcrossTwelvePointBoundary() = runBlocking {
  val events=MutableSharedFlow<TelemetryEvent>(extraBufferCapacity=8)
  val sent=mutableListOf<ByteArray>()
  val sender=ConfirmedCanvasSender(events,{ frame -> sent+=frame;events.tryEmit(TelemetryEvent.CanvasBatchApplied(sequence(frame))) },25,1)
  val points=List(29) { TouchPoint(it,3,10,20,30) }
  sender.send(points)
  assertEquals(listOf(12,12,5),sent.map { it[4].toInt() })
  val received=sent.flatMap { frame ->
   assertEquals(0x1a,frame[1].toInt());assertEquals(7+frame[4]*5,frame.size)
   var checksum=0;for(i in 1 until frame.size-2) checksum=checksum xor (frame[i].toInt() and 255)
   assertEquals(checksum,frame[frame.size-2].toInt() and 255)
   (0 until frame[4].toInt()).map { i -> TouchPoint(frame[5+i*5].toInt(),frame[6+i*5].toInt(),frame[7+i*5].toInt(),frame[8+i*5].toInt(),frame[9+i*5].toInt()) }
  }
  assertEquals(points,received)
 }
 @Test fun lostAcknowledgementRetriesIdenticalAbsoluteWrites() = runBlocking {
  val events=MutableSharedFlow<TelemetryEvent>(extraBufferCapacity=8)
  val sent=mutableListOf<ByteArray>()
  val sender=ConfirmedCanvasSender(events,{ frame ->
   sent+=frame
   if(sent.size==1) events.tryEmit(TelemetryEvent.CanvasBatchApplied((sequence(frame)+1) and 65535))
   else events.tryEmit(TelemetryEvent.CanvasBatchApplied(sequence(frame)))
  },25,1)
  sender.send(listOf(TouchPoint(2,4,0,0,0)))
  assertEquals(2,sent.size);assertArrayEquals(sent[0],sent[1])
 }
 @Test fun neverClaimsSuccessWhenFirmwareDoesNotAcknowledge() = runBlocking {
  val events=MutableSharedFlow<TelemetryEvent>()
  var sends=0
  val sender=ConfirmedCanvasSender(events,{ sends++ },10,1)
  try { sender.send(listOf(TouchPoint(1,1,255,0,0)));fail("Old firmware must not appear synchronized") }
  catch(e:IllegalStateException) { assertTrue(e.message!!.contains("firmware")) }
  assertEquals(3,sends)
 }
 @Test(expected=IllegalArgumentException::class) fun legacyEncoderNeverSilentlyDropsThreePoints() {
  BleProtocolHelper.buildBatchDrawFrame(List(15) { TouchPoint(it,1,0,0,0) })
 }
 @Test fun transportRejectionIsRetriedWithoutAdvancingSequence() = runBlocking {
  val events=MutableSharedFlow<TelemetryEvent>(extraBufferCapacity=8)
  val sent=mutableListOf<ByteArray>()
  val sender=ConfirmedCanvasSender(events,{ frame ->
   sent+=frame
   if(sent.size==1) error("GATT busy")
   events.tryEmit(TelemetryEvent.CanvasBatchApplied(sequence(frame)))
  },20,1)
  sender.send(listOf(TouchPoint(63,31,255,255,255)))
  assertEquals(2,sent.size);assertArrayEquals(sent[0],sent[1])
 }
 @Test fun clearRequiresReceiverConfirmationToo() = runBlocking {
  val events=MutableSharedFlow<TelemetryEvent>(extraBufferCapacity=8)
  val packets=mutableListOf<ByteArray>()
  val sender=ConfirmedCanvasSender(events,{ frame -> packets+=frame;events.tryEmit(TelemetryEvent.CanvasBatchApplied(sequence(frame))) },20,1)
  sender.clear()
  assertEquals(1,packets.size);assertEquals(7,packets.single().size);assertEquals(0,packets.single()[4].toInt())
 }

}
