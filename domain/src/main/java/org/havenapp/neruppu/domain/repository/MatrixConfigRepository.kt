package org.havenapp.neruppu.domain.repository

interface MatrixConfigRepository {
    var homeserverUrl: String
    var roomId: String
    var accessToken: String
    val isComplete: Boolean
    fun clear()
}
