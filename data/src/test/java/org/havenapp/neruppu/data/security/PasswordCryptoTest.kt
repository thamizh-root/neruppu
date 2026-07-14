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

import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordCryptoTest {

    private fun createLegacyHash(password: String, salt: ByteArray, iterations: Int, bits: Int): ByteArray {
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterations, bits)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun encodeLegacyStoredValue(password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = createLegacyHash(password, salt, 310000, 512)
        val combined = ByteArray(salt.size + hash.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(hash, 0, combined, salt.size, hash.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    @Test
    fun `TC-CRYPTO-01 Hash produces non-empty base64 string`() {
        val hash = PasswordCrypto.hashPassword("test-password-123")
        assertTrue(hash.isNotEmpty())
    }

    @Test
    fun `TC-CRYPTO-02 Correct password verifies successfully`() {
        val password = "mySecretPassword"
        val hash = PasswordCrypto.hashPassword(password)
        assertTrue(PasswordCrypto.verifyPassword(password, hash))
    }

    @Test
    fun `TC-CRYPTO-03 Wrong password fails verification`() {
        val hash = PasswordCrypto.hashPassword("correct-password")
        assertTrue(!PasswordCrypto.verifyPassword("wrong-password", hash))
    }

    @Test
    fun `TC-CRYPTO-04 Null stored hash returns false`() {
        assertTrue(!PasswordCrypto.verifyPassword("password", null))
    }

    @Test
    fun `TC-CRYPTO-05 Different passwords produce different hashes`() {
        val hash1 = PasswordCrypto.hashPassword("password-a")
        val hash2 = PasswordCrypto.hashPassword("password-b")
        assertTrue(hash1 != hash2)
    }

    @Test
    fun `TC-CRYPTO-06 Same password produces different hashes (salt randomness)`() {
        val hash1 = PasswordCrypto.hashPassword("same-password")
        val hash2 = PasswordCrypto.hashPassword("same-password")
        assertTrue(hash1 != hash2)
        assertTrue(PasswordCrypto.verifyPassword("same-password", hash1))
        assertTrue(PasswordCrypto.verifyPassword("same-password", hash2))
    }

    @Test
    fun `TC-CRYPTO-07 Empty password can be hashed and verified`() {
        val hash = PasswordCrypto.hashPassword("")
        assertTrue(PasswordCrypto.verifyPassword("", hash))
    }

    @Test
    fun `TC-CRYPTO-08 Long password handles correctly`() {
        val longPassword = "a".repeat(500)
        val hash = PasswordCrypto.hashPassword(longPassword)
        assertTrue(PasswordCrypto.verifyPassword(longPassword, hash))
    }

    @Test
    fun `TC-CRYPTO-09 Unicode password handles correctly`() {
        val password = "p\u00e4ssw\u00f6rd\u4e2d\u6587"
        val hash = PasswordCrypto.hashPassword(password)
        assertTrue(PasswordCrypto.verifyPassword(password, hash))
    }

    @Test
    fun `TC-CRYPTO-10 Stored hash is not equal to raw password`() {
        val password = "myPassword123"
        val hash = PasswordCrypto.hashPassword(password)
        assertTrue(hash != password)
    }

    @Test
    fun `TC-CRYPTO-11 Legacy 512-bit hash verifies correctly`() {
        val password = "legacyPassword123"
        val legacyHash = encodeLegacyStoredValue(password)
        assertTrue(PasswordCrypto.verifyPassword(password, legacyHash))
    }

    @Test
    fun `TC-CRYPTO-12 Legacy 512-bit hash rejects wrong password`() {
        val legacyHash = encodeLegacyStoredValue("legacyPassword123")
        assertTrue(!PasswordCrypto.verifyPassword("wrongPassword", legacyHash))
    }
}
