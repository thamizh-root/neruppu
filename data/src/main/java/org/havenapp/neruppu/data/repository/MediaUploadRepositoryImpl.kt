package org.havenapp.neruppu.data.repository

import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.havenapp.neruppu.data.worker.MediaUploadWorker
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
    private val telegramTransport: AlertTransport,
    private val matrixTransport: org.havenapp.neruppu.domain.transport.AlertTransport
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
            val mediaRef = event.mediaUri ?: event.audioUri
            val mediaFile = mediaRef?.let { path ->
                val file = File(path)
                if (!file.exists()) {
                    sensorRepository.updateEventUploadStatus(
                        eventId = event.id,
                        status = UploadStatus.FAILED,
                        target = target.name,
                        uploadedAt = null,
                        failureReason = "Media file not found: $path"
                    )
                    hasFailure = true
                    return@let null
                }

                MediaFile(
                    absolutePath = path,
                    mimeType = guessMimeType(path),
                    sizeBytes = file.length(),
                    timestamp = event.timestamp.toEpochMilli()
                )
            }

            if (mediaFile == null) {
                return@forEach
            }

            val payload = AlertPayload.MediaAlert(
                sensorType = event.sensorType,
                message = event.description,
                mediaFile = mediaFile,
                timestamp = event.timestamp.toEpochMilli()
            )

            val result = when (target) {
                AlertTarget.TELEGRAM -> telegramTransport.send(payload)
                AlertTarget.MATRIX -> matrixTransport.send(payload)
                AlertTarget.NONE -> Result.success(Unit)
            }

            if (result.isSuccess) {
                sensorRepository.updateEventUploadStatus(
                    eventId = event.id,
                    status = UploadStatus.UPLOADED,
                    target = target.name,
                    uploadedAt = System.currentTimeMillis(),
                    failureReason = null
                )
                deleteMediaFile(mediaFile.absolutePath)
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
