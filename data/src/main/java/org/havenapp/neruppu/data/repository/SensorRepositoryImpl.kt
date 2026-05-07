package org.havenapp.neruppu.data.repository

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
    override fun getEvents(): Flow<List<Event>> {
        return eventDao.getEvents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveEvent(event: Event) {
        eventDao.insertEvent(event.toEntity())
    }

    override suspend fun clearEvents() {
        eventDao.clearEvents()
    }
}
