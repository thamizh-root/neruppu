package org.havenapp.neruppu.data.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordCryptoTest {

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
}
