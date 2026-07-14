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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.havenapp.neruppu.domain.di.MatrixTransport
import org.havenapp.neruppu.domain.repository.MatrixConfigRepository
import org.havenapp.neruppu.domain.repository.TelegramConfigRepository
import org.havenapp.neruppu.domain.repository.AlertTargetRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.model.AlertTarget
import javax.inject.Inject

data class MatrixUiState(
    val homeserverUrl: String = "",
    val roomId: String = "",
    val accessToken: String = "",
    override val isSaved: Boolean = false,
    override val isLoading: Boolean = false,
    override val testStatus: TestStatus? = null
) : IntegrationConfigUiState

@HiltViewModel
class MatrixSettingsViewModel @Inject constructor(
    private val configRepository: MatrixConfigRepository,
    private val telegramConfigRepository: TelegramConfigRepository,
    private val alertTargetRepository: AlertTargetRepository,
    @MatrixTransport private val alertTransport: AlertTransport
) : ViewModel(), IntegrationConfigActions {

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

    override fun saveConfig() {
        configRepository.homeserverUrl = _uiState.value.homeserverUrl
        configRepository.roomId = _uiState.value.roomId
        configRepository.accessToken = _uiState.value.accessToken
        _uiState.update { it.copy(isSaved = configRepository.isComplete) }
        if (configRepository.isComplete) {
            telegramConfigRepository.clear()
            alertTargetRepository.setActiveTarget(AlertTarget.MATRIX)
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
