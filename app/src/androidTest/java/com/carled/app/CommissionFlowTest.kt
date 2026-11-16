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
        compose.onNodeWithText("Image").performScrollTo().performClick()
        compose.onNodeWithText("GIF").performClick()
        compose.onNodeWithText("Total: 400.000 VND").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Frame count (2–8)").performScrollTo().performTextReplacement("9")
        compose.onNodeWithText("Continue to brief").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Frame count (2–8)").performScrollTo().performTextReplacement("8")
        compose.onNodeWithText("Continue to brief").performScrollTo().performClick()
        compose.onNodeWithText("Continue to payment").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Description").performScrollTo().performTextInput("Animated LED logo")
        compose.onNodeWithText("Continue to payment").performScrollTo().performClick()
        compose.onNodeWithText("Awaiting bank transfer").assertIsDisplayed()
        compose.onNodeWithText("Amount: 400.000 VND").assertIsDisplayed()
        compose.onNodeWithText("Pay later").performClick()
        compose.onNodeWithText("Awaiting payment").assertIsDisplayed()
        compose.onNodeWithText("Animated LED logo").performClick()
        compose.onNodeWithText("Request created").assertIsDisplayed()
        compose.onNodeWithText("Open SePay payment").performClick()
        compose.onNodeWithText("Simulate successful transfer").performClick()
        compose.onNodeWithText("Payment received").assertIsDisplayed()
        compose.onNodeWithText("Open commission chat").performClick()
        compose.onAllNodesWithText("Demo SePay payment received", substring = true)
            .filterToOne(hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithText("Staff: Demo SePay payment received", substring = true).assertIsDisplayed()
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
