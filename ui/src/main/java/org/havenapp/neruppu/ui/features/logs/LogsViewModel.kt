package org.havenapp.neruppu.ui.features.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.domain.usecase.ClearEventsResult
import org.havenapp.neruppu.domain.usecase.ClearEventsWithPasswordUseCase
import org.havenapp.neruppu.domain.usecase.HasDeletePasswordUseCase
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val hasDeletePasswordUseCase: HasDeletePasswordUseCase,
    private val clearEventsWithPasswordUseCase: ClearEventsWithPasswordUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow("All")
    val filter: StateFlow<String> = _filter.asStateFlow()

    private val _deleteState = MutableStateFlow(LogsDeleteUiState())
    val deleteState: StateFlow<LogsDeleteUiState> = _deleteState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val events: Flow<PagingData<Event>> = _filter.flatMapLatest { filter ->
        sensorRepository.getEvents(filter)
    }.cachedIn(viewModelScope)

    fun setFilter(filter: String) {
        _filter.value = filter
    }

    fun requestDelete() {
        viewModelScope.launch {
            val hasPassword = hasDeletePasswordUseCase.execute()
            _deleteState.value = if (hasPassword) {
                LogsDeleteUiState(showPasswordDialog = true, deleteMessage = null)
            } else {
                LogsDeleteUiState(
                    showPasswordDialog = false,
                    deleteMessage = LogsDeleteMessage.Error(
                        "Set a delete password in Settings before clearing events."
                    )
                )
            }
        }
    }

    fun showPasswordDialog() {
        _deleteState.value = LogsDeleteUiState(showPasswordDialog = true, deleteMessage = null)
    }

    fun clearLogs(password: String) {
        viewModelScope.launch {
            _deleteState.value = _deleteState.value.copy(
                isDeleting = true,
                showPasswordDialog = false,
                deleteMessage = null
            )

            val result = clearEventsWithPasswordUseCase.execute(password)
            _deleteState.value = _deleteState.value.copy(
                isDeleting = false,
                deleteMessage = when (result) {
                    ClearEventsResult.Success -> LogsDeleteMessage.Success
                    ClearEventsResult.MissingPassword -> LogsDeleteMessage.Error(
                        "Set a delete password in Settings before clearing events."
                    )
                    ClearEventsResult.InvalidPassword -> LogsDeleteMessage.Error(
                        "Password is incorrect."
                    )
                    is ClearEventsResult.Error -> LogsDeleteMessage.Error(result.message)
                }
            )
        }
    }

    fun hidePasswordDialog() {
        _deleteState.value = _deleteState.value.copy(showPasswordDialog = false)
    }

    fun clearDeleteMessage() {
        _deleteState.value = _deleteState.value.copy(deleteMessage = null)
    }
}

data class LogsDeleteUiState(
    val showPasswordDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteMessage: LogsDeleteMessage? = null
)

sealed class LogsDeleteMessage {
    object Success : LogsDeleteMessage()
    data class Error(val text: String) : LogsDeleteMessage()
}
