package org.havenapp.neruppu.domain.usecase

import org.havenapp.neruppu.domain.repository.DeletePasswordRepository
import javax.inject.Inject

class DeletePasswordUseCase @Inject constructor(
    private val deletePasswordRepository: DeletePasswordRepository
) {

    fun hasPassword(): Boolean = runCatching {
        deletePasswordRepository.hasPassword()
    }.getOrDefault(false)

    fun setPassword(newPassword: String, confirmPassword: String): DeletePasswordResult {
        val validationError = validateNewPassword(newPassword, confirmPassword)
        if (validationError != null) return DeletePasswordResult.Error(validationError)

        return runCatching {
            deletePasswordRepository.setPassword(newPassword)
            DeletePasswordResult.Success
        }.getOrElse {
            DeletePasswordResult.Error("Failed to save delete password.")
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): DeletePasswordResult {
        if (!hasPassword()) {
            return DeletePasswordResult.Error("Delete password is not configured.")
        }
        if (!verifyPassword(currentPassword)) {
            return DeletePasswordResult.Error("Current password is incorrect.")
        }

        val validationError = validateNewPassword(newPassword, confirmPassword)
        if (validationError != null) return DeletePasswordResult.Error(validationError)

        return runCatching {
            deletePasswordRepository.setPassword(newPassword)
            DeletePasswordResult.Success
        }.getOrElse {
            DeletePasswordResult.Error("Failed to save delete password.")
        }
    }

    fun removePassword(currentPassword: String): DeletePasswordResult {
        if (!hasPassword()) {
            return DeletePasswordResult.Error("Delete password is not configured.")
        }
        if (!verifyPassword(currentPassword)) {
            return DeletePasswordResult.Error("Current password is incorrect.")
        }

        return runCatching {
            deletePasswordRepository.removePassword(currentPassword)
            DeletePasswordResult.Success
        }.getOrElse {
            DeletePasswordResult.Error("Failed to remove delete password.")
        }
    }

    private fun verifyPassword(password: String): Boolean = runCatching {
        deletePasswordRepository.verifyPassword(password)
    }.getOrDefault(false)

    private fun validateNewPassword(newPassword: String, confirmPassword: String): String? {
        if (newPassword.isBlank()) {
            return "Password cannot be empty."
        }
        if (newPassword != confirmPassword) {
            return "Passwords do not match."
        }
        return null
    }
}

sealed class DeletePasswordResult {
    object Success : DeletePasswordResult()
    data class Error(val message: String) : DeletePasswordResult()
}
