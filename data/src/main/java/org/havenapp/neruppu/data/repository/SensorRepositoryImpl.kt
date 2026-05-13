package org.havenapp.neruppu.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.Uri
import android.util.Log
import org.havenapp.neruppu.data.local.dao.EventDao
import org.havenapp.neruppu.data.local.entity.toDomain
import org.havenapp.neruppu.data.local.entity.toEntity
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.repository.SensorRepository
import java.io.File

class SensorRepositoryImpl(
    private val context: Context,
    private val eventDao: EventDao
) : SensorRepository {
    override fun getEvents(filter: String): Flow<PagingData<Event>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { eventDao.getEventsPaging(filter) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun saveEvent(event: Event): Long {
        val id = eventDao.insertEvent(event.toEntity())
        Log.d("SensorRepository", "Event saved: ${event.sensorType} with ID $id")
        return id
    }

    override suspend fun updateEventAudio(eventId: Long, audioUri: String) {
        val entity = eventDao.getEventById(eventId)
        if (entity != null) {
            eventDao.updateEvent(entity.copy(audioUri = audioUri))
            Log.d("SensorRepository", "Audio attached to event $eventId: $audioUri")
        } else {
            Log.e("SensorRepository", "Failed to attach audio: Event $eventId not found!")
        }
    }

    override suspend fun clearEvents(deleteFiles: Boolean) = withContext(Dispatchers.IO) {
        if (deleteFiles) {
            // Stream through DB in pages to avoid OOM
            var offset = 0
            val pageSize = 100
            while (true) {
                val page = eventDao.getEventsPage(offset, pageSize)
                if (page.isEmpty()) break
                page.forEach { event ->
                    event.mediaUri?.let { deleteMedia(it) }
                    event.audioUri?.let { deleteMedia(it) }
                }
                offset += pageSize
            }
        }
        eventDao.clearEvents()
    }

    private fun deleteMedia(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content") {
                context.contentResolver.delete(uri, null, null)
            } else if (uri.scheme == "file" || uriString.startsWith("/")) {
                val path = if (uri.scheme == "file") uri.path else uriString
                path?.let { File(it).delete() }
            }
        } catch (e: Exception) {
            // Log or ignore deletion errors to ensure db clearing continues
        }
    }
}
