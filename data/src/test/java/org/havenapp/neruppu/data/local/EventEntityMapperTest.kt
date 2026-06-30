package org.havenapp.neruppu.data.local

import org.havenapp.neruppu.data.local.entity.EventEntity
import org.havenapp.neruppu.data.local.entity.toDomain
import org.havenapp.neruppu.data.local.entity.toEntity
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.model.UploadStatus
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class EventEntityMapperTest {

    @Test
    fun `TC-MAP-01 EventEntity toDomain maps all fields including upload metadata`() {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")
        val entity = EventEntity(
            id = 42L,
            timestamp = timestamp,
            sensorType = SensorType.CAMERA_MOTION,
            description = "Motion detected",
            mediaUri = "/path/to/image.jpg",
            audioUri = "/path/to/audio.mp4",
            uploadStatusValue = 2,
            uploadTarget = "TELEGRAM",
            uploadedAt = 1234567890L,
            failureReason = null
        )

        val domain = entity.toDomain()

        assertEquals(42L, domain.id)
        assertEquals(timestamp, domain.timestamp)
        assertEquals(SensorType.CAMERA_MOTION, domain.sensorType)
        assertEquals("Motion detected", domain.description)
        assertEquals("/path/to/image.jpg", domain.mediaUri)
        assertEquals("/path/to/audio.mp4", domain.audioUri)
        assertEquals(UploadStatus.UPLOADED, domain.uploadStatus)
        assertEquals("TELEGRAM", domain.uploadTarget)
        assertEquals(1234567890L, domain.uploadedAt)
        assertNull(domain.failureReason)
    }

    @Test
    fun `TC-MAP-02 Event toEntity maps all fields including upload metadata`() {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")
        val domain = Event(
            id = 42L,
            timestamp = timestamp,
            sensorType = SensorType.LIGHT,
            description = "Light change",
            mediaUri = null,
            audioUri = null,
            uploadStatus = UploadStatus.FAILED,
            uploadTarget = "MATRIX",
            uploadedAt = null,
            failureReason = "Network timeout"
        )

        val entity = domain.toEntity()

        assertEquals(42L, entity.id)
        assertEquals(timestamp, entity.timestamp)
        assertEquals(SensorType.LIGHT, entity.sensorType)
        assertEquals("Light change", entity.description)
        assertNull(entity.mediaUri)
        assertNull(entity.audioUri)
        assertEquals(3, entity.uploadStatusValue)
        assertEquals("MATRIX", entity.uploadTarget)
        assertNull(entity.uploadedAt)
        assertEquals("Network timeout", entity.failureReason)
    }

    @Test
    fun `TC-MAP-03 Default upload fields are PENDING and null`() {
        val domain = Event(
            sensorType = SensorType.MICROPHONE,
            description = "Sound detected"
        )

        assertEquals(UploadStatus.PENDING, domain.uploadStatus)
        assertNull(domain.uploadTarget)
        assertNull(domain.uploadedAt)
        assertNull(domain.failureReason)
    }

    @Test
    fun `TC-MAP-04 UploadStatus integer conversion PENDING`() {
        val entity = EventEntity(
            id = 1L,
            timestamp = Instant.now(),
            sensorType = SensorType.CAMERA_MOTION,
            description = "Test",
            mediaUri = null,
            uploadStatusValue = 1
        )
        assertEquals(UploadStatus.PENDING, entity.uploadStatus)
    }

    @Test
    fun `TC-MAP-05 UploadStatus integer conversion UPLOADED`() {
        val entity = EventEntity(
            id = 1L,
            timestamp = Instant.now(),
            sensorType = SensorType.CAMERA_MOTION,
            description = "Test",
            mediaUri = null,
            uploadStatusValue = 2
        )
        assertEquals(UploadStatus.UPLOADED, entity.uploadStatus)
    }

    @Test
    fun `TC-MAP-06 UploadStatus integer conversion FAILED`() {
        val entity = EventEntity(
            id = 1L,
            timestamp = Instant.now(),
            sensorType = SensorType.CAMERA_MOTION,
            description = "Test",
            mediaUri = null,
            uploadStatusValue = 3
        )
        assertEquals(UploadStatus.FAILED, entity.uploadStatus)
    }

    @Test
    fun `TC-MAP-07 UploadStatus integer conversion invalid defaults to PENDING`() {
        val entity = EventEntity(
            id = 1L,
            timestamp = Instant.now(),
            sensorType = SensorType.CAMERA_MOTION,
            description = "Test",
            mediaUri = null,
            uploadStatusValue = 999
        )
        assertEquals(UploadStatus.PENDING, entity.uploadStatus)
    }
}