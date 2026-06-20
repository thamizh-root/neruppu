package org.havenapp.neruppu.domain.usecase

import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.MediaFile
import org.havenapp.neruppu.domain.model.SensorEvent
import org.havenapp.neruppu.domain.repository.MediaStorageRepository
import org.havenapp.neruppu.domain.repository.SensorRepository
import java.time.Instant
import javax.inject.Inject

class HandleSensorEventUseCase @Inject constructor(
    private val mediaStorage: MediaStorageRepository,
    private val sensorRepository: SensorRepository
) {
    suspend fun execute(sensorEvent: SensorEvent): Result<Long> = runCatching {
        println("Neruppu: Executing Use Case for ${sensorEvent.sensorType}")

        var savedImage: MediaFile? = null
        var savedAudio: MediaFile? = null

        sensorEvent.imageBytes?.let {
            savedImage = mediaStorage.saveImage(it, sensorEvent.timestamp).getOrNull()
            println("Neruppu: Local image saved: ${savedImage != null}")
        }

        sensorEvent.audioFile?.let {
            savedAudio = mediaStorage.saveAudio(it, sensorEvent.timestamp).getOrNull()
            println("Neruppu: Local audio saved: ${savedAudio != null}")
        }

        val event = Event(
            timestamp = java.time.Instant.ofEpochMilli(sensorEvent.timestamp),
            sensorType = sensorEvent.sensorType,
            description = sensorEvent.description,
            mediaUri = savedImage?.absolutePath,
            audioUri = savedAudio?.absolutePath
        )
        val eventId = sensorRepository.saveEvent(event)
        println("Neruppu: Event saved in database with ID: $eventId")

        eventId
    }
}
