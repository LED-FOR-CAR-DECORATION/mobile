package com.carled.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carled.app.core.di.ServiceLocator
import com.carled.app.domain.model.*
import com.carled.app.domain.repository.BleDeviceRepository
import com.carled.app.domain.usecase.ble.*
import com.carled.app.presentation.screens.marquee.*
import com.carled.app.presentation.theme.CarLedTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MarqueeFlowTest {
 @get:Rule val compose = createComposeRule()
 private class Device : BleDeviceRepository by ServiceLocator.bleDeviceRepository {
  override val connectionState=MutableStateFlow<ConnectionState>(ConnectionState.Connected("Test LED","test"))
  override val telemetryEvent=MutableSharedFlow<TelemetryEvent>(extraBufferCapacity=8)
  var bytes:ByteArray?=null
  var command:Pair<String,Int>?=null
  override suspend fun uploadProjection(rgb565Data:ByteArray) { bytes=rgb565Data }
  override suspend fun sendMarquee(text:String,r:Int,g:Int,b:Int,speed:Int,size:Int,rainbow:Boolean,scrollMode:String) { command=text to size }
 }
 private fun model(device:Device)=MarqueeViewModel(ServiceLocator.bleUseCases.copy(
  getConnectionState=GetConnectionStateUseCase(device),getTelemetryEvent=GetTelemetryEventUseCase(device),
  uploadProjection=UploadProjectionUseCase(device),sendMarquee=SendMarqueeUseCase(device)))
 @Test fun staticFitsPreviewAndWaitsForProjectionAcknowledgement() {
  val device=Device();val vm=model(device)
  compose.runOnIdle { vm.updateText("XIN NHUONG DUONG - CAM ON!");vm.updateSize(2);vm.updateScrollMode("static") }
  compose.setContent { CarLedTheme { MirageMarqueeScreen(vm,{}) } }
  compose.onNodeWithText("5×7").assertExists();compose.onNodeWithText("10×14").assertExists()
  compose.onNodeWithText("Tự vừa màn hình · 5×7 · 3 dòng · căn giữa").assertExists()
  compose.onNodeWithText("Tốc độ · 60").assertDoesNotExist()
  val context=ApplicationProvider.getApplicationContext<Context>()
  val directory=File(context.getExternalFilesDir(null),"qa").apply { mkdirs() }
  File(directory,"static-text-autofit.png").outputStream().use {
   requireNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG,100,it)
  }
  compose.onNodeWithText("Hiển thị trên LED").performScrollTo().performClick()
  compose.waitUntil(5000) { device.bytes!=null }
  compose.runOnIdle {
   assertEquals(4096,device.bytes!!.size);assertNull(device.command);assertTrue(vm.uiState.value.isSending)
   val expected=MarqueeLayout.fit(vm.uiState.value.text,2).render(0xffffd700.toInt()).rgb565()
   assertArrayEquals(expected,device.bytes)
   device.telemetryEvent.tryEmit(TelemetryEvent.MarqueeActive(vm.uiState.value.text))
  }
  compose.waitForIdle();compose.runOnIdle { assertTrue(vm.uiState.value.isSending);device.telemetryEvent.tryEmit(TelemetryEvent.UploadComplete("ok",6)) }
  compose.waitUntil(5000) { !vm.uiState.value.isSending }
  compose.runOnIdle { assertTrue(vm.uiState.value.feedbackMessage!!.contains("LED đã xác nhận chữ tĩnh")) }
 }
 @Test fun scrollKeepsChosenSizeAndStaticOverflowDoesNotSend() {
  val device=Device();val vm=model(device)
  compose.runOnIdle { vm.updateText("W".repeat(41));vm.updateSize(2);vm.updateScrollMode("static");vm.sendMarquee() }
  compose.runOnIdle { assertNull(device.bytes);assertFalse(vm.uiState.value.isSending);assertNotNull(vm.uiState.value.feedbackMessage)
   vm.updateText("HELLO");vm.updateScrollMode("scroll_left");vm.sendMarquee() }
  compose.waitUntil(5000) { device.command!=null }
  compose.runOnIdle { assertEquals("HELLO" to 2,device.command);assertNull(device.bytes);device.telemetryEvent.tryEmit(TelemetryEvent.MarqueeActive("OTHER")) }
  compose.waitForIdle();compose.runOnIdle { assertTrue(vm.uiState.value.isSending);device.telemetryEvent.tryEmit(TelemetryEvent.MarqueeActive("HELLO")) }
  compose.waitUntil(5000) { !vm.uiState.value.isSending }
 }
 @Test fun staticUploadErrorIsNotReportedAsSuccess() {
  val device=Device();val vm=model(device)
  compose.runOnIdle { vm.updateText("LED");vm.updateScrollMode("static");vm.sendMarquee() }
  compose.waitUntil(5000) { device.bytes!=null }
  compose.runOnIdle { device.telemetryEvent.tryEmit(TelemetryEvent.UploadError("flash_write_fail")) }
  compose.waitUntil(5000) { !vm.uiState.value.isSending }
  compose.runOnIdle { assertTrue(vm.uiState.value.feedbackMessage!!.contains("flash_write_fail"));assertNull(vm.uiState.value.lastEventText) }
 }

}
