package org.havenapp.neruppu.domain.usecase

import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.MediaFile
import org.havenapp.neruppu.domain.model.SensorEvent
import org.havenapp.neruppu.domain.repository.MediaStorageRepository
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import java.time.Instant
import javax.inject.Inject

class HandleSensorEventUseCase @Inject constructor(
    private val mediaStorage: MediaStorageRepository,
    private val alertTransports: Set<@JvmSuppressWildcards AlertTransport>,
    private val sensorRepository: SensorRepository
) {
    suspend fun execute(sensorEvent: SensorEvent): Result<Long> = runCatching {
        // println instead of Log.d because domain is a pure JVM library
        println("Neruppu: Executing Use Case for ${sensorEvent.sensorType}")

        // 1. If event has media, save locally first
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

        // 2. Always log the event in Room
        val event = Event(
            timestamp = java.time.Instant.ofEpochMilli(sensorEvent.timestamp),
            sensorType = sensorEvent.sensorType,
            description = sensorEvent.description,
            mediaUri = savedImage?.absolutePath,
            audioUri = savedAudio?.absolutePath
        )
        val eventId = sensorRepository.saveEvent(event)
        println("Neruppu: Event saved in database with ID: $eventId")

        // 3. Send alerts via all configured transports
        val payload = when {
            savedImage != null -> {
                AlertPayload.MediaAlert(
                    sensorType = sensorEvent.sensorType,
                    message = sensorEvent.description,
                    mediaFile = savedImage!!,
                    timestamp = sensorEvent.timestamp
                )
            }
            savedAudio != null -> {
                AlertPayload.MediaAlert(
                    sensorType = sensorEvent.sensorType,
                    message = sensorEvent.description,
                    mediaFile = savedAudio!!,
                    timestamp = sensorEvent.timestamp
                )
            }
            else -> {
                AlertPayload.TextAlert(
                    sensorType = sensorEvent.sensorType,
                    message = sensorEvent.description,
                    timestamp = sensorEvent.timestamp
                )
            }
        }
        
        alertTransports.forEach { transport ->
            if (transport.isConfigured) {
                println("Neruppu: Sending alert via ${transport.javaClass.simpleName}")
                // Fire and forget push
                transport.send(payload)
            }
        }
        
        eventId
    }
}
