package org.havenapp.neruppu.ui.features.logs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.model.UploadStatus
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class EventItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun TC_LOGS_UI_06_motionEventShowsCorrectLabel() {
        composeTestRule.setContent {
            MaterialTheme {
                EventItem(
                    event = Event(
                        sensorType = SensorType.CAMERA_MOTION,
                        description = "Motion detected"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Motion detected").assertIsDisplayed()
    }

    @Test
    fun TC_LOGS_UI_07_microphoneEventShowsCorrectLabel() {
        composeTestRule.setContent {
            MaterialTheme {
                EventItem(
                    event = Event(
                        sensorType = SensorType.MICROPHONE,
                        description = "Loud noise burst"
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Loud noise burst").assertIsDisplayed()
    }
}
