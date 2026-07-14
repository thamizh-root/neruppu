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

package org.havenapp.neruppu.data.telegram

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.model.MediaFile
import org.havenapp.neruppu.domain.model.SensorType
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TelegramAlertTransportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val apiClient = mockk<TelegramApiClient>()
    private val configStore = mockk<TelegramConfigStore>()
    private lateinit var transport: TelegramAlertTransport

    @Before
    fun setup() {
        transport = TelegramAlertTransport(apiClient, configStore)
        every { configStore.isComplete } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `TC-TG-01 Text alert is sent correctly`() = runBlocking {
        val payload = AlertPayload.TextAlert(SensorType.CAMERA_MOTION, "Motion detected", 1000L)
        coEvery { apiClient.sendMessage(any()) } returns Result.success(Unit)

        transport.send(payload)

        coVerify { apiClient.sendMessage(match { it.contains("CAMERA_MOTION") && it.contains("Motion detected") }) }
    }

    @Test
    fun `TC-TG-03 Audio alert uses sendVoice`() = runBlocking {
        val file = tempFolder.newFile("test.mp4")
        file.writeBytes(byteArrayOf(1, 2, 3))
        
        val payload = AlertPayload.MediaAlert(
            SensorType.MICROPHONE,
            "Noise",
            MediaFile(file.absolutePath, "audio/mp4", 3L, 1000L),
            1000L
        )
        
        coEvery { apiClient.sendVoice(any(), any()) } returns Result.success(Unit)

        transport.send(payload)

        coVerify { apiClient.sendVoice(byteArrayOf(1, 2, 3), any()) }
        coVerify(exactly = 0) { apiClient.sendPhoto(any(), any()) }
    }

    @Test
    fun `TC-TG-04 Skips dispatch if unconfigured`() = runBlocking {
        every { configStore.isComplete } returns false
        val payload = AlertPayload.TextAlert(SensorType.CAMERA_MOTION, "Motion", 1000L)

        val result = transport.send(payload)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { apiClient.sendMessage(any()) }
    }
}
