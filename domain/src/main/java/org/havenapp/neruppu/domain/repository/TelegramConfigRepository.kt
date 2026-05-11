package org.havenapp.neruppu.domain.repository

interface TelegramConfigRepository {
    var botToken: String
    var chatId: String
    val isComplete: Boolean
    fun clear()
}
