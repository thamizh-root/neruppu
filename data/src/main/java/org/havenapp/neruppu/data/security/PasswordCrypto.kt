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

package org.havenapp.neruppu.data.security

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordCrypto {

    private const val SALT_LENGTH_BYTES = 16
    private const val HASH_LENGTH_BITS = 256
    private const val LEGACY_HASH_LENGTH_BITS = 512
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
            val hashBits = expectedHash.size * 8
            val actualHash = computeHash(password.toCharArray(), salt, hashBits)
            constantTimeEquals(expectedHash, actualHash)
        } catch (_: Exception) {
            false
        }
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun computeHash(password: CharArray, salt: ByteArray, hashLengthBits: Int = HASH_LENGTH_BITS): ByteArray {
        val spec: KeySpec = PBEKeySpec(password, salt, ITERATIONS, hashLengthBits)
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
        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decodeStoredValue(encoded: String): Pair<ByteArray, ByteArray> {
        val combined = Base64.getDecoder().decode(encoded)
        val hashLengthBytes = when (combined.size) {
            SALT_LENGTH_BYTES + LEGACY_HASH_LENGTH_BITS / 8 -> LEGACY_HASH_LENGTH_BITS / 8
            SALT_LENGTH_BYTES + HASH_LENGTH_BITS / 8 -> HASH_LENGTH_BITS / 8
            else -> throw IllegalArgumentException("Invalid stored password value length")
        }
        val salt = ByteArray(SALT_LENGTH_BYTES)
        val hash = ByteArray(hashLengthBytes)
        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH_BYTES)
        System.arraycopy(combined, SALT_LENGTH_BYTES, hash, 0, hashLengthBytes)
        return salt to hash
    }
}
