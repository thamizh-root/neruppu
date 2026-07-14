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

package org.havenapp.neruppu.ui.features.settings

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.havenapp.neruppu.domain.repository.DeletePasswordRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeletePasswordSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val deletePasswordRepository = mockk<DeletePasswordRepository>(relaxed = true)

    private lateinit var viewModel: DeletePasswordSettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { deletePasswordRepository.hasPassword() } returns false
        viewModel = DeletePasswordSettingsViewModel(deletePasswordRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `TC-SET-01 setup password success updates state`() = runTest(testDispatcher) {
        every { deletePasswordRepository.setPassword(any()) } returns Unit

        viewModel.setPassword("new", "new") {}
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasPassword)
        assertEquals("Delete password set", viewModel.uiState.value.successMessage)
        verify { deletePasswordRepository.setPassword("new") }
    }

    @Test
    fun `TC-SET-02 setup password error updates message`() = runTest(testDispatcher) {
        viewModel.setPassword("new", "different") {}
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasPassword)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Passwords do not match") == true)
    }

    @Test
    fun `TC-SET-03 change password success updates state`() = runTest(testDispatcher) {
        every { deletePasswordRepository.hasPassword() } returns true
        every { deletePasswordRepository.verifyPassword("old") } returns true
        every { deletePasswordRepository.setPassword(any()) } returns Unit

        viewModel = DeletePasswordSettingsViewModel(deletePasswordRepository)
        viewModel.changePassword("old", "new", "new") {}
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasPassword)
        assertEquals("Password changed", viewModel.uiState.value.successMessage)
        verify { deletePasswordRepository.setPassword("new") }
    }

    @Test
    fun `TC-SET-04 remove password success clears password state`() = runTest(testDispatcher) {
        every { deletePasswordRepository.hasPassword() } returns true
        every { deletePasswordRepository.verifyPassword("old") } returns true
        every { deletePasswordRepository.removePassword(any()) } returns true

        viewModel = DeletePasswordSettingsViewModel(deletePasswordRepository)
        viewModel.removePassword("old") {}
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasPassword)
        assertEquals("Delete password removed", viewModel.uiState.value.successMessage)
        verify { deletePasswordRepository.removePassword("old") }
    }
}
