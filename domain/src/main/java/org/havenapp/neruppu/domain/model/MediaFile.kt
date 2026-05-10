package org.havenapp.neruppu.domain.model

data class MediaFile(
    val absolutePath: String,
    val mimeType: String,        // "image/jpeg" or "audio/mp4"
    val sizeBytes: Long,
    val timestamp: Long
)
