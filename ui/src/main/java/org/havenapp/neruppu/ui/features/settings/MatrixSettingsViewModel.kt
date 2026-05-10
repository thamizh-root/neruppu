package org.havenapp.neruppu.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.havenapp.neruppu.domain.repository.MatrixConfigRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import javax.inject.Inject

data class MatrixUiState(
    val homeserverUrl: String = "",
    val roomId: String = "",
    val accessToken: String = "",
    val isSaved: Boolean = false,
    val testStatus: TestStatus? = null
)

data class TestStatus(
    val success: Boolean,
    val error: String? = null
)

@HiltViewModel
class MatrixSettingsViewModel @Inject constructor(
    private val configRepository: MatrixConfigRepository,
    private val alertTransport: AlertTransport
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MatrixUiState(
            homeserverUrl = configRepository.homeserverUrl,
            roomId = configRepository.roomId,
            accessToken = configRepository.accessToken,
            isSaved = configRepository.isComplete
        )
    )
    val uiState: StateFlow<MatrixUiState> = _uiState.asStateFlow()

    fun onHomeserverChange(value: String) {
        _uiState.update { it.copy(homeserverUrl = value) }
    }

    fun onRoomIdChange(value: String) {
        _uiState.update { it.copy(roomId = value) }
    }

    fun onTokenChange(value: String) {
        _uiState.update { it.copy(accessToken = value) }
    }

    fun saveConfig() {
        configRepository.homeserverUrl = _uiState.value.homeserverUrl
        configRepository.roomId = _uiState.value.roomId
        configRepository.accessToken = _uiState.value.accessToken
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
