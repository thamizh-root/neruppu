package org.havenapp.neruppu.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.havenapp.neruppu.domain.model.Event

interface SensorRepository {
    fun getEvents(): Flow<PagingData<Event>>
    suspend fun saveEvent(event: Event): Long
    suspend fun updateEventAudio(eventId: Long, audioUri: String)
    suspend fun clearEvents()
}
