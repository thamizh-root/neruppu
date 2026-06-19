package org.havenapp.neruppu.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.UploadStatus

interface SensorRepository {
    fun getEvents(filter: String = "All"): Flow<PagingData<Event>>
    suspend fun saveEvent(event: Event): Long
    suspend fun updateEventAudio(eventId: Long, audioUri: String)
    suspend fun updateEventUploadStatus(eventId: Long, status: UploadStatus, target: String?, uploadedAt: Long? = null, failureReason: String? = null)
    suspend fun clearEvents(deleteFiles: Boolean = false)
    suspend fun getPendingUploadEvents(limit: Int = 50): List<Event>
}
