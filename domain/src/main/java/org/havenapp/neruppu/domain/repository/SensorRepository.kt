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
    suspend fun getLastEventTimestamp(): Long?
}
