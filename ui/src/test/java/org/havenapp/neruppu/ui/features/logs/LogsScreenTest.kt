package org.havenapp.neruppu.ui.features.logs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.domain.usecase.ClearEventsWithPasswordUseCase
import org.havenapp.neruppu.domain.usecase.ClearEventsResult
import org.havenapp.neruppu.domain.usecase.HasDeletePasswordUseCase
import org.junit.Rule
import org.junit.Test

class LogsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createFakeViewModel(): LogsViewModel {
        val sensorRepository = mockk<SensorRepository>()
        val hasDeletePasswordUseCase = mockk<HasDeletePasswordUseCase>()
        val clearEventsWithPasswordUseCase = mockk<ClearEventsWithPasswordUseCase>()

        coEvery { sensorRepository.getEvents(any()) } returns flowOf(PagingData.empty())
        every { hasDeletePasswordUseCase.execute() } returns false
        coEvery { clearEventsWithPasswordUseCase.execute(any()) } returns ClearEventsResult.Success

        return LogsViewModel(
            sensorRepository = sensorRepository,
            hasDeletePasswordUseCase = hasDeletePasswordUseCase,
            clearEventsWithPasswordUseCase = clearEventsWithPasswordUseCase
        )
    }

    @Test
    fun TC_LOGS_UI_01_eventsHeaderIsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                LogsScreen(viewModel = createFakeViewModel(), onRequestDelete = {})
            }
        }

        composeTestRule.onNodeWithText("Events").assertIsDisplayed()
    }

    @Test
    fun TC_LOGS_UI_02_deleteButtonIsDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                LogsScreen(viewModel = createFakeViewModel(), onRequestDelete = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Clear all").assertIsDisplayed()
    }

    @Test
    fun TC_LOGS_UI_03_filterChipsAreDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                LogsScreen(viewModel = createFakeViewModel(), onRequestDelete = {})
            }
        }

        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Motion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sound").assertIsDisplayed()
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
    }
}
