package org.havenapp.neruppu.domain.repository

import kotlinx.coroutines.flow.Flow
import org.havenapp.neruppu.domain.model.Event

interface SensorRepository {
    fun getEvents(): Flow<List<Event>>
    suspend fun saveEvent(event: Event)
    suspend fun clearEvents()
}
