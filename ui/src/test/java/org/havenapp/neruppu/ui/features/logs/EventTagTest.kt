package org.havenapp.neruppu.ui.features.logs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class EventTagTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun TC_LOGS_UI_04_activeEventTagIsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                EventTag(text = "All", active = true, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("All").assertIsDisplayed()
    }

    @Test
    fun TC_LOGS_UI_05_inactiveEventTagIsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                EventTag(text = "Motion", active = false, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Motion").assertIsDisplayed()
    }
}
