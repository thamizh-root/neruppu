package org.havenapp.neruppu.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.havenapp.neruppu.domain.di.TelegramTransport
import org.havenapp.neruppu.domain.repository.TelegramConfigRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import javax.inject.Inject

data class TelegramUiState(
    val botToken: String = "",
    val chatId: String = "",
    val isSaved: Boolean = false,
    val testStatus: TestStatus? = null
)

@HiltViewModel
class TelegramSettingsViewModel @Inject constructor(
    private val configRepository: TelegramConfigRepository,
    @TelegramTransport private val alertTransport: AlertTransport
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TelegramUiState(
            botToken = configRepository.botToken,
            chatId = configRepository.chatId,
            isSaved = configRepository.isComplete
        )
    )
    val uiState: StateFlow<TelegramUiState> = _uiState.asStateFlow()

    fun onTokenChange(value: String) {
        _uiState.update { it.copy(botToken = value) }
    }

    fun onChatIdChange(value: String) {
        _uiState.update { it.copy(chatId = value) }
    }

    fun saveConfig() {
        configRepository.botToken = _uiState.value.botToken
        configRepository.chatId = _uiState.value.chatId
        _uiState.update { it.copy(isSaved = configRepository.isComplete) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(testStatus = null) }
            val result = alertTransport.testConnection()
            _uiState.update { 
                it.copy(
                    testStatus = TestStatus(
                        success = result.isSuccess,
                        error = result.exceptionOrNull()?.message
                    )
                )
            }
        }
    }
}
