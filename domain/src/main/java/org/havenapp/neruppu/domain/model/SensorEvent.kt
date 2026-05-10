package org.havenapp.neruppu.domain.model

import java.io.File

data class SensorEvent(
    val sensorType: SensorType,
    val description: String,
    val timestamp: Long,
    val imageBytes: ByteArray? = null,
    val audioFile: File? = null
)
