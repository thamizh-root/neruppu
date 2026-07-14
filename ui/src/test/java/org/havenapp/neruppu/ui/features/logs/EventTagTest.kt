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
