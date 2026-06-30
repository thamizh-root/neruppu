package org.havenapp.neruppu.ui.features.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class SettingRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun TC_SETTINGS_UI_03_settingRowLabelIsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                SettingRow(
                    icon = Icons.Default.CameraAlt,
                    label = "Motion detection",
                    subLabel = "CameraX analysis",
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Motion detection").assertIsDisplayed()
        composeTestRule.onNodeWithText("CameraX analysis").assertIsDisplayed()
    }
}
