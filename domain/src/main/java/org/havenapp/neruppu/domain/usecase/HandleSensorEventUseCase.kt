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
