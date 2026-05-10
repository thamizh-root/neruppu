package org.havenapp.neruppu.domain.transport

import org.havenapp.neruppu.domain.model.AlertPayload

interface AlertTransport {
    val isConfigured: Boolean
    suspend fun send(payload: AlertPayload): Result<Unit>
    suspend fun testConnection(): Result<Unit>
}
