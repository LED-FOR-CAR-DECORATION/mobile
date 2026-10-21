package com.carled.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carled.app.core.di.ServiceLocator
import com.carled.app.domain.model.*
import com.carled.app.presentation.screens.canvas.*
import com.carled.app.presentation.theme.CarLedTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioToolsTest {
    @get:Rule val compose = createComposeRule()
    private fun model() = StudioViewModel(ServiceLocator.bleUseCases, ApplicationProvider.getApplicationContext()).apply {
        live(false); create(64, 32)
    }
    @Test fun toolsSheetCancelsDraftOptionsAndAppliesShapeMirrorAndGridTogether() {
        val vm = model()
        compose.setContent { CarLedTheme { StudioScreen(vm, {}) } }
        compose.onNodeWithTag("studioScroll").performScrollToNode(hasText("Công cụ"))
        compose.onNodeWithText("Công cụ").performScrollTo().assertIsDisplayed().performClick()

        compose.onNodeWithText("Hình dạng", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Tô kín").performScrollTo().performClick()
        compose.onNodeWithText("Đóng công cụ").assertDoesNotExist()
        compose.onNodeWithContentDescription("Đóng công cụ").performClick()
        compose.runOnIdle { assertEquals(PixelTool.PEN, vm.state.value.tool); assertFalse(vm.state.value.filled) }
        compose.onNodeWithTag("studioScroll").performScrollToNode(hasText("Công cụ"))
        compose.onNodeWithText("Công cụ").performScrollTo().assertIsDisplayed().performClick()

        compose.onNodeWithText("Hình dạng", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Tô kín").performScrollTo().performClick()
        compose.onNodeWithText("Hai trục").performScrollTo().performClick()
        compose.onNodeWithText("BỘ CÔNG CỤ VẼ").performScrollTo()
        screenshot("stitch-12-tools")
        compose.onNodeWithTag("applyStudioTools").performClick()
        compose.runOnIdle {
            assertEquals(PixelTool.SHAPE, vm.state.value.tool)
            assertEquals(MirrorMode.BOTH, vm.state.value.mirror)
            assertTrue(vm.state.value.filled)
            vm.down(1, 1); vm.move(3, 3); vm.end()
            assertNotEquals(0, vm.state.value.document!!.pixels[2 * 64 + 2])
            assertNotEquals(0, vm.state.value.document!!.pixels[29 * 64 + 61])
        }
    }
    @Test fun pixelTextHasFullScreenAlignmentNudgingAndNonDestructiveApply() {
        val vm = model()
        compose.runOnIdle { vm.down(0, 0); vm.end() }
        val original = vm.state.value.document!!.pixels
        compose.setContent { CarLedTheme { StudioScreen(vm, {}) } }
        compose.onNodeWithTag("studioScroll").performScrollToNode(hasText("Công cụ"))
        compose.onNodeWithText("Công cụ").performScrollTo().assertIsDisplayed().performClick()

        compose.onNodeWithText("Văn bản").performClick()
        compose.onNodeWithTag("pixelTextScreen").assertExists()
        compose.onNodeWithTag("pixelTextInput").performScrollTo().performTextInput("LED")
        compose.onNodeWithTag("centerPixelText").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Dịch xuống 1 pixel").performScrollTo().performClick()
        screenshot("stitch-13-text-controls")
        compose.onNodeWithText("Màn hình trực quan (64×32 LED)").performScrollTo()
        screenshot("stitch-13-text-preview")
        compose.onNodeWithTag("applyPixelText").performClick()
        compose.runOnIdle {
            val doc = vm.state.value.document!!
            assertEquals(original, doc.pixels)
            assertEquals("LED", doc.texts.single().text)
            assertEquals(26, doc.texts.single().x)
            assertEquals(1, doc.texts.single().y)
            vm.undo()
            assertTrue(vm.state.value.document!!.texts.isEmpty())
            assertEquals(original, vm.state.value.document!!.pixels)
        }
    }
    @Test fun invalidTextCannotApplyAndCancelPreservesExistingBlock() {
        val vm = model()
        compose.runOnIdle { vm.text(PixelText("existing", "LED", 5, 5)) }
        var show by mutableStateOf(true)
        compose.setContent { CarLedTheme { if (show) PixelTextDialog(vm.state.value.document!!, 0xff89c89b.toInt(), vm) { show = false } } }
        compose.onNodeWithTag("pixelTextInput").performScrollTo().performTextReplacement("XE HƠI")
        compose.onNodeWithTag("applyPixelText").assertIsNotEnabled()
        compose.onNodeWithText("Hủy bỏ").performClick()
        compose.runOnIdle { assertEquals("LED", vm.state.value.document!!.texts.single().text) }
    }
    @Test fun textBlockCanBeDraggedWithinCanvasAndApplied() {
        val vm = model()
        compose.runOnIdle { vm.text(PixelText("drag", "LED", 5, 5)) }
        var show by mutableStateOf(true)
        compose.setContent { CarLedTheme { if (show) PixelTextDialog(vm.state.value.document!!, 0xff89c89b.toInt(), vm) { show = false } } }
        compose.onNodeWithTag("textObjectPreview").performScrollTo().performTouchInput {
            swipe(androidx.compose.ui.geometry.Offset(width * 10f / 64, height * 7f / 32),
                androidx.compose.ui.geometry.Offset(width * 30f / 64, height * 17f / 32), 400)
        }
        compose.onNodeWithTag("applyPixelText").performClick()
        compose.runOnIdle {
            val block = vm.state.value.document!!.texts.single()
            assertTrue(block.x > 5); assertTrue(block.y > 5)
            assertTrue(PixelFont.fits(block, 64, 32))
        }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        val tag = if (name == "stitch-12-tools") "studioToolsSheet" else "pixelTextScreen"
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
