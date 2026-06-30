package org.havenapp.neruppu.data.repository

import android.util.Log
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
        Log.d("MediaUploadRepository", "Enqueued upload for event $eventId")
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
            val payload = when {
                event.mediaUri != null -> {
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
                        return@forEach
                    }
                    AlertPayload.MediaAlert(
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
                }
                event.audioUri != null -> {
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
                        return@forEach
                    }
                    AlertPayload.MediaAlert(
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
                }
                else -> {
                    AlertPayload.TextAlert(
                        sensorType = event.sensorType,
                        message = event.description,
                        timestamp = event.timestamp.toEpochMilli()
                    )
                }
            }

            val transport = when (target) {
                AlertTarget.TELEGRAM -> telegramTransport
                AlertTarget.MATRIX -> matrixTransport
                AlertTarget.NONE -> null
            }

            if (transport != null) {
                val result = transport.send(payload)

                if (result.isSuccess) {
                    sensorRepository.updateEventUploadStatus(
                        eventId = event.id,
                        status = UploadStatus.UPLOADED,
                        target = target.name,
                        uploadedAt = System.currentTimeMillis(),
                        failureReason = null
                    )
                    event.mediaUri?.let { deleteMediaFile(it) }
                    event.audioUri?.let { deleteMediaFile(it) }
                } else {
                    hasFailure = true
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    sensorRepository.updateEventUploadStatus(
                        eventId = event.id,
                        status = UploadStatus.FAILED,
                        target = target.name,
                        uploadedAt = null,
                        failureReason = error
                    )
                }
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
            Log.d("MediaUploadRepository", "Media file already missing: $path")
            return
        }

        try {
            if (file.delete()) {
                Log.d("MediaUploadRepository", "Deleted media file: $path")
            } else {
                Log.w("MediaUploadRepository", "Failed to delete media file: $path")
            }
        } catch (e: Exception) {
            Log.w("MediaUploadRepository", "Failed to delete media file: $path", e)
        }
    }
}
