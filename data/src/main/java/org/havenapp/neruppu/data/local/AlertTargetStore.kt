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

package org.havenapp.neruppu.data.local

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import org.havenapp.neruppu.domain.model.AlertTarget
import org.havenapp.neruppu.domain.repository.AlertTargetRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertTargetStore @Inject constructor(
    @ApplicationContext private val context: Context
) : AlertTargetRepository {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "neruppu_alert_target",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override val activeTarget: AlertTarget
        get() {
            val value = prefs.getString(KEY_TARGET, "NONE") ?: "NONE"
            return when (value) {
                "TELEGRAM" -> AlertTarget.TELEGRAM
                "MATRIX" -> AlertTarget.MATRIX
                else -> AlertTarget.NONE
            }
        }

    override fun setActiveTarget(target: AlertTarget) {
        prefs.edit().putString(KEY_TARGET, target.name).apply()
        Log.d("AlertTargetStore", "Active target set to: $target")
    }

    override fun clearActiveTarget() {
        prefs.edit().remove(KEY_TARGET).apply()
        Log.d("AlertTargetStore", "Active target cleared")
    }

    companion object {
        private const val KEY_TARGET = "active_target"
    }
}