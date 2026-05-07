package org.havenapp.neruppu.domain.model

import java.time.Instant

data class Event(
    val id: Long = 0,
    val timestamp: Instant = Instant.now(),
    val sensorType: SensorType,
    val description: String,
    val mediaUri: String? = null,
    val audioUri: String? = null
)

enum class SensorType {
    ACCELEROMETER,
    MICROPHONE,
    LIGHT,
    BAROMETER,
    POWER,
    CAMERA_MOTION
}
