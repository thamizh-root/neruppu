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

package org.havenapp.neruppu.data.matrix

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MatrixApiClientTest {
    private val configStore = mockk<MatrixConfigStore>()
    
    @Before
    fun setup() {
        every { configStore.homeserverUrl } returns "https://matrix.org"
        every { configStore.accessToken } returns "secret_token"
        every { configStore.roomId } returns "!room:matrix.org"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `sendTextEvent sends correct JSON payload to correct URL`() = runBlocking {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            if (url.contains("rooms/!room%3Amatrix.org/send/m.room.message")) {
                respond(
                    content = """{"event_id": "${"$"}123"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respondBadRequest()
            }
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        
        val api = MatrixApiClient(configStore, client)
        val result = api.sendTextEvent("Hello Matrix")
        
        assert(result.isSuccess)
    }

    @Test
    fun `sendTextEvent succeeds even if status is 205 but has event_id`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"event_id": "${"$"}nonstandard"}""",
                status = HttpStatusCode.ResetContent, // 205
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        
        val api = MatrixApiClient(configStore, client)
        val result = api.sendTextEvent("Special status test")
        
        assert(result.isSuccess)
    }

    @Test
    fun `sendTextEvent fails if response body is empty or malformed`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        
        val api = MatrixApiClient(configStore, client)
        val result = api.sendTextEvent("Fail test")
        
        assert(result.isFailure)
        assert(result.exceptionOrNull()?.message?.contains("Failed to send text event") == true)
    }

    @Test
    fun `resolveRoomId correctly handles complex aliases`() = runBlocking {
        var resolvedCalled = false
        val complexEngine = MockEngine { request ->
            val urlStr = request.url.toString()
            if (urlStr.contains("directory/room/#haven.app%3Amatrix.org")) {
                resolvedCalled = true
                respond(
                    content = """{"room_id": "!resolved:matrix.org"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else if (urlStr.contains("rooms/!resolved%3Amatrix.org/send")) {
                respond(
                    content = """{"event_id": "${"$"}abc"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respondBadRequest()
            }
        }
        
        val client = HttpClient(complexEngine) {
            install(ContentNegotiation) { json() }
        }
        
        every { configStore.roomId } returns "#haven.app:matrix.org"
        
        val api = MatrixApiClient(configStore, client)
        val result = api.sendTextEvent("test")
        
        assert(resolvedCalled)
        assert(result.isSuccess)
    }

    @Test
    fun `testConnection performs whoami and membership checks`() = runBlocking {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("/account/whoami") -> {
                    respond(
                        content = """{"user_id": "@user:matrix.org"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                request.url.encodedPath.contains("/state/m.room.member/@user%3Amatrix.org") -> {
                    respond(
                        content = """{"membership": "join"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                request.url.encodedPath.contains("/state/m.room.encryption") -> {
                    respond(
                        content = """{"algorithm": "m.megolm.v1.aes-sha2"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respondBadRequest()
            }
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        
        val api = MatrixApiClient(configStore, client)
        val result = api.testConnection()
        
        assert(result.isSuccess)
    }
}
