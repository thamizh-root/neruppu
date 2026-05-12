package org.havenapp.neruppu.data.matrix

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import org.havenapp.neruppu.domain.repository.MatrixConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatrixConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
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
        set(value) = prefs.edit().putString(KEY_HOMESERVER, value).apply()

    override var roomId: String
        get() = prefs.getString(KEY_ROOM_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ROOM_ID, value).apply()

    override var accessToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    override val isComplete: Boolean
        get() = homeserverUrl.isNotBlank() && roomId.isNotBlank() && accessToken.isNotBlank()

    override fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_HOMESERVER = "homeserver_url"
        private const val KEY_ROOM_ID = "room_id"
        private const val KEY_TOKEN = "access_token"
    }
}
