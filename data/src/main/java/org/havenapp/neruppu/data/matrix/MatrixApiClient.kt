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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import org.havenapp.neruppu.data.BuildConfig
import android.util.Log
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatrixApiClient @Inject constructor(
    private val configStore: MatrixConfigStore,
    private val client: HttpClient
) {
    private fun getBaseUrl(): String {
        return configStore.homeserverUrl.trim().let {
            if (!it.startsWith("http")) "https://$it" else it
        }.trimEnd('/')
    }

    /**
     * Matrix path parameters for room IDs and user IDs are sensitive to encoding.
     * Often, the literal sigil (! or # or @) must remain unencoded while the rest
     * of the ID (containing : and dots) must be encoded.
     */
    private fun encodeMatrixIdForPath(id: String): String {
        val encoded = URLEncoder.encode(id, "UTF-8")
        return when {
            id.startsWith("!") && encoded.startsWith("%21") -> "!" + encoded.substring(3)
            id.startsWith("#") && encoded.startsWith("%23") -> "#" + encoded.substring(3)
            id.startsWith("@") && encoded.startsWith("%40") -> "@" + encoded.substring(3)
            else -> encoded
        }
    }

    private suspend fun resolveRoomId(roomIdOrAlias: String): Result<String> = runCatching {
        if (roomIdOrAlias.startsWith("!")) return Result.success(roomIdOrAlias)
        if (!roomIdOrAlias.startsWith("#")) {
            error("Invalid Room ID or Alias: $roomIdOrAlias. Must start with '!' or '#'")
        }
        
        val encodedAlias = encodeMatrixIdForPath(roomIdOrAlias)
        val url = "${getBaseUrl()}/_matrix/client/v3/directory/room/$encodedAlias"
        
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Resolving room alias: $url")
        val response: HttpResponse = client.get(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            header("Accept", "application/json")
        }

        val body = response.body<JsonObject>()
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Resolve response [${response.status}]: $body")
        
        body["room_id"]?.jsonPrimitive?.content 
            ?: error("Failed to resolve alias [${response.status}]: $body")
    }

    private suspend fun joinRoom(roomIdOrAlias: String): Result<Unit> = runCatching {
        val encodedId = encodeMatrixIdForPath(roomIdOrAlias)
        val url = "${getBaseUrl()}/_matrix/client/v3/join/$encodedId"

        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Joining room: $url")
        val response: HttpResponse = client.post(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        val body = response.body<JsonObject>()
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Join response [${response.status}]: $body")

        if (response.status.value !in 200..299 && !body.containsKey("room_id")) {
            error("Failed to join room [${response.status}]: $body")
        }
    }

    // Send a plain text message
    suspend fun sendTextEvent(
        message: String,
        txnId: String = UUID.randomUUID().toString()
    ): Result<Unit> = runCatching {
        val resolvedRoomId = resolveRoomId(configStore.roomId).getOrThrow()
        
        val sendResult = performSendText(resolvedRoomId, message, txnId)
        
        if (sendResult.isFailure) {
            if (BuildConfig.DEBUG) Log.w("MatrixApiClient", "Initial send failed, attempting to join and retry...")
            joinRoom(resolvedRoomId).onSuccess {
                return performSendText(resolvedRoomId, message, txnId)
            }
        }
        
        sendResult.getOrThrow()
    }.onFailure {
        if (BuildConfig.DEBUG) Log.e("MatrixApiClient", "Exception during sendTextEvent", it)
    }

    private suspend fun performSendText(roomId: String, message: String, txnId: String): Result<Unit> = runCatching {
        val encodedRoomId = encodeMatrixIdForPath(roomId)
        val url = "${getBaseUrl()}/_matrix/client/v3/rooms/$encodedRoomId/send/m.room.message/$txnId"

        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Sending text event to: $url (txn: $txnId)")
        val response: HttpResponse = client.put(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            // Follow body structure from verified curl
            setBody(buildJsonObject {
                put("body", JsonPrimitive(message))
                put("msgtype", JsonPrimitive("m.text"))
            })
        }
        
        val body = response.body<JsonObject>()
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Send text response [${response.status}]: $body")
        
        if (body.containsKey("event_id")) {
            Unit
        } else {
            error("Failed to send text event [${response.status}]: $body")
        }
    }

    // Upload media -> returns mxc:// URI
    suspend fun uploadMedia(
        bytes: ByteArray,
        mimeType: String,
        fileName: String
    ): Result<String> = runCatching {
        val encodedFileName = URLEncoder.encode(fileName, "UTF-8")
        val url = "${getBaseUrl()}/_matrix/media/v3/upload?filename=$encodedFileName"
        
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Uploading media to: $url")
        val response: HttpResponse = client.post(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            header("Accept", "application/json")
            contentType(ContentType.parse(mimeType))
            setBody(bytes)
        }

        val body = response.body<JsonObject>()
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Upload response [${response.status}]: $body")
        
        body["content_uri"]?.jsonPrimitive?.content
            ?: error("Failed to upload media [${response.status}]: $body")
    }.onFailure {
        if (BuildConfig.DEBUG) Log.e("MatrixApiClient", "Exception during uploadMedia", it)
    }

    // Send image event with mxc:// URI
    suspend fun sendImageEvent(
        mxcUri: String,
        caption: String,
        mimeType: String = "image/jpeg",
        txnId: String = UUID.randomUUID().toString()
    ): Result<Unit> = runCatching {
        val resolvedRoomId = resolveRoomId(configStore.roomId).getOrThrow()
        
        val sendResult = performSendMedia(resolvedRoomId, "m.image", mxcUri, caption, mimeType, txnId)
        
        if (sendResult.isFailure) {
            if (BuildConfig.DEBUG) Log.w("MatrixApiClient", "Initial media send failed, attempting to join and retry...")
            joinRoom(resolvedRoomId).onSuccess {
                return performSendMedia(resolvedRoomId, "m.image", mxcUri, caption, mimeType, txnId)
            }
        }
        sendResult.getOrThrow()
    }.onFailure {
        if (BuildConfig.DEBUG) Log.e("MatrixApiClient", "Exception during sendImageEvent", it)
    }

    // Send audio event with mxc:// URI
    suspend fun sendAudioEvent(
        mxcUri: String,
        caption: String,
        mimeType: String = "audio/mp4",
        txnId: String = UUID.randomUUID().toString()
    ): Result<Unit> = runCatching {
        val resolvedRoomId = resolveRoomId(configStore.roomId).getOrThrow()
        
        val sendResult = performSendMedia(resolvedRoomId, "m.audio", mxcUri, caption, mimeType, txnId)
        
        if (sendResult.isFailure) {
            if (BuildConfig.DEBUG) Log.w("MatrixApiClient", "Initial audio send failed, attempting to join and retry...")
            joinRoom(resolvedRoomId).onSuccess {
                return performSendMedia(resolvedRoomId, "m.audio", mxcUri, caption, mimeType, txnId)
            }
        }
        sendResult.getOrThrow()
    }.onFailure {
        if (BuildConfig.DEBUG) Log.e("MatrixApiClient", "Exception during sendAudioEvent", it)
    }

    private suspend fun performSendMedia(
        roomId: String,
        msgType: String,
        mxcUri: String,
        caption: String,
        mimeType: String,
        txnId: String
    ): Result<Unit> = runCatching {
        val encodedRoomId = encodeMatrixIdForPath(roomId)
        val url = "${getBaseUrl()}/_matrix/client/v3/rooms/$encodedRoomId/send/m.room.message/$txnId"
        
        val response: HttpResponse = client.put(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("body", JsonPrimitive(caption))
                put("msgtype", JsonPrimitive(msgType))
                put("url", JsonPrimitive(mxcUri))
                put("info", buildJsonObject {
                    put("mimetype", JsonPrimitive(mimeType))
                })
            })
        }
        
        val body = response.body<JsonObject>()
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Send media response [${response.status}]: $body")
        
        if (body.containsKey("event_id")) {
            Unit
        } else {
            error("Failed to send media event [${response.status}]: $body")
        }
    }

    suspend fun testConnection(): Result<Unit> = runCatching {
        val url = "${getBaseUrl()}/_matrix/client/v3/account/whoami"
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Testing connection (whoami) at: $url")
        val whoamiResponse: HttpResponse = client.get(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            header("Accept", "application/json")
        }

        val whoamiBody = whoamiResponse.body<JsonObject>()
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Whoami response [${whoamiResponse.status}]: $whoamiBody")
        
        val userId = whoamiBody["user_id"]?.jsonPrimitive?.content
            ?: error("Connection failed [${whoamiResponse.status}]: $whoamiBody")

        // Step 2: Resolve Room ID
        val resolvedRoomId = resolveRoomId(configStore.roomId).getOrThrow()

        // Step 3: Check room membership
        val encodedRoomId = encodeMatrixIdForPath(resolvedRoomId)
        val encodedUserId = encodeMatrixIdForPath(userId)
        val memberUrl = "${getBaseUrl()}/_matrix/client/v3/rooms/$encodedRoomId/state/m.room.member/$encodedUserId"

        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Checking room membership at: $memberUrl")
        val memberResponse: HttpResponse = client.get(memberUrl) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            header("Accept", "application/json")
        }

        val memberBody = memberResponse.body<JsonObject>()
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Membership response [${memberResponse.status}]: $memberBody")

        if (memberResponse.status.value !in 200..299) {
             if (BuildConfig.DEBUG) Log.w("MatrixApiClient", "Not a member, attempting to join...")
             joinRoom(resolvedRoomId).getOrThrow()
        }

        // Step 4: Check room encryption status
        val encEncodedRoomId = encodeMatrixIdForPath(resolvedRoomId)
        val encryptionUrl = "${getBaseUrl()}/_matrix/client/v3/rooms/$encEncodedRoomId/state/m.room.encryption"
        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Checking room encryption at: $encryptionUrl")
        val encryptionResponse: HttpResponse = client.get(encryptionUrl) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            header("Accept", "application/json")
        }
        if (encryptionResponse.status.value in 200..299) {
            if (BuildConfig.DEBUG) Log.w("MatrixApiClient", "Room is ENCRYPTED. Messages may not be visible in some clients unless verified.")
        }

        if (BuildConfig.DEBUG) Log.d("MatrixApiClient", "Test connection successful")
        Unit
    }.onFailure {
        if (BuildConfig.DEBUG) Log.e("MatrixApiClient", "Exception during testConnection", it)
    }
}
