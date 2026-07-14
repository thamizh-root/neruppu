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
import org.havenapp.neruppu.data.BuildConfig
import org.havenapp.neruppu.data.local.dao.EventDao
import org.havenapp.neruppu.data.local.entity.toDomain
import org.havenapp.neruppu.data.local.entity.toEntity
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.UploadStatus
import org.havenapp.neruppu.domain.repository.SensorRepository
import java.io.File

class SensorRepositoryImpl(
    private val context: Context,
    private val eventDao: EventDao,
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
        if (BuildConfig.DEBUG) {
            Log.d("SensorRepository", "Event saved: ${event.sensorType} with ID $id")
        }
        return id
    }

    override suspend fun updateEventAudio(eventId: Long, audioUri: String) {
        val entity = eventDao.getEventById(eventId)
        if (entity != null) {
            eventDao.updateEvent(entity.copy(audioUri = audioUri))
            if (BuildConfig.DEBUG) {
                Log.d("SensorRepository", "Audio attached to event $eventId: $audioUri")
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.e("SensorRepository", "Failed to attach audio: Event $eventId not found!")
            }
        }
    }

    override suspend fun updateEventUploadStatus(eventId: Long, status: UploadStatus, target: String?, uploadedAt: Long?, failureReason: String?) {
        val entity = eventDao.getEventById(eventId)
        if (entity != null) {
            eventDao.updateEvent(entity.copy(
                uploadStatusValue = when (status) {
                    UploadStatus.PENDING -> 1
                    UploadStatus.UPLOADED -> 2
                    UploadStatus.FAILED -> 3
                },
                uploadTarget = target,
                uploadedAt = uploadedAt,
                failureReason = failureReason
            ))
            if (BuildConfig.DEBUG) {
                Log.d("SensorRepository", "Upload status updated for event $eventId: $status")
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.e("SensorRepository", "Failed to update upload status: Event $eventId not found!")
            }
        }
    }

    override suspend fun getPendingUploadEvents(limit: Int): List<Event> {
        return eventDao.getPendingUploadEvents(limit).map { it.toDomain() }
    }

    override suspend fun getLastEventTimestamp(): Long? {
        return eventDao.getLastEventTimestamp()
    }

    override suspend fun clearEvents(deleteFiles: Boolean) = withContext(Dispatchers.IO) {
        if (deleteFiles) {
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
            // Ignore deletion errors to ensure db clearing continues
        }
    }
}