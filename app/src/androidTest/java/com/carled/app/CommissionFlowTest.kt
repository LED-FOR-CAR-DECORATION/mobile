package com.carled.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carled.app.domain.model.User
import com.carled.app.presentation.screens.commission.CommissionScreen
import com.carled.app.presentation.theme.CarLedTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommissionFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun submitsGifRequestAndOpensTimeline() {
        openCommission()
        compose.onNodeWithText("Submit request").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Image").performScrollTo().performClick()
        compose.onNodeWithText("GIF").performClick()
        compose.onNodeWithText("Estimated price: $12.00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Description").performScrollTo().performTextInput("Animated LED logo")
        compose.onNodeWithText("Submit request").performScrollTo().performClick()
        compose.onNodeWithText("Animated LED logo").performClick()
        compose.onNodeWithText("Request created").assertIsDisplayed()
    }

    @Test
    fun sendsReplyAndUpdatesInbox() {
        openCommission()
        compose.onNodeWithText("Inbox").performClick()
        compose.onNodeWithText("Staff: Payment received!", substring = true).performClick()
        compose.onNodeWithText("Message").performTextInput("Thanks for the update")
        compose.onNodeWithContentDescription("Send message").performClick()
        compose.onNodeWithText("Thanks for the update").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("You: Thanks for the update").assertIsDisplayed()
    }

    private fun openCommission() {
        compose.setContent {
            CarLedTheme {
                CommissionScreen(User("qa", "qa@example.com", "Commission QA", "Customer"))
            }
        }
    }
}
