package org.havenapp.neruppu.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.havenapp.neruppu.domain.repository.DeletePasswordRepository
import javax.inject.Inject

data class DeletePasswordUiState(
    val hasPassword: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DeletePasswordSettingsViewModel @Inject constructor(
    private val deletePasswordRepository: DeletePasswordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DeletePasswordUiState(
            hasPassword = deletePasswordRepository.hasPassword()
        )
    )
    val uiState: StateFlow<DeletePasswordUiState> = _uiState.asStateFlow()

    fun setPassword(newPassword: String, confirmPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            if (newPassword != confirmPassword) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Passwords do not match") }
                onComplete(false)
                return@launch
            }
            if (newPassword.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Password cannot be empty") }
                onComplete(false)
                return@launch
            }
            deletePasswordRepository.setPassword(newPassword)
            _uiState.update { it.copy(isLoading = false, hasPassword = true, successMessage = "Delete password set") }
            onComplete(true)
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            if (!_uiState.value.hasPassword || !deletePasswordRepository.verifyPassword(oldPassword)) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid old password") }
                onComplete(false)
                return@launch
            }
            if (newPassword != confirmPassword) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "New passwords do not match") }
                onComplete(false)
                return@launch
            }
            deletePasswordRepository.setPassword(newPassword)
            _uiState.update { it.copy(isLoading = false, successMessage = "Password changed") }
            onComplete(true)
        }
    }

    fun removePassword(oldPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val success = deletePasswordRepository.removePassword(oldPassword)
            if (success) {
                _uiState.update { it.copy(isLoading = false, hasPassword = false, successMessage = "Delete password removed") }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid password") }
            }
            onComplete(success)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}