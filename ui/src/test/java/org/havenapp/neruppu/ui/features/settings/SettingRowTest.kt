/*
 * Copyright (C) 2026 thamizh-root
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
