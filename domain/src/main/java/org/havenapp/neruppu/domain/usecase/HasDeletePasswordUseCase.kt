package org.havenapp.neruppu.domain.usecase

import org.havenapp.neruppu.domain.repository.DeletePasswordRepository
import javax.inject.Inject

class HasDeletePasswordUseCase @Inject constructor(
    private val deletePasswordRepository: DeletePasswordRepository
) {

    fun execute(): Boolean = runCatching {
        deletePasswordRepository.hasPassword()
    }.getOrDefault(false)
}
