package org.havenapp.neruppu.domain.repository

import org.havenapp.neruppu.domain.model.Event

interface MediaUploadRepository {
    suspend fun enqueueUpload(eventId: Long)
    suspend fun uploadPendingEvents(): Boolean
    suspend fun getPendingUploadEvents(limit: Int = 50): List<Event>
}