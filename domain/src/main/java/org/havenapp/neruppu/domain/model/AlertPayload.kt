package org.havenapp.neruppu.domain.model

sealed class AlertPayload {
    abstract val sensorType: SensorType
    abstract val message: String
    abstract val timestamp: Long

    data class TextAlert(
        override val sensorType: SensorType,
        override val message: String,
        override val timestamp: Long
    ) : AlertPayload()

    data class MediaAlert(
        override val sensorType: SensorType,
        override val message: String,
        val mediaFile: MediaFile,
        override val timestamp: Long
    ) : AlertPayload()
}
