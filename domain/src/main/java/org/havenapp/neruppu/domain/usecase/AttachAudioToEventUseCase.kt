package org.havenapp.neruppu.domain.usecase

import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.repository.MediaStorageRepository
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import java.io.File
import javax.inject.Inject

class AttachAudioToEventUseCase @Inject constructor(
    private val mediaStorage: MediaStorageRepository,
    private val alertTransports: Set<@JvmSuppressWildcards AlertTransport>,
    private val sensorRepository: SensorRepository
) {
    suspend fun execute(
        eventId: Long,
        tempAudioFile: File,
        timestamp: Long
    ): Result<Unit> = runCatching {
        try {
            println("Neruppu: Attaching audio to event $eventId")

            // 1. Move to permanent storage
            val savedAudio = mediaStorage.saveAudio(tempAudioFile, timestamp).getOrNull()
                ?: error("Failed to save audio file to permanent storage")

            // 2. Update database
            sensorRepository.updateEventAudio(eventId, savedAudio.absolutePath)
            println("Neruppu: Database updated with audio URI for event $eventId")

            // 3. Send alerts
            val payload = AlertPayload.MediaAlert(
                sensorType = SensorType.MICROPHONE,
                message = "Acoustic event recording captured",
                mediaFile = savedAudio,
                timestamp = timestamp
            )

            alertTransports.forEach { transport ->
                if (transport.isConfigured) {
                    println("Neruppu: Sending audio alert via ${transport.javaClass.simpleName}")
                    transport.send(payload)
                }
            }
        } finally {
            // 4. Cleanup temporary file to prevent storage leaks
            if (tempAudioFile.exists()) {
                val deleted = tempAudioFile.delete()
                println("Neruppu: Temporary audio file deleted: $deleted")
            }
        }
    }
}
