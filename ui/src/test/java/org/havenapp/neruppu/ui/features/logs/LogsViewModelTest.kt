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

package org.havenapp.neruppu.ui.features.logs

import androidx.paging.PagingData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.domain.usecase.ClearEventsResult
import org.havenapp.neruppu.domain.usecase.ClearEventsWithPasswordUseCase
import org.havenapp.neruppu.domain.usecase.HasDeletePasswordUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val sensorRepository = mockk<SensorRepository>(relaxed = true)
    private val hasDeletePasswordUseCase = mockk<HasDeletePasswordUseCase>()
    private val clearEventsWithPasswordUseCase = mockk<ClearEventsWithPasswordUseCase>()

    private lateinit var viewModel: LogsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { sensorRepository.getEvents(any()) } returns flowOf(PagingData.empty<Event>())
        viewModel = LogsViewModel(
            sensorRepository = sensorRepository,
            hasDeletePasswordUseCase = hasDeletePasswordUseCase,
            clearEventsWithPasswordUseCase = clearEventsWithPasswordUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `TC-LOGS-01 requestDelete opens password dialog when password exists`() = runTest(testDispatcher) {
        every { hasDeletePasswordUseCase.execute() } returns true

        viewModel.requestDelete()

        assertTrue(viewModel.deleteState.value.showPasswordDialog)
        assertFalse(viewModel.deleteState.value.isDeleting)
        assertEquals(null, viewModel.deleteState.value.deleteMessage)
    }

    @Test
    fun `TC-LOGS-02 requestDelete blocks deletion when password is missing`() = runTest(testDispatcher) {
        every { hasDeletePasswordUseCase.execute() } returns false

        viewModel.requestDelete()

        assertFalse(viewModel.deleteState.value.showPasswordDialog)
        assertTrue(viewModel.deleteState.value.deleteMessage is LogsDeleteMessage.Error)
        assertEquals(
            "Set a delete password in Settings before clearing events.",
            (viewModel.deleteState.value.deleteMessage as LogsDeleteMessage.Error).text
        )
    }

    @Test
    fun `TC-LOGS-03 clearLogs completes after valid password`() = runTest(testDispatcher) {
        coEvery { clearEventsWithPasswordUseCase.execute("1234") } returns ClearEventsResult.Success
        viewModel.showPasswordDialog()

        viewModel.clearLogs("1234")

        assertFalse(viewModel.deleteState.value.showPasswordDialog)
        assertTrue(viewModel.deleteState.value.deleteMessage is LogsDeleteMessage.Success)
    }

    @Test
    fun `TC-LOGS-04 clearLogs reports invalid password`() = runTest(testDispatcher) {
        coEvery { clearEventsWithPasswordUseCase.execute("wrong") } returns ClearEventsResult.InvalidPassword

        viewModel.clearLogs("wrong")

        assertFalse(viewModel.deleteState.value.showPasswordDialog)
        assertFalse(viewModel.deleteState.value.isDeleting)
        assertTrue(viewModel.deleteState.value.deleteMessage is LogsDeleteMessage.Error)
        assertEquals(
            "Password is incorrect.",
            (viewModel.deleteState.value.deleteMessage as LogsDeleteMessage.Error).text
        )
    }
}
