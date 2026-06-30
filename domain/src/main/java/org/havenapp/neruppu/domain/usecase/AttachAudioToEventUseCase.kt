package org.havenapp.neruppu.domain.usecase

import org.havenapp.neruppu.domain.repository.MediaStorageRepository
import org.havenapp.neruppu.domain.repository.SensorRepository
import java.io.File
import javax.inject.Inject

class AttachAudioToEventUseCase @Inject constructor(
    private val mediaStorage: MediaStorageRepository,
    private val sensorRepository: SensorRepository
) {
    suspend fun execute(
        eventId: Long,
        tempAudioFile: File,
        timestamp: Long
    ): Result<Unit> = runCatching {
        println("Neruppu: Attaching audio to event $eventId")

        val savedAudio = mediaStorage.saveAudio(tempAudioFile, timestamp).getOrNull()
            ?: error("Failed to save audio file to permanent storage")

        sensorRepository.updateEventAudio(eventId, savedAudio.absolutePath)
        println("Neruppu: Database updated with audio URI for event $eventId")
    }
}
