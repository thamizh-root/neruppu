package org.havenapp.neruppu.domain.repository

interface DeletePasswordRepository {
    fun hasPassword(): Boolean
    fun setPassword(password: String)
    fun verifyPassword(password: String): Boolean
    fun removePassword(oldPassword: String): Boolean
}
