package org.havenapp.neruppu.domain.usecase

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.havenapp.neruppu.domain.model.*
import org.havenapp.neruppu.domain.repository.MediaStorageRepository
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HandleSensorEventUseCaseTest {

    private val mediaStorage = mockk<MediaStorageRepository>()
    private val sensorRepository = mockk<SensorRepository>()
    private val transport1 = mockk<AlertTransport>(relaxed = true)
    private val transport2 = mockk<AlertTransport>(relaxed = true)
    
    private lateinit var useCase: HandleSensorEventUseCase

    @Before
    fun setup() {
        useCase = HandleSensorEventUseCase(
            mediaStorage,
            setOf(transport1, transport2),
            sensorRepository
        )
        
        every { transport1.isConfigured } returns true
        every { transport2.isConfigured } returns true
        coEvery { sensorRepository.saveEvent(any()) } returns 1L
    }

    @Test
    fun `TC-ORCH-01 Image is saved locally if present`() = runBlocking {
        val sensorEvent = SensorEvent(
            sensorType = SensorType.CAMERA_MOTION,
            description = "Motion",
            timestamp = 1000L,
            imageBytes = byteArrayOf(1, 2, 3)
        )
        
        val mockMedia = MediaFile("path", "image/jpeg", 3L, 1000L)
        coEvery { mediaStorage.saveImage(any(), any()) } returns Result.success(mockMedia)

        useCase.execute(sensorEvent)

        coVerify { mediaStorage.saveImage(byteArrayOf(1, 2, 3), 1000L) }
    }

    @Test
    fun `TC-ORCH-02 Event is logged in database`() = runBlocking {
        val sensorEvent = SensorEvent(
            sensorType = SensorType.LIGHT,
            description = "Bright",
            timestamp = 1000L
        )

        useCase.execute(sensorEvent)

        coVerify { sensorRepository.saveEvent(match { it.sensorType == SensorType.LIGHT }) }
    }

    @Test
    fun `TC-ORCH-04 All configured transports receive the alert`() = runBlocking {
        val sensorEvent = SensorEvent(SensorType.ACCELEROMETER, "Shake", 1000L)
        
        useCase.execute(sensorEvent)

        coVerify { transport1.send(any()) }
        coVerify { transport2.send(any()) }
    }

    @Test
    fun `TC-ORCH-05 Failure in one transport does not block others`() = runBlocking {
        val sensorEvent = SensorEvent(SensorType.ACCELEROMETER, "Shake", 1000L)
        coEvery { transport1.send(any()) } returns Result.failure(Exception("Fail"))

        val result = useCase.execute(sensorEvent)

        assertTrue(result.isSuccess)
        coVerify { transport2.send(any()) }
    }

    @Test
    fun `TC-ORCH-03 Payload priority Image over Audio`() = runBlocking {
        val sensorEvent = SensorEvent(
            sensorType = SensorType.CAMERA_MOTION,
            description = "Motion",
            timestamp = 1000L,
            imageBytes = byteArrayOf(1),
            audioFile = java.io.File("test.mp4") // Simulating audio present too
        )
        
        coEvery { mediaStorage.saveImage(any(), any()) } returns Result.success(MediaFile("img", "image/jpeg", 1, 1000))
        coEvery { mediaStorage.saveAudio(any(), any()) } returns Result.success(MediaFile("aud", "audio/mp4", 1, 1000))

        useCase.execute(sensorEvent)

        val payloadSlot = slot<AlertPayload>()
        coVerify { transport1.send(capture(payloadSlot)) }
        
        assertTrue(payloadSlot.captured is AlertPayload.MediaAlert)
        assertEquals("image/jpeg", (payloadSlot.captured as AlertPayload.MediaAlert).mediaFile.mimeType)
    }
}
