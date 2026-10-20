package com.carled.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carled.app.core.di.ServiceLocator
import com.carled.app.presentation.screens.canvas.StudioScreen
import com.carled.app.presentation.screens.canvas.StudioViewModel
import com.carled.app.presentation.theme.CarLedTheme
import android.content.Context
import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.app.Activity
import android.content.Intent
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.CompositionLocalProvider
import com.carled.app.domain.model.PixelText

@RunWith(AndroidJUnit4::class)
class StudioFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun createDrawUndoRedoAndSaveDraftOffline() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = StudioViewModel(ServiceLocator.bleUseCases, context)
        compose.setContent { CarLedTheme { StudioScreen(vm, {}) } }
        compose.onNodeWithText("Tạo canvas").performScrollTo().assertIsDisplayed()
        screenshot(context, "canvas-size")
        compose.onNodeWithText("Tạo canvas").performClick()
        compose.onNodeWithTag("importFromGallery").performScrollTo().assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("pixelCanvas").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("pixelCanvas").performTouchInput {
            down(center); moveBy(androidx.compose.ui.geometry.Offset(40f, 0f)); up()
        }
        compose.runOnIdle {
            assertEquals(64, vm.state.value.document!!.width)
            assertTrue(vm.state.value.document!!.pixels.any { it != 0 })
            assertTrue(vm.state.value.canUndo)
        }
        compose.onNodeWithContentDescription("Hoàn tác").performClick()
        compose.runOnIdle { assertTrue(vm.state.value.document!!.pixels.all { it == 0 }); assertTrue(vm.state.value.canRedo) }
        compose.onNodeWithContentDescription("Làm lại").performClick()
        compose.runOnIdle { assertTrue(vm.state.value.document!!.pixels.any { it != 0 }) }
        screenshot(context, "pixel-editor")
        compose.onNodeWithText("Lưu vào Gallery").performClick()
        compose.waitUntil(10000) { !vm.state.value.dirty }
        compose.onNodeWithText("Đã lưu bản nháp trên điện thoại").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Màu").performScrollTo().performClick()
        compose.onNodeWithText("Màu & bảng màu").assertIsDisplayed()
        compose.onNodeWithText("RGB").performScrollTo().performClick()
        screenshot(context, "color-palettes")
        compose.onNodeWithText("Áp dụng").performClick()
        compose.onNodeWithText("Thêm").performScrollTo().performClick()
        compose.onNodeWithText("Văn bản").performScrollTo().performClick()
        compose.onNodeWithText("Nội dung tiếng Anh").performTextInput("LED")
        screenshot(context, "pixel-text")
        compose.onNodeWithText("Áp dụng").performClick()
        compose.runOnIdle { assertEquals("LED", vm.state.value.document!!.texts.single().text) }
    }
    @Test fun visibleImportActionLoadsGalleryImageAndPreservesText() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "mirage-import-test-${System.nanoTime()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png") }))
        try {
            val image = Bitmap.createBitmap(16, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
            resolver.openOutputStream(uri)!!.use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
            val vm = StudioViewModel(ServiceLocator.bleUseCases, context)
            vm.create(64, 32)
            vm.text(PixelText("label", "LED", 0, 0))
            val registry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                    // A selected photo returned by the system picker; no BLE connection is involved.
                    Handler(Looper.getMainLooper()).post { dispatchResult(requestCode, Activity.RESULT_OK, Intent().setData(uri)) }
                }
            }
            val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
            compose.setContent {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    CarLedTheme { StudioScreen(vm, {}) }
                }
            }
            compose.onNodeWithTag("importFromGallery").performScrollTo().assertIsEnabled().performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Nhập vào canvas").fetchSemanticsNodes().isNotEmpty() }
            compose.runOnIdle { assertTrue(vm.state.value.document!!.pixels.all { it == 0 }) }
            compose.onNodeWithText("Nhập vào canvas").performClick()
            compose.onNodeWithText("Thay lớp pixel?").assertIsDisplayed()
            compose.onNodeWithText("Thay thế").performClick()
            compose.runOnIdle {
                assertEquals(64, vm.state.value.document!!.width)
                assertEquals(32, vm.state.value.document!!.height)
                assertTrue(vm.state.value.document!!.pixels.all { it == android.graphics.Color.RED })
                assertEquals("LED", vm.state.value.document!!.texts.single().text)
                vm.undo()
                assertTrue(vm.state.value.document!!.pixels.all { it == 0 })
                assertEquals("LED", vm.state.value.document!!.texts.single().text)
            }
        } finally { resolver.delete(uri, null, null) }
    }
    @Test fun directPhotoPickerCreatesProjectionWithoutCreatingDraft() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "projection-test.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png") }))
        val before = com.carled.app.data.local.PixelDraftStore(context).drafts().map { it.id }
        try {
            val image = Bitmap.createBitmap(20,20,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
            resolver.openOutputStream(uri)!!.use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
            image.recycle()
            val vm = com.carled.app.presentation.screens.upload.UploadViewModel(ServiceLocator.bleUseCases)
            val registry = object : ActivityResultRegistry() {
                override fun <I,O> onLaunch(requestCode:Int, contract:ActivityResultContract<I,O>, input:I, options:ActivityOptionsCompat?) {
                    Handler(Looper.getMainLooper()).post { dispatchResult(requestCode, Activity.RESULT_OK, Intent().setData(uri)) }
                }
            }
            val owner = object : ActivityResultRegistryOwner { override val activityResultRegistry = registry }
            compose.setContent { CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                CarLedTheme { com.carled.app.presentation.screens.upload.PhotoProjectionScreen(vm, {}, {}) }
            } }
            compose.onNodeWithText("Chọn ảnh từ điện thoại").performClick()
            compose.waitUntil(10000) { vm.uiState.value.previewDocument != null && !vm.uiState.value.isProcessing }
            compose.runOnIdle {
                assertEquals(64,vm.uiState.value.previewDocument!!.width)
                assertEquals(32,vm.uiState.value.previewDocument!!.height)
                assertTrue(vm.uiState.value.previewDocument!!.pixels.all { it == android.graphics.Color.BLUE })
                assertEquals(before,com.carled.app.data.local.PixelDraftStore(context).drafts().map { it.id })
            }
            screenshot(context,"direct-photo-preview")
            compose.onNodeWithText("Tạo canvas").assertDoesNotExist()
            compose.onNodeWithText("Chiếu lên LED").performScrollTo().assertIsNotEnabled()
        } finally { resolver.delete(uri,null,null) }
    }
    @Test fun colorWheelTracksContinuousFingerMovement() {
        val selected = androidx.compose.runtime.mutableIntStateOf(android.graphics.Color.RED)
        var changes = 0
        compose.setContent { CarLedTheme { com.carled.app.presentation.components.LedColorWheel(selected.intValue, { selected.intValue = it; changes++ }) } }
        compose.onNodeWithTag("ledColorWheel").performTouchInput {
            down(center + androidx.compose.ui.geometry.Offset(width * .35f, 0f))
            moveTo(center + androidx.compose.ui.geometry.Offset(0f, height * .35f))
            moveTo(center + androidx.compose.ui.geometry.Offset(-width * .35f, 0f))
            up()
        }
        compose.runOnIdle {
            assertTrue(changes >= 3)
            val hsv = FloatArray(3); android.graphics.Color.colorToHSV(selected.intValue,hsv)
            assertTrue(hsv[0] in 170f..190f)
            assertTrue(hsv[1] > .5f)
        }
        screenshot(ApplicationProvider.getApplicationContext(), "color-wheel")
    }
    private fun screenshot(context: Context, name: String) {
        compose.waitForIdle()
        val directory = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        val bitmap = requireNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
