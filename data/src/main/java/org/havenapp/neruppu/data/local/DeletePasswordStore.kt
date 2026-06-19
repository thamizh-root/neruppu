package org.havenapp.neruppu.data.local

import android.content.Context
import android.util.Log
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
        runCatching {
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
        }.getOrElse {
            Log.e("DeletePasswordStore", "Encrypted prefs failed, falling back", it)
            context.getSharedPreferences("neruppu_delete_password_fallback", Context.MODE_PRIVATE)
        }
    }

    override fun hasPassword(): Boolean {
        return prefs.contains(KEY_HASH)
    }

    override fun setPassword(password: String) {
        val hash = PasswordCrypto.hashPassword(password)
        prefs.edit().putString(KEY_HASH, hash).apply()
        Log.d("DeletePasswordStore", "Delete password set")
    }

    override fun verifyPassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        val valid = PasswordCrypto.verifyPassword(password, storedHash)
        Log.d("DeletePasswordStore", "Password verification result: $valid")
        return valid
    }

    override fun removePassword(oldPassword: String): Boolean {
        if (!verifyPassword(oldPassword)) return false
        prefs.edit().remove(KEY_HASH).apply()
        Log.d("DeletePasswordStore", "Delete password removed")
        return true
    }

    companion object {
        private const val KEY_HASH = "delete_password_hash"
    }
}
