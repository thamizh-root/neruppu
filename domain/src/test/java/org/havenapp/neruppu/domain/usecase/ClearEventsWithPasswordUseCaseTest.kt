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

package org.havenapp.neruppu.domain.usecase

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.UploadStatus
import org.havenapp.neruppu.domain.repository.DeletePasswordRepository
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearEventsWithPasswordUseCaseTest {

    @Test
    fun `TC-DEL-01 blocks deletion when no delete password is configured`() = runBlocking {
        val passwordRepository = FakeDeletePasswordRepository(configured = false)
        val sensorRepository = FakeSensorRepository()
        val useCase = ClearEventsWithPasswordUseCase(passwordRepository, sensorRepository)

        val result = useCase.execute("any")

        assertTrue(result is ClearEventsResult.MissingPassword)
        assertFalse(sensorRepository.clearCalled)
    }

    @Test
    fun `TC-DEL-02 blocks deletion when password is incorrect`() = runBlocking {
        val passwordRepository = FakeDeletePasswordRepository(configured = true, verifyResult = false)
        val sensorRepository = FakeSensorRepository()
        val useCase = ClearEventsWithPasswordUseCase(passwordRepository, sensorRepository)

        val result = useCase.execute("wrong")

        assertTrue(result is ClearEventsResult.InvalidPassword)
        assertFalse(sensorRepository.clearCalled)
    }

    @Test
    fun `TC-DEL-03 clears events and media when password is valid`() = runBlocking {
        val passwordRepository = FakeDeletePasswordRepository(configured = true)
        val sensorRepository = FakeSensorRepository()
        val useCase = ClearEventsWithPasswordUseCase(passwordRepository, sensorRepository)

        val result = useCase.execute("correct")

        assertTrue(result is ClearEventsResult.Success)
        assertTrue(sensorRepository.clearCalled)
        assertEquals(true, sensorRepository.deleteFiles)
    }

    @Test
    fun `TC-DEL-04 reports password read failure`() = runBlocking {
        val passwordRepository = FailingPasswordRepository
        val sensorRepository = FakeSensorRepository()
        val useCase = ClearEventsWithPasswordUseCase(passwordRepository, sensorRepository)

        val result = useCase.execute("any")

        assertTrue(result is ClearEventsResult.Error)
        assertEquals("Unable to read delete password settings.", (result as ClearEventsResult.Error).message)
        assertFalse(sensorRepository.clearCalled)
    }

    @Test
    fun `TC-DEL-05 reports clear failure`() = runBlocking {
        val passwordRepository = FakeDeletePasswordRepository(configured = true)
        val sensorRepository = FailingSensorRepository
        val useCase = ClearEventsWithPasswordUseCase(passwordRepository, sensorRepository)

        val result = useCase.execute("correct")

        assertTrue(result is ClearEventsResult.Error)
        assertEquals("Failed to clear events.", (result as ClearEventsResult.Error).message)
    }

    private class FakeDeletePasswordRepository(
        private val configured: Boolean,
        private val verifyResult: Boolean = true
    ) : DeletePasswordRepository {

        override fun hasPassword(): Boolean = configured

        override fun setPassword(password: String) = Unit

        override fun verifyPassword(password: String): Boolean = verifyResult

        override fun removePassword(oldPassword: String): Boolean = verifyResult
    }

    private object FailingPasswordRepository : DeletePasswordRepository {
        override fun hasPassword(): Boolean = throw IllegalStateException("read failed")
        override fun setPassword(password: String) = Unit
        override fun verifyPassword(password: String): Boolean = true
        override fun removePassword(oldPassword: String): Boolean = true
    }

    private class FakeSensorRepository : SensorRepository {
        var clearCalled = false
        var deleteFiles: Boolean = false

        override fun getEvents(filter: String): Flow<PagingData<Event>> = flowOf(PagingData.empty())

        override suspend fun saveEvent(event: Event): Long = 1L

        override suspend fun updateEventAudio(eventId: Long, audioUri: String) = Unit

        override suspend fun updateEventUploadStatus(
            eventId: Long,
            status: UploadStatus,
            target: String?,
            uploadedAt: Long?,
            failureReason: String?
        ) = Unit

        override suspend fun clearEvents(deleteFiles: Boolean) {
            clearCalled = true
            this.deleteFiles = deleteFiles
        }

        override suspend fun getPendingUploadEvents(limit: Int): List<Event> = emptyList()
    }

    private object FailingSensorRepository : SensorRepository {
        override fun getEvents(filter: String): Flow<PagingData<Event>> = flowOf(PagingData.empty())
        override suspend fun saveEvent(event: Event): Long = 1L
        override suspend fun updateEventAudio(eventId: Long, audioUri: String) = Unit
        override suspend fun updateEventUploadStatus(
            eventId: Long,
            status: UploadStatus,
            target: String?,
            uploadedAt: Long?,
            failureReason: String?
        ) = Unit

        override suspend fun clearEvents(deleteFiles: Boolean) = throw IllegalStateException("clear failed")

        override suspend fun getPendingUploadEvents(limit: Int): List<Event> = emptyList()
    }
}
