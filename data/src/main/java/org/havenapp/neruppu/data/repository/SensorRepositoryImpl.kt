package org.havenapp.neruppu.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.havenapp.neruppu.data.local.dao.EventDao
import org.havenapp.neruppu.data.local.entity.toDomain
import org.havenapp.neruppu.data.local.entity.toEntity
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.repository.SensorRepository

class SensorRepositoryImpl(
    private val eventDao: EventDao
) : SensorRepository {
    override fun getEvents(): Flow<PagingData<Event>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { eventDao.getEventsPaging() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun saveEvent(event: Event): Long {
        return eventDao.insertEvent(event.toEntity())
    }

    override suspend fun updateEventAudio(eventId: Long, audioUri: String) {
        val entity = eventDao.getEventById(eventId)
        if (entity != null) {
            eventDao.updateEvent(entity.copy(audioUri = audioUri))
        }
    }

    override suspend fun clearEvents() {
        eventDao.clearEvents()
    }
}
