package com.carled.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carled.app.core.di.ServiceLocator
import com.carled.app.data.local.TextProfileStore
import com.carled.app.domain.model.*
import com.carled.app.domain.repository.BleDeviceRepository
import com.carled.app.domain.usecase.ble.*
import com.carled.app.presentation.screens.marquee.*
import com.carled.app.presentation.theme.CarLedTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TextProfilesTest {
 @get:Rule val compose=createComposeRule()
 private val context get()=ApplicationProvider.getApplicationContext<Context>()
 private fun profile(id:String)=TextProfile(id,"Profile $id","HELLO LED",2,73,"blink",12,34,56,true)
 private fun cleanup(account:String) { context.getSharedPreferences("mirage_text_profiles",0).edit().remove("account:$account").commit() }
 @Test fun basicQuotaPersistsUpdatesAndSeparatesAccounts() {
  val account=UUID.randomUUID().toString()
  try {
   val store=TextProfileStore(context,account)
   (1..3).forEach { store.save(profile("$it")) }
   assertEquals(3,store.list().size)
   try { store.save(profile("4"));fail("A fourth basic profile must be rejected") } catch(e:IllegalArgumentException) { }
   store.save(profile("1").copy(name="Renamed",text="NEW TEXT"))
   assertEquals(3,store.list().size)
   assertEquals("NEW TEXT",TextProfileStore(context,account).list().first { it.id=="1" }.text)
   assertTrue(TextProfileStore(context,"other-$account").list().isEmpty())
   store.delete("2");store.save(profile("4"));assertEquals(3,store.list().size)
  } finally { cleanup(account) }
 }
 @Test fun verifiedExpandedQuotaHasTenSlotsAndDowngradeKeepsData() {
  val account=UUID.randomUUID().toString()
  var plan=TextProfilePlan.EXPANDED
  try {
   val store=TextProfileStore(context,account,TextProfilePlanProvider { plan })
   (1..10).forEach { store.save(profile("$it")) }
   try { store.save(profile("11"));fail("11th must be rejected") } catch(e:IllegalArgumentException) { }
   plan=TextProfilePlan.BASIC
   assertEquals(10,store.list().size)
   store.save(profile("1").copy(name="Updated after downgrade"))
   assertEquals(10,store.list().size)
   try { store.save(profile("11"));fail("No new entries over quota") } catch(e:IllegalArgumentException) { }
   assertEquals(TextProfilePlan.BASIC,PendingTextProfilePlans.currentPlan())
  } finally { cleanup(account) }
 }
 @Test fun malformedStorageCannotBeOverwrittenAndDuplicateNamesAreRejected() {
  val account=UUID.randomUUID().toString()
  try {
   val store=TextProfileStore(context,account)
   store.save(profile("1"))
   try { store.save(profile("2").copy(name="profile 1"));fail("Duplicate name") } catch(e:IllegalArgumentException) { }
   assertEquals(1,store.list().size)
   val prefs=context.getSharedPreferences("mirage_text_profiles",0)
   prefs.edit().putString("account:$account","corrupt").commit()
   try { store.save(profile("2"));fail("Must preserve unreadable data") } catch(e:Exception) { }
   assertEquals("corrupt",prefs.getString("account:$account",null))
  } finally { cleanup(account) }
 }
 @Test fun offlineUiSavesRestoresUpdatesAndDeletesWithoutSendingToLed() {
  val account=UUID.randomUUID().toString()
  var sends=0
  val device=object:BleDeviceRepository by ServiceLocator.bleDeviceRepository {
   override val connectionState=MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
   override suspend fun uploadProjection(rgb565Data:ByteArray) { sends++ }
   override suspend fun uploadAnimation(rgb565Data:ByteArray,frameCount:Int,frameDelayMs:Int) { sends++ }
   override suspend fun sendMarquee(text:String,r:Int,g:Int,b:Int,speed:Int,size:Int,rainbow:Boolean,scrollMode:String) { sends++ }
  }
  val store=TextProfileStore(context,account)
  val vm=MarqueeViewModel(ServiceLocator.bleUseCases.copy(getConnectionState=GetConnectionStateUseCase(device),uploadProjection=UploadProjectionUseCase(device),sendMarquee=SendMarqueeUseCase(device)),store)
  try {
   compose.waitUntil(5000) { vm.profiles.value.loaded && !vm.profiles.value.busy }
   compose.runOnIdle { vm.updateText("HELLO LED");vm.updateSize(2);vm.updateSpeed(73);vm.updateColor(12,34,56);vm.updateScrollMode("blink") }
   compose.setContent { CarLedTheme { MirageMarqueeScreen(vm,{}) } }
   compose.onNodeWithText("Lưu mới").performScrollTo().performClick()
   compose.onNodeWithText("Tên profile").performTextInput("Đi làm")
   compose.onNodeWithText("Lưu profile").performClick()
   compose.waitUntil(5000) { vm.profiles.value.entries.size==1 && !vm.profiles.value.busy }
   compose.runOnIdle { vm.updateText("OTHER");vm.updateSize(1);vm.updateSpeed(10);vm.updateColor(255,0,0);vm.updateScrollMode("scroll_left") }
   compose.onNodeWithText("Chọn profile").performScrollTo().performClick()
   compose.onNodeWithText("Dùng Đi làm").performClick()
   compose.runOnIdle {
    val s=vm.uiState.value;assertEquals("HELLO LED",s.text);assertEquals(2,s.size);assertEquals(73,s.speed)
    assertEquals("blink",s.scrollMode);assertEquals(12,s.r);assertEquals(34,s.g);assertEquals(56,s.b);assertEquals(0,sends)
    vm.updateText("UPDATED")
   }
   compose.onNodeWithText("Cập nhật profile").performScrollTo().performClick()
   compose.onNodeWithText("Tên profile").performTextClearance();compose.onNodeWithText("Tên profile").performTextInput("Buổi sáng")
   compose.onNodeWithText("Lưu profile").performClick()
   compose.waitUntil(5000) { vm.profiles.value.entries.firstOrNull()?.name=="Buổi sáng" && !vm.profiles.value.busy }
   compose.runOnIdle { assertEquals(1,store.list().size);assertEquals("UPDATED",store.list().single().text);vm.saveProfile("Hai",false) }
   compose.waitUntil(5000) { vm.profiles.value.entries.size==2 && !vm.profiles.value.busy }
   compose.runOnIdle { vm.saveProfile("Ba",false) }
   compose.waitUntil(5000) { vm.profiles.value.entries.size==3 && !vm.profiles.value.busy }
   compose.onNodeWithText("Lưu mới").performScrollTo().assertIsNotEnabled()
   compose.onNodeWithText("Mở rộng lên 10 profile").performScrollTo().performClick()
   compose.onNodeWithText("Đã hiểu").performClick()
   compose.runOnIdle { assertEquals(3,vm.profiles.value.plan.limit) }
   compose.onNodeWithText("Chọn profile").performClick();compose.onNodeWithText("Xóa Ba").performScrollTo().performClick()
   compose.onNodeWithText("Xác nhận xóa").performClick()
   compose.waitUntil(5000) { vm.profiles.value.entries.size==2 && !vm.profiles.value.busy }
   compose.onNodeWithText("Đóng").performClick()
   compose.onNodeWithText("Lưu mới").performScrollTo().assertIsEnabled()
   compose.runOnIdle { assertEquals(0,sends);assertEquals(2,TextProfileStore(context,account).list().size) }
   val directory=File(context.getExternalFilesDir(null),"qa").apply { mkdirs() }
   File(directory,"text-profiles.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
  } finally { cleanup(account) }
 }
}
