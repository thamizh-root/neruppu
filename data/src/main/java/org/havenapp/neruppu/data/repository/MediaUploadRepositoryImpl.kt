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

package org.havenapp.neruppu.data.repository

import android.util.Log
import org.havenapp.neruppu.data.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.havenapp.neruppu.data.worker.MediaUploadWorker
import org.havenapp.neruppu.domain.di.MatrixTransport
import org.havenapp.neruppu.domain.di.TelegramTransport
import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.model.AlertTarget
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.MediaFile
import org.havenapp.neruppu.domain.model.UploadStatus
import org.havenapp.neruppu.domain.repository.AlertTargetRepository
import org.havenapp.neruppu.domain.repository.MediaUploadRepository
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaUploadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val sensorRepository: SensorRepository,
    private val alertTargetRepository: AlertTargetRepository,
    @TelegramTransport private val telegramTransport: AlertTransport,
    @MatrixTransport private val matrixTransport: AlertTransport
) : MediaUploadRepository {

    override suspend fun enqueueUpload(eventId: Long) {
        MediaUploadWorker.enqueueUpload(context, eventId)
        if (BuildConfig.DEBUG) {
            Log.d("MediaUploadRepository", "Enqueued upload for event $eventId")
        }
    }

    override suspend fun getPendingUploadEvents(limit: Int): List<Event> = withContext(Dispatchers.IO) {
        sensorRepository.getPendingUploadEvents(limit)
    }

    override suspend fun uploadPendingEvents(): Boolean = withContext(Dispatchers.IO) {
        val target = alertTargetRepository.activeTarget
        if (target == AlertTarget.NONE) {
            return@withContext true
        }

        val events = sensorRepository.getPendingUploadEvents(limit = 50)
        var hasFailure = false

        events.forEach { event ->
            val transport = when (target) {
                AlertTarget.TELEGRAM -> telegramTransport
                AlertTarget.MATRIX -> matrixTransport
                AlertTarget.NONE -> null
            }

            if (transport == null) {
                return@forEach
            }

            var uploadSucceeded = true

            // Upload image first (if present)
            if (event.mediaUri != null) {
                val mediaUri = event.mediaUri!!
                val file = File(mediaUri)
                if (!file.exists()) {
                    sensorRepository.updateEventUploadStatus(
                        eventId = event.id,
                        status = UploadStatus.FAILED,
                        target = target.name,
                        uploadedAt = null,
                        failureReason = "Media file not found: $mediaUri"
                    )
                    hasFailure = true
                    uploadSucceeded = false
                } else {
                    val payload = AlertPayload.MediaAlert(
                        sensorType = event.sensorType,
                        message = event.description,
                        mediaFile = MediaFile(
                            absolutePath = mediaUri,
                            mimeType = guessMimeType(mediaUri),
                            sizeBytes = file.length(),
                            timestamp = event.timestamp.toEpochMilli()
                        ),
                        timestamp = event.timestamp.toEpochMilli()
                    )

                    val result = transport.send(payload)
                    if (result.isFailure) {
                        hasFailure = true
                        uploadSucceeded = false
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        sensorRepository.updateEventUploadStatus(
                            eventId = event.id,
                            status = UploadStatus.FAILED,
                            target = target.name,
                            uploadedAt = null,
                            failureReason = "Image upload failed: $error"
                        )
                    } else {
                        deleteMediaFile(mediaUri)
                    }
                }
            }

            // Upload audio (if present) - separate from image
            if (uploadSucceeded && event.audioUri != null) {
                val audioUri = event.audioUri!!
                val file = File(audioUri)
                if (!file.exists()) {
                    sensorRepository.updateEventUploadStatus(
                        eventId = event.id,
                        status = UploadStatus.FAILED,
                        target = target.name,
                        uploadedAt = null,
                        failureReason = "Audio file not found: $audioUri"
                    )
                    hasFailure = true
                    uploadSucceeded = false
                } else {
                    val payload = AlertPayload.MediaAlert(
                        sensorType = event.sensorType,
                        message = event.description,
                        mediaFile = MediaFile(
                            absolutePath = audioUri,
                            mimeType = guessMimeType(audioUri),
                            sizeBytes = file.length(),
                            timestamp = event.timestamp.toEpochMilli()
                        ),
                        timestamp = event.timestamp.toEpochMilli()
                    )

                    val result = transport.send(payload)
                    if (result.isFailure) {
                        hasFailure = true
                        uploadSucceeded = false
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        sensorRepository.updateEventUploadStatus(
                            eventId = event.id,
                            status = UploadStatus.FAILED,
                            target = target.name,
                            uploadedAt = null,
                            failureReason = "Audio upload failed: $error"
                        )
                    } else {
                        deleteMediaFile(audioUri)
                    }
                }
            }

            // If no media, send text alert
            if (uploadSucceeded && event.mediaUri == null && event.audioUri == null) {
                val payload = AlertPayload.TextAlert(
                    sensorType = event.sensorType,
                    message = event.description,
                    timestamp = event.timestamp.toEpochMilli()
                )
                transport.send(payload)
            }

            // Mark as uploaded if all succeeded
            if (uploadSucceeded) {
                sensorRepository.updateEventUploadStatus(
                    eventId = event.id,
                    status = UploadStatus.UPLOADED,
                    target = target.name,
                    uploadedAt = System.currentTimeMillis(),
                    failureReason = null
                )
            }
        }

        !hasFailure
    }

    private fun guessMimeType(path: String): String {
        return when {
            path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            path.endsWith(".png", ignoreCase = true) -> "image/png"
            path.endsWith(".mp4", ignoreCase = true) -> "audio/mp4"
            path.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
            else -> "application/octet-stream"
        }
    }

    private fun deleteMediaFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            if (BuildConfig.DEBUG) {
                Log.d("MediaUploadRepository", "Media file already missing: $path")
            }
            return
        }

        try {
            if (file.delete()) {
                if (BuildConfig.DEBUG) {
                    Log.d("MediaUploadRepository", "Deleted media file: $path")
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.w("MediaUploadRepository", "Failed to delete media file: $path")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w("MediaUploadRepository", "Failed to delete media file: $path", e)
            }
        }
    }
}