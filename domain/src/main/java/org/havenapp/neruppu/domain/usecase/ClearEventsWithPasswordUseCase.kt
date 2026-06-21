package org.havenapp.neruppu.domain.usecase

import org.havenapp.neruppu.domain.repository.DeletePasswordRepository
import org.havenapp.neruppu.domain.repository.SensorRepository
import javax.inject.Inject

class ClearEventsWithPasswordUseCase @Inject constructor(
    private val deletePasswordRepository: DeletePasswordRepository,
    private val sensorRepository: SensorRepository
) {

    suspend fun execute(password: String): ClearEventsResult {
        val hasPassword = runCatching {
            deletePasswordRepository.hasPassword()
        }.getOrElse {
            return ClearEventsResult.Error("Unable to read delete password settings.")
        }
        if (!hasPassword) {
            return ClearEventsResult.MissingPassword
        }

        val passwordValid = runCatching {
            deletePasswordRepository.verifyPassword(password)
        }.getOrElse {
            return ClearEventsResult.Error("Unable to verify delete password.")
        }
        if (!passwordValid) {
            return ClearEventsResult.InvalidPassword
        }

        return runCatching {
            sensorRepository.clearEvents(deleteFiles = true)
            ClearEventsResult.Success
        }.getOrElse {
            ClearEventsResult.Error("Failed to clear events.")
        }
    }
}

sealed class ClearEventsResult {
    object Success : ClearEventsResult()
    object MissingPassword : ClearEventsResult()
    object InvalidPassword : ClearEventsResult()
    data class Error(val message: String) : ClearEventsResult()
}
