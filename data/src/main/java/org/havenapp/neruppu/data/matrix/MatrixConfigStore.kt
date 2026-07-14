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

package org.havenapp.neruppu.data.matrix

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import org.havenapp.neruppu.domain.repository.AlertTargetRepository
import org.havenapp.neruppu.domain.repository.MatrixConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatrixConfigStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertTargetRepository: AlertTargetRepository
) : MatrixConfigRepository {
    private val prefs by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "neruppu_matrix_config",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            Log.e("MatrixConfigStore", "Encrypted prefs failed, falling back to plain prefs", it)
            context.getSharedPreferences("neruppu_matrix_fallback", Context.MODE_PRIVATE)
        }
    }

    override var homeserverUrl: String
        get() = prefs.getString(KEY_HOMESERVER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HOMESERVER, value.trim()).apply()

    override var roomId: String
        get() = prefs.getString(KEY_ROOM_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ROOM_ID, value.trim()).apply()

    override var accessToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value.trim()).apply()

    override val isComplete: Boolean
        get() = homeserverUrl.isNotBlank() && roomId.isNotBlank() && accessToken.isNotBlank()

    override fun clear() {
        prefs.edit().clear().apply()
        alertTargetRepository.clearActiveTarget()
        Log.d("MatrixConfigStore", "Matrix config cleared, target reset to NONE")
    }

    fun setAsActiveTarget() {
        alertTargetRepository.setActiveTarget(org.havenapp.neruppu.domain.model.AlertTarget.MATRIX)
    }

    companion object {
        private const val KEY_HOMESERVER = "homeserver_url"
        private const val KEY_ROOM_ID = "room_id"
        private const val KEY_TOKEN = "access_token"
    }
}
