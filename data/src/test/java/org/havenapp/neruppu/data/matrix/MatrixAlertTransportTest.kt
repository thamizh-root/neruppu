package org.havenapp.neruppu.data.matrix

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

class MatrixAlertTransportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val apiClient = mockk<MatrixApiClient>()
    private val configStore = mockk<MatrixConfigStore>()
    private lateinit var transport: MatrixAlertTransport

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

        transport = MatrixAlertTransport(apiClient, configStore)
        every { configStore.isComplete } returns true
        
        // Exclude toString from verification to avoid Order failures
        excludeRecords { apiClient.toString() }
    }

    @Test
    fun `TC-MX-01 Text-first alert followed by media`() = runBlocking {
        val file = tempFolder.newFile("test.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        
        val payload = AlertPayload.MediaAlert(
            SensorType.CAMERA_MOTION,
            "Motion",
            MediaFile(file.absolutePath, "image/jpeg", 3L, 1000L),
            1000L
        )
        
        coEvery { apiClient.sendTextEvent(any(), any()) } returns Result.success(Unit)
        coEvery { apiClient.uploadMedia(any(), any(), any()) } returns Result.success("mxc://server/id")
        coEvery { apiClient.sendImageEvent(any(), any(), any(), any()) } returns Result.success(Unit)

        transport.send(payload)

        coVerifyOrder {
            apiClient.sendTextEvent(any(), any())
            apiClient.uploadMedia(match { it.contentEquals(byteArrayOf(1, 2, 3)) }, "image/jpeg", any())
            apiClient.sendImageEvent("mxc://server/id", any(), "image/jpeg", any())
        }
    }

    @Test
    fun `TC-MX-05 Matrix upload failure returns failure`() = runBlocking {
        val file = tempFolder.newFile("test.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        
        val payload = AlertPayload.MediaAlert(
            SensorType.CAMERA_MOTION,
            "Motion",
            MediaFile(file.absolutePath, "image/jpeg", 3L, 1000L),
            1000L
        )
        
        coEvery { apiClient.sendTextEvent(any(), any()) } returns Result.success(Unit)
        coEvery { apiClient.uploadMedia(any(), any(), any()) } returns Result.failure(Exception("Upload failed"))

        val result = transport.send(payload)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { apiClient.sendImageEvent(any(), any(), any(), any()) }
    }

    @Test
    fun `TC-MX-04 Audio alert sends audio event`() = runBlocking {
        val file = tempFolder.newFile("test.mp4")
        file.writeBytes(byteArrayOf(1, 2, 3))
        
        val payload = AlertPayload.MediaAlert(
            SensorType.MICROPHONE,
            "Noise",
            MediaFile(file.absolutePath, "audio/mp4", 3L, 1000L),
            1000L
        )
        
        coEvery { apiClient.sendTextEvent(any(), any()) } returns Result.success(Unit)
        coEvery { apiClient.uploadMedia(any(), any(), any()) } returns Result.success("mxc://server/id")
        coEvery { apiClient.sendAudioEvent(any(), any(), any(), any()) } returns Result.success(Unit)

        transport.send(payload)

        coVerify { apiClient.sendAudioEvent("mxc://server/id", any(), "audio/mp4", any()) }
        coVerify(exactly = 0) { apiClient.sendImageEvent(any(), any(), any(), any()) }
    }

    @Test
    fun `TC-MX-07 testConnection returns success if API succeeds`() = runBlocking {
        coEvery { apiClient.testConnection() } returns Result.success(Unit)
        
        val result = transport.testConnection()
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun `TC-MX-08 Skips dispatch if unconfigured`() = runBlocking {
        every { configStore.isComplete } returns false
        val payload = AlertPayload.TextAlert(SensorType.CAMERA_MOTION, "Motion", 1000L)

        val result = transport.send(payload)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { apiClient.sendTextEvent(any()) }
    }

    @Test
    fun `TC-MX-09 PNG image alert sends image event`() = runBlocking {
        val file = tempFolder.newFile("test.png")
        file.writeBytes(byteArrayOf(1, 2, 3))
        
        val payload = AlertPayload.MediaAlert(
            SensorType.CAMERA_MOTION,
            "Motion",
            MediaFile(file.absolutePath, "image/png", 3L, 1000L),
            1000L
        )
        
        coEvery { apiClient.sendTextEvent(any(), any()) } returns Result.success(Unit)
        coEvery { apiClient.uploadMedia(any(), any(), any()) } returns Result.success("mxc://server/id")
        coEvery { apiClient.sendImageEvent(any(), any(), any(), any()) } returns Result.success(Unit)

        transport.send(payload)

        coVerify { apiClient.sendImageEvent("mxc://server/id", any(), "image/png", any()) }
    }
}
