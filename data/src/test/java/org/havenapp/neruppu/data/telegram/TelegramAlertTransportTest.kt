package org.havenapp.neruppu.data.telegram

import android.util.Log
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.model.MediaFile
import org.havenapp.neruppu.domain.model.SensorType
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
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

        transport = TelegramAlertTransport(apiClient, configStore)
        every { configStore.isComplete } returns true
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
