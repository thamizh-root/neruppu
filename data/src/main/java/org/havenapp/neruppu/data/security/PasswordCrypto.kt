package org.havenapp.neruppu.data.security

import android.util.Log
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordCrypto {

    private const val TAG = "PasswordCrypto"
    private const val SALT_LENGTH = 16
    private const val HASH_LENGTH = 512
    private const val ITERATIONS = 310000
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val hash = computeHash(password.toCharArray(), salt)
        return encodeStoredValue(salt, hash)
    }

    fun verifyPassword(password: String, storedValue: String?): Boolean {
        if (storedValue == null) return false
        return try {
            val (salt, expectedHash) = decodeStoredValue(storedValue)
            val actualHash = computeHash(password.toCharArray(), salt)
            constantTimeEquals(expectedHash, actualHash)
        } catch (e: Exception) {
            Log.e(TAG, "Password verification failed", e)
            false
        }
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun computeHash(password: CharArray, salt: ByteArray): ByteArray {
        val spec: KeySpec = PBEKeySpec(password, salt, ITERATIONS, HASH_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    private fun encodeStoredValue(salt: ByteArray, hash: ByteArray): String {
        val combined = ByteArray(salt.size + hash.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(hash, 0, combined, salt.size, hash.size)
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    private fun decodeStoredValue(encoded: String): Pair<ByteArray, ByteArray> {
        val combined = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        require(combined.size == SALT_LENGTH + HASH_LENGTH) {
            "Invalid stored password value length"
        }
        val salt = ByteArray(SALT_LENGTH)
        val hash = ByteArray(HASH_LENGTH)
        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH)
        System.arraycopy(combined, SALT_LENGTH, hash, 0, HASH_LENGTH)
        return salt to hash
    }
}
