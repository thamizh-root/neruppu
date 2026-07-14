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

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.mockk.every
import io.mockk.mockk
import io.mockk.coEvery
import kotlinx.coroutines.flow.MutableStateFlow
import org.havenapp.neruppu.domain.model.AlertTarget
import org.havenapp.neruppu.domain.repository.AlertTargetRepository
import org.havenapp.neruppu.domain.repository.DeletePasswordRepository
import org.havenapp.neruppu.domain.repository.MatrixConfigRepository
import org.havenapp.neruppu.domain.repository.TelegramConfigRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun TC_SETTINGS_UI_01_settingsHeaderIsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    motionEnabled = true,
                    onMotionToggle = {},
                    soundEnabled = true,
                    onSoundToggle = {},
                    lightEnabled = false,
                    onLightToggle = {},
                    motionSensitivity = 0.5f,
                    onMotionSensitivityChange = {},
                    soundThreshold = 0.6f,
                    onSoundThresholdChange = {},
                    pushAlerts = true,
                    onPushAlertsToggle = {},
                    savePhotos = true,
                    onSavePhotosToggle = {},
                    telegramViewModel = TelegramSettingsViewModel(
                        configRepository = FakeTelegramConfigRepository(),
                        matrixConfigRepository = FakeMatrixConfigRepository(),
                        alertTargetRepository = FakeAlertTargetRepository(),
                        alertTransport = FakeAlertTransport()
                    ),
                    matrixViewModel = MatrixSettingsViewModel(
                        configRepository = FakeMatrixConfigRepository(),
                        telegramConfigRepository = FakeTelegramConfigRepository(),
                        alertTargetRepository = FakeAlertTargetRepository(),
                        alertTransport = FakeAlertTransport()
                    ),
                    deletePasswordViewModel = DeletePasswordSettingsViewModel(
                        deletePasswordRepository = FakeDeletePasswordRepository()
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun TC_SETTINGS_UI_02_motionToggleLabelIsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    motionEnabled = true,
                    onMotionToggle = {},
                    soundEnabled = true,
                    onSoundToggle = {},
                    lightEnabled = false,
                    onLightToggle = {},
                    motionSensitivity = 0.5f,
                    onMotionSensitivityChange = {},
                    soundThreshold = 0.6f,
                    onSoundThresholdChange = {},
                    pushAlerts = true,
                    onPushAlertsToggle = {},
                    savePhotos = true,
                    onSavePhotosToggle = {},
                    telegramViewModel = TelegramSettingsViewModel(
                        configRepository = FakeTelegramConfigRepository(),
                        matrixConfigRepository = FakeMatrixConfigRepository(),
                        alertTargetRepository = FakeAlertTargetRepository(),
                        alertTransport = FakeAlertTransport()
                    ),
                    matrixViewModel = MatrixSettingsViewModel(
                        configRepository = FakeMatrixConfigRepository(),
                        telegramConfigRepository = FakeTelegramConfigRepository(),
                        alertTargetRepository = FakeAlertTargetRepository(),
                        alertTransport = FakeAlertTransport()
                    ),
                    deletePasswordViewModel = DeletePasswordSettingsViewModel(
                        deletePasswordRepository = FakeDeletePasswordRepository()
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Motion detection").assertIsDisplayed()
    }
}

private class FakeTelegramConfigRepository : TelegramConfigRepository {
    override var botToken: String = ""
    override var chatId: String = ""
    override val isComplete: Boolean = false
    override fun clear() {}
}

private class FakeMatrixConfigRepository : MatrixConfigRepository {
    override var homeserverUrl: String = ""
    override var roomId: String = ""
    override var accessToken: String = ""
    override val isComplete: Boolean = false
    override fun clear() {}
}

private class FakeAlertTargetRepository : AlertTargetRepository {
    override val activeTarget: AlertTarget = AlertTarget.NONE
    override fun setActiveTarget(target: AlertTarget) {}
    override fun clearActiveTarget() {}
}

private class FakeAlertTransport : AlertTransport {
    override val isConfigured: Boolean = true
    override suspend fun send(payload: org.havenapp.neruppu.domain.model.AlertPayload): Result<Unit> = Result.success(Unit)
    override suspend fun testConnection(): Result<Unit> = Result.success(Unit)
}

private class FakeDeletePasswordRepository : DeletePasswordRepository {
    override fun hasPassword(): Boolean = false
    override fun setPassword(password: String) {}
    override fun verifyPassword(password: String): Boolean = true
    override fun removePassword(oldPassword: String): Boolean = true
}
