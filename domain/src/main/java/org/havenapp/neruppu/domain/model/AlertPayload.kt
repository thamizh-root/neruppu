package org.havenapp.neruppu.domain.model

sealed class AlertPayload {
    data class TextAlert(
        val sensorType: SensorType,
        val message: String,
        val timestamp: Long
    ) : AlertPayload()

    data class MediaAlert(
        val sensorType: SensorType,
        val message: String,
        val mediaFile: MediaFile,
        val timestamp: Long
    ) : AlertPayload()
}
