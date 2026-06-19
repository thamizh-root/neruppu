package org.havenapp.neruppu.domain.model

import java.time.Instant

enum class UploadStatus {
    PENDING,
    UPLOADED,
    FAILED
}

data class Event(
    val id: Long = 0,
    val timestamp: Instant = Instant.now(),
    val sensorType: SensorType,
    val description: String,
    val mediaUri: String? = null,
    val audioUri: String? = null,
    val uploadStatus: UploadStatus = UploadStatus.PENDING,
    val uploadTarget: String? = null,
    val uploadedAt: Long? = null,
    val failureReason: String? = null
)

enum class SensorType {
    ACCELEROMETER,
    MICROPHONE,
    LIGHT,
    BAROMETER,
    POWER,
    CAMERA_MOTION
}
