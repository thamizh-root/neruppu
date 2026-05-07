package org.havenapp.neruppu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import java.time.Instant

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val sensorType: SensorType,
    val description: String,
    val mediaUri: String?,
    val audioUri: String? = null
)

fun EventEntity.toDomain(): Event = Event(
    id = id,
    timestamp = timestamp,
    sensorType = sensorType,
    description = description,
    mediaUri = mediaUri,
    audioUri = audioUri
)

fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
    timestamp = timestamp,
    sensorType = sensorType,
    description = description,
    mediaUri = mediaUri,
    audioUri = audioUri
)
