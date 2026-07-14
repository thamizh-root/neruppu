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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import org.havenapp.neruppu.data.security.PasswordCrypto
import org.havenapp.neruppu.domain.repository.DeletePasswordRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeletePasswordStore @Inject constructor(
    @ApplicationContext private val context: Context
) : DeletePasswordRepository {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "neruppu_delete_password",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun hasPassword(): Boolean {
        return prefs.contains(KEY_HASH)
    }

    override fun setPassword(password: String) {
        require(password.isNotBlank()) { "Delete password cannot be empty" }
        val hash = PasswordCrypto.hashPassword(password)
        prefs.edit().putString(KEY_HASH, hash).apply()
    }

    override fun verifyPassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        return PasswordCrypto.verifyPassword(password, storedHash)
    }

    override fun removePassword(oldPassword: String): Boolean {
        if (!verifyPassword(oldPassword)) return false
        prefs.edit().remove(KEY_HASH).apply()
        return true
    }

    companion object {
        private const val KEY_HASH = "delete_password_hash"
    }
}
