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

package org.havenapp.neruppu.data.telegram

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import org.havenapp.neruppu.domain.repository.AlertTargetRepository
import org.havenapp.neruppu.domain.repository.TelegramConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramConfigStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertTargetRepository: AlertTargetRepository
) : TelegramConfigRepository {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "neruppu_telegram_config",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override var botToken: String
        get() = prefs.getString(KEY_BOT_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BOT_TOKEN, value).apply()

    override var chatId: String
        get() = prefs.getString(KEY_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CHAT_ID, value).apply()

    override val isComplete: Boolean
        get() {
            val complete = botToken.isNotBlank() && chatId.isNotBlank()
            Log.d("TelegramConfigStore", "isComplete: $complete (Token: ${botToken.isNotBlank()}, ChatID: ${chatId.isNotBlank()})")
            return complete
        }

    override fun clear() {
        prefs.edit().clear().apply()
        alertTargetRepository.clearActiveTarget()
        Log.d("TelegramConfigStore", "Telegram config cleared, target reset to NONE")
    }

    fun setAsActiveTarget() {
        alertTargetRepository.setActiveTarget(org.havenapp.neruppu.domain.model.AlertTarget.TELEGRAM)
    }

    companion object {
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_CHAT_ID = "chat_id"
    }
}
