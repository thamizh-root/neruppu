package org.havenapp.neruppu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.model.UploadStatus
import java.time.Instant

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val sensorType: SensorType,
    val description: String,
    val mediaUri: String?,
    val audioUri: String? = null,
    val uploadStatusValue: Int = 1,
    val uploadTarget: String? = null,
    val uploadedAt: Long? = null,
    val failureReason: String? = null
) {
    val uploadStatus: UploadStatus get() = when (uploadStatusValue) {
        1 -> UploadStatus.PENDING
        2 -> UploadStatus.UPLOADED
        3 -> UploadStatus.FAILED
        else -> UploadStatus.PENDING
    }
}

fun EventEntity.toDomain(): Event = Event(
    id = id,
    timestamp = timestamp,
    sensorType = sensorType,
    description = description,
    mediaUri = mediaUri,
    audioUri = audioUri,
    uploadStatus = uploadStatus,
    uploadTarget = uploadTarget,
    uploadedAt = uploadedAt,
    failureReason = failureReason
)

fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
    timestamp = timestamp,
    sensorType = sensorType,
    description = description,
    mediaUri = mediaUri,
    audioUri = audioUri,
    uploadStatusValue = when (uploadStatus) {
        UploadStatus.PENDING -> 1
        UploadStatus.UPLOADED -> 2
        UploadStatus.FAILED -> 3
    },
    uploadTarget = uploadTarget,
    uploadedAt = uploadedAt,
    failureReason = failureReason
)
