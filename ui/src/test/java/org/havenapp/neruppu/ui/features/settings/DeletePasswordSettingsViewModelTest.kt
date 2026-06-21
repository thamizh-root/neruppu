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
import org.havenapp.neruppu.domain.usecase.DeletePasswordResult
import org.havenapp.neruppu.domain.usecase.DeletePasswordUseCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeletePasswordSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val deletePasswordUseCase = mockk<DeletePasswordUseCase>(relaxed = true)

    private lateinit var viewModel: DeletePasswordSettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DeletePasswordSettingsViewModel(deletePasswordUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `TC-SET-01 setup password success updates state`() = runTest(testDispatcher) {
        every { deletePasswordUseCase.setPassword("new", "new") } returns DeletePasswordResult.Success
        every { deletePasswordUseCase.hasPassword() } returns true

        viewModel.onSetupPassword("new", "new")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasPassword)
        assertTrue(viewModel.uiState.value.message is DeletePasswordMessage.Success)
        verify { deletePasswordUseCase.setPassword("new", "new") }
    }

    @Test
    fun `TC-SET-02 setup password error updates message`() = runTest(testDispatcher) {
        every { deletePasswordUseCase.setPassword("new", "different") } returns DeletePasswordResult.Error("Passwords do not match.")
        every { deletePasswordUseCase.hasPassword() } returns false

        viewModel.onSetupPassword("new", "different")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.message is DeletePasswordMessage.Error)
        assertEquals(
            "Passwords do not match.",
            (viewModel.uiState.value.message as DeletePasswordMessage.Error).text
        )
    }

    @Test
    fun `TC-SET-03 change password success updates state`() = runTest(testDispatcher) {
        every { deletePasswordUseCase.changePassword("old", "new", "new") } returns DeletePasswordResult.Success
        every { deletePasswordUseCase.hasPassword() } returns true

        viewModel.onChangePassword("old", "new", "new")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasPassword)
        assertTrue(viewModel.uiState.value.message is DeletePasswordMessage.Success)
        verify { deletePasswordUseCase.changePassword("old", "new", "new") }
    }

    @Test
    fun `TC-SET-04 remove password success clears password state`() = runTest(testDispatcher) {
        every { deletePasswordUseCase.removePassword("old") } returns DeletePasswordResult.Success
        every { deletePasswordUseCase.hasPassword() } returns false

        viewModel.onRemovePassword("old")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasPassword)
        assertTrue(viewModel.uiState.value.message is DeletePasswordMessage.Success)
        verify { deletePasswordUseCase.removePassword("old") }
    }
}
