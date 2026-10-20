package com.carled.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carled.app.core.di.ServiceLocator
import com.carled.app.data.ble.TouchPoint
import com.carled.app.domain.model.*
import com.carled.app.domain.repository.BleDeviceRepository
import com.carled.app.domain.usecase.ble.*
import com.carled.app.presentation.screens.canvas.*
import com.carled.app.presentation.theme.CarLedTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiyLiveTest {
 @get:Rule val compose=createComposeRule()
 private class Device:BleDeviceRepository by ServiceLocator.bleDeviceRepository {
  override val connectionState=MutableStateFlow<ConnectionState>(ConnectionState.Connected("Test LED","test"))
  override val telemetryEvent=MutableSharedFlow<TelemetryEvent>(extraBufferCapacity=8)
  val board=IntArray(2048)
  var clears=0;var batches=0;var uploads=0;var bytes:ByteArray?=null;var fail=false
  override suspend fun clearCanvas() { clears++;board.fill(0) }
  override suspend fun sendBatchDraw(points:List<TouchPoint>) {
   check(!fail) { "write rejected" };assertTrue(points.size<=15);batches++
   points.forEach { board[it.y*64+it.x]=(it.r shl 16) or (it.g shl 8) or it.b }
  }
  override suspend fun uploadProjection(rgb565Data:ByteArray) { uploads++;bytes=rgb565Data }
 }
 private fun model(d:Device)=StudioViewModel(ServiceLocator.bleUseCases.copy(
  getConnectionState=GetConnectionStateUseCase(d),getTelemetryEvent=GetTelemetryEventUseCase(d),
  clearCanvas=ClearCanvasUseCase(d),sendBatchDraw=SendBatchDrawUseCase(d),uploadProjection=UploadProjectionUseCase(d)),ApplicationProvider.getApplicationContext())
 private fun settled(vm:StudioViewModel,d:Device) {
  compose.waitUntil(10000) { d.clears>0 && !vm.state.value.syncing && vm.state.value.document!!.composite().map { it and 0xffffff }.toIntArray().contentEquals(d.board) }
 }
 @Test fun penEraseUndoImportAndClearConvergeLiveWithoutWritingFlash() {
  val d=Device();val vm=model(d)
  compose.runOnIdle { vm.create(64,32);vm.color(0xffff0000.toInt());vm.down(1,1);vm.move(25,1);vm.end() }
  settled(vm,d)
  compose.runOnIdle { assertTrue(d.batches>=2);assertEquals(0,d.uploads);vm.tool(PixelTool.ERASER);vm.down(2,1);vm.end() }
  settled(vm,d);compose.runOnIdle { assertEquals(0,d.board[66]);vm.undo() }
  settled(vm,d);compose.runOnIdle { assertEquals(0xff0000,d.board[66]);vm.redo() }
  settled(vm,d)
  compose.runOnIdle { vm.importPixels(List(2048) { if(it in 100..132) 0xff00ff00.toInt() else 0 });vm.text(PixelText("text","LED",0,0)) }
  settled(vm,d);compose.runOnIdle { vm.clear() }
  settled(vm,d);compose.runOnIdle { assertTrue(d.board.all { it==0 });assertEquals(0,d.uploads);vm.live(false) }
 }
 @Test fun twoSaveActionsAreSeparateAndFlashWaitsForAck() {
  val d=Device();val vm=model(d)
  compose.runOnIdle { vm.create(64,32);vm.down(1,1);vm.end() }
  settled(vm,d)
  compose.setContent { CarLedTheme { StudioScreen(vm,{}) } }
  compose.onNodeWithTag("saveToLed").assertIsDisplayed()
  compose.onNodeWithTag("saveToGallery").assertIsDisplayed().performClick()
  compose.waitUntil(5000) { !vm.state.value.dirty }
  compose.runOnIdle { assertEquals(0,d.uploads) }
  compose.onNodeWithTag("saveToLed").performClick()
  compose.waitUntil(5000) { d.uploads==1 }
  compose.runOnIdle {
   assertTrue(vm.state.value.sending);assertArrayEquals(vm.state.value.document!!.rgb565(),d.bytes)
   val before=vm.state.value.document;vm.down(4,4);vm.move(5,5);vm.end();vm.clear();vm.undo();assertEquals(before,vm.state.value.document)
   d.telemetryEvent.tryEmit(TelemetryEvent.UploadComplete("ok",6))
  }
  compose.waitUntil(5000) { !vm.state.value.sending }
  compose.runOnIdle { assertTrue(vm.state.value.feedback!!.contains("SPIFFS"));vm.live(false) }
 }
 @Test fun failurePausesLiveAndReconnectSynchronizesLatestCanvas() {
  val d=Device();val vm=model(d)
  compose.runOnIdle { vm.create(64,32) };settled(vm,d)
  compose.runOnIdle { d.fail=true;vm.down(3,3);vm.end() }
  compose.waitUntil(5000) { !vm.state.value.liveEnabled }
  compose.runOnIdle { assertNotNull(vm.state.value.liveError);d.fail=false;vm.live(true) };settled(vm,d)
  compose.runOnIdle { d.connectionState.value=ConnectionState.Disconnected }
  compose.waitUntil(5000) { !vm.state.value.connected }
  val batches=d.batches
  compose.runOnIdle { vm.down(6,6);vm.end() }
  compose.waitForIdle();compose.runOnIdle { assertEquals(batches,d.batches);d.connectionState.value=ConnectionState.Connected("Test LED","test") }
  settled(vm,d)
  compose.runOnIdle { assertTrue(d.clears>=3);vm.create(32,32) }
  val after=d.batches;compose.waitForIdle();compose.runOnIdle { assertEquals(after,d.batches);vm.send();assertEquals(0,d.uploads);vm.live(false) }
 }
}
