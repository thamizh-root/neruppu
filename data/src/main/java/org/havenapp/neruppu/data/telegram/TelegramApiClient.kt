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

package org.havenapp.neruppu.data.telegram

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramApiClient @Inject constructor(
    private val configStore: TelegramConfigStore
) {
        private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json() }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
        }
    }

    private val baseUrl: String
        get() = "https://api.telegram.org/bot${configStore.botToken}"

    suspend fun sendMessage(text: String): Result<Unit> = runCatching {
        val response: HttpResponse = client.post("$baseUrl/sendMessage") {
            val formData = formData {
                append("chat_id", configStore.chatId)
                append("text", text)
                append("parse_mode", "HTML")
            }
            setBody(MultiPartFormDataContent(formData))
        }
        if (response.status.value !in 200..299) {
            error("Telegram sendMessage failed: ${response.status}")
        }
    }

    suspend fun sendPhoto(bytes: ByteArray, caption: String): Result<Unit> = runCatching {
        val response: HttpResponse = client.post("$baseUrl/sendPhoto") {
            val formData = formData {
                append("chat_id", configStore.chatId)
                append("caption", caption)
                append("photo", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"alert.jpg\"")
                })
            }
            setBody(MultiPartFormDataContent(formData))
        }
        if (response.status.value !in 200..299) {
            error("Telegram sendPhoto failed: ${response.status}")
        }
    }

    suspend fun sendAudio(bytes: ByteArray, caption: String): Result<Unit> = runCatching {
        val response: HttpResponse = client.post("$baseUrl/sendAudio") {
            val formData = formData {
                append("chat_id", configStore.chatId)
                append("caption", caption)
                append("audio", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "audio/mp4")
                    append(HttpHeaders.ContentDisposition, "filename=\"alert.mp4\"")
                })
            }
            setBody(MultiPartFormDataContent(formData))
        }
        if (response.status.value !in 200..299) {
            error("Telegram sendAudio failed: ${response.status}")
        }
    }

    suspend fun sendVoice(bytes: ByteArray, caption: String): Result<Unit> = runCatching {
        val response: HttpResponse = client.post("$baseUrl/sendVoice") {
            val formData = formData {
                append("chat_id", configStore.chatId)
                append("caption", caption)
                append("parse_mode", "HTML")
                append("voice", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "audio/mp4")
                    append(HttpHeaders.ContentDisposition, "filename=\"alert.mp4\"")
                })
            }
            setBody(MultiPartFormDataContent(formData))
        }
        if (response.status.value !in 200..299) error("Telegram sendVoice failed: ${response.status}")
    }

    suspend fun testConnection(): Result<Unit> = runCatching {
        val response: HttpResponse = client.get("$baseUrl/getMe")
        if (response.status.value !in 200..299) {
            error("Telegram connection failed: ${response.status}")
        }
    }
}
