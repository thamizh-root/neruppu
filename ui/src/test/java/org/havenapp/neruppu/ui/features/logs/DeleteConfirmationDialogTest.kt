package org.havenapp.neruppu.ui.features.logs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class DeleteConfirmationDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun TC_LOGS_UI_08_deleteDialogShowsTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                DeleteConfirmationDialog(
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Clear all events and media?").assertIsDisplayed()
    }

    @Test
    fun TC_LOGS_UI_09_deleteDialogShowsConfirmButton() {
        composeTestRule.setContent {
            MaterialTheme {
                DeleteConfirmationDialog(
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
    }
}
