package org.havenapp.neruppu.data.repository

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.model.AlertTarget
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.model.UploadStatus
import org.havenapp.neruppu.domain.repository.AlertTargetRepository
import org.havenapp.neruppu.domain.repository.MediaUploadRepository
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MediaUploadRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var sensorRepository: SensorRepository
    private lateinit var alertTargetRepository: AlertTargetRepository
    private lateinit var telegramTransport: AlertTransport
    private lateinit var matrixTransport: AlertTransport
    private lateinit var mediaUploadRepository: MediaUploadRepository

    private lateinit var testFile: File

    @Before
    fun setup() {
        sensorRepository = mockk()
        alertTargetRepository = mockk(relaxed = true)
        telegramTransport = mockk()
        matrixTransport = mockk()
        mediaUploadRepository = MediaUploadRepositoryImpl(
            context = mockk(),
            sensorRepository = sensorRepository,
            alertTargetRepository = alertTargetRepository,
            telegramTransport = telegramTransport,
            matrixTransport = matrixTransport
        )
        testFile = tempFolder.newFile("test.jpg")
        testFile.writeBytes(byteArrayOf(1, 2, 3))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `TC-MUP-01 No active target does not upload`() = runBlocking {
        every { alertTargetRepository.activeTarget } returns AlertTarget.NONE
        coEvery { sensorRepository.getPendingUploadEvents(any()) } returns emptyList()

        val result = mediaUploadRepository.uploadPendingEvents()

        assertTrue(result)
        coVerify(exactly = 0) { telegramTransport.send(any()) }
        coVerify(exactly = 0) { matrixTransport.send(any()) }
    }

    @Test
    fun `TC-MUP-02 Telegram success updates status and deletes file`() = runBlocking {
        val event = Event(
            id = 1L,
            sensorType = SensorType.CAMERA_MOTION,
            description = "Motion detected",
            mediaUri = testFile.absolutePath,
            uploadStatus = UploadStatus.PENDING
        )
        every { alertTargetRepository.activeTarget } returns AlertTarget.TELEGRAM
        coEvery { sensorRepository.getPendingUploadEvents(any()) } returns listOf(event)
        val payloadSlot = slot<AlertPayload>()
        coEvery { telegramTransport.send(capture(payloadSlot)) } returns Result.success(Unit)

        coEvery { sensorRepository.updateEventUploadStatus(any(), any(), any(), any(), any()) } just Runs

        val result = mediaUploadRepository.uploadPendingEvents()

        assertTrue(result)
        coVerify { sensorRepository.updateEventUploadStatus(
            eventId = 1L,
            status = UploadStatus.UPLOADED,
            target = "TELEGRAM",
            uploadedAt = any(),
            failureReason = null
        ) }
        assertEquals(testFile.absolutePath, (payloadSlot.captured as AlertPayload.MediaAlert).mediaFile.absolutePath)
        assertFalse(testFile.exists())
    }

    @Test
    fun `TC-MUP-03 Telegram failure keeps file and marks failed`() = runBlocking {
        val event = Event(
            id = 1L,
            sensorType = SensorType.CAMERA_MOTION,
            description = "Motion detected",
            mediaUri = testFile.absolutePath,
            uploadStatus = UploadStatus.PENDING
        )
        every { alertTargetRepository.activeTarget } returns AlertTarget.TELEGRAM
        coEvery { sensorRepository.getPendingUploadEvents(any()) } returns listOf(event)
        coEvery { telegramTransport.send(any()) } returns Result.failure(RuntimeException("Network error"))

        coEvery { sensorRepository.updateEventUploadStatus(any(), any(), any(), any(), any()) } just Runs

        val result = mediaUploadRepository.uploadPendingEvents()

        assertFalse(result)
        coVerify { sensorRepository.updateEventUploadStatus(
            eventId = 1L,
            status = UploadStatus.FAILED,
            target = "TELEGRAM",
            uploadedAt = null,
            failureReason = "Network error"
        ) }
        assertTrue(testFile.exists())
    }

    @Test
    fun `TC-MUP-04 Matrix success uploads via matrix transport`() = runBlocking {
        val event = Event(
            id = 1L,
            sensorType = SensorType.MICROPHONE,
            description = "Sound detected",
            audioUri = testFile.absolutePath,
            uploadStatus = UploadStatus.PENDING
        )
        every { alertTargetRepository.activeTarget } returns AlertTarget.MATRIX
        coEvery { sensorRepository.getPendingUploadEvents(any()) } returns listOf(event)
        coEvery { matrixTransport.send(any()) } returns Result.success(Unit)

        coEvery { sensorRepository.updateEventUploadStatus(any(), any(), any(), any(), any()) } just Runs

        val result = mediaUploadRepository.uploadPendingEvents()

        assertTrue(result)
        coVerify(exactly = 0) { telegramTransport.send(any()) }
        coVerify { sensorRepository.updateEventUploadStatus(
            eventId = 1L,
            status = UploadStatus.UPLOADED,
            target = "MATRIX",
            uploadedAt = any(),
            failureReason = null
        ) }
        assertFalse(testFile.exists())
    }

    @Test
    fun `TC-MUP-05 Text event without media is sent and marked uploaded`() = runBlocking {
        val event = Event(
            id = 1L,
            sensorType = SensorType.ACCELEROMETER,
            description = "Motion detected",
            mediaUri = null,
            audioUri = null,
            uploadStatus = UploadStatus.PENDING
        )
        every { alertTargetRepository.activeTarget } returns AlertTarget.TELEGRAM
        coEvery { sensorRepository.getPendingUploadEvents(any()) } returns listOf(event)
        coEvery { telegramTransport.send(any()) } returns Result.success(Unit)

        coEvery { sensorRepository.updateEventUploadStatus(any(), any(), any(), any(), any()) } just Runs

        val result = mediaUploadRepository.uploadPendingEvents()

        assertTrue(result)
        coVerify { sensorRepository.updateEventUploadStatus(
            eventId = 1L,
            status = UploadStatus.UPLOADED,
            target = "TELEGRAM",
            uploadedAt = any(),
            failureReason = null
        ) }
    }
}