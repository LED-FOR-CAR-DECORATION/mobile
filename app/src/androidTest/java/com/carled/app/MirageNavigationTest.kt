package com.carled.app

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carled.app.core.di.ServiceLocator
import com.carled.app.presentation.navigation.AppNavigation
import com.carled.app.presentation.theme.CarLedTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MirageNavigationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun loginDeviceHomeAndNewCanvasRoute() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loggedIn = !ServiceLocator.userPreferences.getAuthToken().isNullOrBlank()
        compose.setContent { CarLedTheme { AppNavigation() } }
        if (!loggedIn) {
            compose.onNodeWithText("Chào mừng trở lại").assertIsDisplayed()
            screenshot(context, "login")
            compose.onNodeWithText("Email").performTextInput("designer@example.com")
            compose.onNodeWithText("Mật khẩu").performTextInput("Preview123!")
            compose.onNodeWithText("Đăng nhập").performScrollTo().performClick()
        }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Thiết bị của tôi").fetchSemanticsNodes().isNotEmpty() }
        screenshot(context, "device-home")
        compose.onNodeWithTag("creationLauncher").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("DIY pixel").assertIsDisplayed()
        compose.onNodeWithText("Chạy chữ").assertIsDisplayed()
        compose.onNodeWithText("Nhập ảnh").assertIsDisplayed()
        screenshot(context, "creation-launcher")
        compose.onNodeWithText("Nhập ảnh").performClick()
        compose.onNodeWithText("Chọn ảnh từ điện thoại").assertIsDisplayed()
        compose.onNodeWithText("Chiếu lên LED").performScrollTo().assertIsNotEnabled()
        screenshot(context, "photo-projection")
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText("Nội dung", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Gallery").assertIsDisplayed()
        compose.onNodeWithText("Inventory").performClick()
        compose.onNodeWithText("Mirage Cat Hero").assertIsDisplayed()
        screenshot(context, "inventory")
        compose.onNodeWithText("Mirage Cat Hero").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Nội dung mô phỏng: chiếu ảnh mẫu tĩnh. File hoạt ảnh và quyền sở hữu thật sẽ lấy từ BE.").fetchSemanticsNodes().isNotEmpty() }
        screenshot(context, "character")
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText("Thiết bị", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("creationLauncher").performScrollTo()
        compose.onNodeWithText("DIY pixel").performClick()
        compose.onNodeWithText("Tạo thiết kế").assertIsDisplayed()
        compose.onNodeWithText("32 × 32").assertExists()
        compose.onNodeWithText("64 × 64").assertExists()
        compose.onNodeWithText("Tạo canvas").performScrollTo().performClick()
        compose.onNodeWithText("Trình vẽ pixel").assertIsDisplayed()
        compose.onNodeWithTag("importFromGallery").performScrollTo().assertIsDisplayed().assertIsEnabled()
    }
    private fun screenshot(context: Context, name: String) {
        compose.waitForIdle()
        val directory = File(context.getExternalFilesDir(null), "qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            requireNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
