package org.havenapp.neruppu.domain.usecase

import org.havenapp.neruppu.domain.repository.DeletePasswordRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeletePasswordUseCaseTest {

    @Test
    fun `TC-PWD-01 setPassword rejects blank password`() {
        val repository = FakeDeletePasswordRepository()
        val useCase = DeletePasswordUseCase(repository)

        val result = useCase.setPassword(" ", " ")

        assertTrue(result is DeletePasswordResult.Error)
        assertEquals("Password cannot be empty.", (result as DeletePasswordResult.Error).message)
        assertFalse(repository.setPasswordCalled)
    }

    @Test
    fun `TC-PWD-02 setPassword rejects mismatched confirmation`() {
        val repository = FakeDeletePasswordRepository()
        val useCase = DeletePasswordUseCase(repository)

        val result = useCase.setPassword("new-password", "different")

        assertTrue(result is DeletePasswordResult.Error)
        assertEquals("Passwords do not match.", (result as DeletePasswordResult.Error).message)
        assertFalse(repository.setPasswordCalled)
    }

    @Test
    fun `TC-PWD-03 setPassword saves valid password`() {
        val repository = FakeDeletePasswordRepository()
        val useCase = DeletePasswordUseCase(repository)

        val result = useCase.setPassword("new-password", "new-password")

        assertTrue(result is DeletePasswordResult.Success)
        assertTrue(repository.setPasswordCalled)
        assertEquals("new-password", repository.savedPassword)
    }

    @Test
    fun `TC-PWD-04 changePassword rejects when no password is configured`() {
        val repository = FakeDeletePasswordRepository(configured = false)
        val useCase = DeletePasswordUseCase(repository)

        val result = useCase.changePassword("old", "new", "new")

        assertTrue(result is DeletePasswordResult.Error)
        assertEquals("Delete password is not configured.", (result as DeletePasswordResult.Error).message)
        assertFalse(repository.setPasswordCalled)
    }

    @Test
    fun `TC-PWD-05 changePassword rejects incorrect current password`() {
        val repository = FakeDeletePasswordRepository(configured = true, verifyResult = false)
        val useCase = DeletePasswordUseCase(repository)

        val result = useCase.changePassword("wrong", "new", "new")

        assertTrue(result is DeletePasswordResult.Error)
        assertEquals("Current password is incorrect.", (result as DeletePasswordResult.Error).message)
        assertFalse(repository.setPasswordCalled)
    }

    @Test
    fun `TC-PWD-06 changePassword saves valid password`() {
        val repository = FakeDeletePasswordRepository(configured = true)
        val useCase = DeletePasswordUseCase(repository)

        val result = useCase.changePassword("old", "new", "new")

        assertTrue(result is DeletePasswordResult.Success)
        assertTrue(repository.setPasswordCalled)
        assertEquals("new", repository.savedPassword)
    }

    @Test
    fun `TC-PWD-07 removePassword rejects when no password is configured`() {
        val repository = FakeDeletePasswordRepository(configured = false)
        val useCase = DeletePasswordUseCase(repository)

        val result = useCase.removePassword("old")

        assertTrue(result is DeletePasswordResult.Error)
        assertEquals("Delete password is not configured.", (result as DeletePasswordResult.Error).message)
        assertFalse(repository.removePasswordCalled)
    }

    @Test
    fun `TC-PWD-08 removePassword rejects incorrect current password`() {
        val repository = FakeDeletePasswordRepository(configured = true, verifyResult = false)
        val useCase = DeletePasswordUseCase(repository)

        val result = useCase.removePassword("wrong")

        assertTrue(result is DeletePasswordResult.Error)
        assertEquals("Current password is incorrect.", (result as DeletePasswordResult.Error).message)
        assertFalse(repository.removePasswordCalled)
    }

    @Test
    fun `TC-PWD-09 removePassword removes configured password`() {
        val repository = FakeDeletePasswordRepository(configured = true)
        val useCase = DeletePasswordUseCase(repository)

        val result = useCase.removePassword("old")

        assertTrue(result is DeletePasswordResult.Success)
        assertTrue(repository.removePasswordCalled)
        assertEquals("old", repository.verifiedPassword)
    }

    private class FakeDeletePasswordRepository(
        private val configured: Boolean = false,
        private val verifyResult: Boolean = true
    ) : DeletePasswordRepository {

        var setPasswordCalled = false
        var removePasswordCalled = false
        var savedPassword: String? = null
        var verifiedPassword: String? = null

        override fun hasPassword(): Boolean = configured

        override fun setPassword(password: String) {
            setPasswordCalled = true
            savedPassword = password
        }

        override fun verifyPassword(password: String): Boolean {
            verifiedPassword = password
            return verifyResult
        }

        override fun removePassword(oldPassword: String): Boolean {
            removePasswordCalled = true
            return verifyPassword(oldPassword)
        }
    }
}
