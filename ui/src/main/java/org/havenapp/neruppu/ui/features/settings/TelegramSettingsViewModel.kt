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
import org.havenapp.neruppu.domain.repository.MatrixConfigRepository
import org.havenapp.neruppu.domain.repository.AlertTargetRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.model.AlertTarget
import javax.inject.Inject

data class TelegramUiState(
    val botToken: String = "",
    val chatId: String = "",
    override val isSaved: Boolean = false,
    override val isLoading: Boolean = false,
    override val testStatus: TestStatus? = null
) : IntegrationConfigUiState

@HiltViewModel
class TelegramSettingsViewModel @Inject constructor(
    private val configRepository: TelegramConfigRepository,
    private val matrixConfigRepository: MatrixConfigRepository,
    private val alertTargetRepository: AlertTargetRepository,
    @TelegramTransport private val alertTransport: AlertTransport
) : ViewModel(), IntegrationConfigActions {

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

    override fun saveConfig() {
        configRepository.botToken = _uiState.value.botToken
        configRepository.chatId = _uiState.value.chatId
        _uiState.update { it.copy(isSaved = configRepository.isComplete) }
        if (configRepository.isComplete) {
            matrixConfigRepository.clear()
            alertTargetRepository.setActiveTarget(AlertTarget.TELEGRAM)
        }
    }

    override fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, testStatus = null) }
            val result = alertTransport.testConnection()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    testStatus = TestStatus(
                        success = result.isSuccess,
                        error = result.exceptionOrNull()?.message
                    )
                )
            }
        }
    }

    override fun sendMockMessage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, testStatus = null) }
            val payload = AlertPayload.TextAlert(
                sensorType = SensorType.POWER,
                message = "This is a test message from Neruppu to verify your connection.",
                timestamp = System.currentTimeMillis()
            )
            val result = alertTransport.send(payload)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    testStatus = TestStatus(
                        success = result.isSuccess,
                        error = if (result.isSuccess) "Mock message sent!" else result.exceptionOrNull()?.message
                    )
                )
            }
        }
    }
}