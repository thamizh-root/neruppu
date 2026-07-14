/*
 * Copyright (C) 2026 thamizh-root
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
