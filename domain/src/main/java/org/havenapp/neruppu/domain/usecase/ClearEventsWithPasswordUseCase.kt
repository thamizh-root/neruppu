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
