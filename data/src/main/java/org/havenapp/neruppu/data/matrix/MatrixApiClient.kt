package org.havenapp.neruppu.data.matrix

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import android.util.Log
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatrixApiClient @Inject constructor(
    private val configStore: MatrixConfigStore
) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json() }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("MatrixApiClient", message)
                }
            }
            level = LogLevel.ALL
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 10_000
        }
    }

    // Send a plain text message
    suspend fun sendTextEvent(
        message: String,
        txnId: String = java.util.UUID.randomUUID().toString()
    ): Result<Unit> = runCatching {
        val baseUrl = configStore.homeserverUrl.trimEnd('/')
        val encodedRoomId = URLEncoder.encode(configStore.roomId, "UTF-8")
        val url = "$baseUrl/_matrix/client/v3" +
                  "/rooms/$encodedRoomId/send/m.room.message/$txnId"
        Log.d("MatrixApiClient", "Sending text event to: $url")
        val response: HttpResponse = client.put(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("msgtype", JsonPrimitive("m.text"))
                put("body", JsonPrimitive(message))
            })
        }
        Log.d("MatrixApiClient", "Response received: ${response.status}")
        if (response.status.value !in 200..299) {
            val errorBody = response.body<String>()
            Log.e("MatrixApiClient", "Send text failed with status ${response.status}: $errorBody")
            error("Failed to send text event: ${response.status} - $errorBody")
        }
    }.onFailure {
        Log.e("MatrixApiClient", "Exception during sendTextEvent", it)
    }

    // Modern Upload Flow (Matrix v1.7+)
    // Step 1: Create a media URI
    private suspend fun createMediaUri(): Result<String> = runCatching {
        val baseUrl = configStore.homeserverUrl.trimEnd('/')
        val url = "$baseUrl/_matrix/media/v1/create"
        val response: HttpResponse = client.post(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { }) // Ensure {} is sent for v1/create
        }
        if (response.status.value !in 200..299) {
            val errorBody = response.body<String>()
            Log.e("MatrixApiClient", "Create media URI failed with status ${response.status}: $errorBody")
            error("Failed to create media URI: ${response.status}")
        }
        val responseBody = response.body<JsonObject>()
        responseBody["content_uri"]?.jsonPrimitive?.content
            ?: error("No content_uri in create response")
    }

    // Step 2: Upload content to the reserved URI
    suspend fun uploadMedia(
        bytes: ByteArray,
        mimeType: String,
        fileName: String
    ): Result<String> = runCatching {
        val mxcUri = createMediaUri().getOrThrow()
        
        // mxc://serverName/mediaId
        val uriParts = mxcUri.removePrefix("mxc://").split("/", limit = 2)
        if (uriParts.size < 2) error("Invalid MXC URI: $mxcUri")
        
        val serverName = uriParts[0]
        val mediaId = uriParts[1]
        
        val baseUrl = configStore.homeserverUrl.trimEnd('/')
        val encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8")
        val url = "$baseUrl/_matrix/media/v3/upload/$serverName/$mediaId?filename=$encodedFileName"
        
        Log.d("MatrixApiClient", "Uploading media to: $url")
        val response: HttpResponse = client.put(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            contentType(ContentType.parse(mimeType))
            setBody(bytes)
        }
        if (response.status.value !in 200..299) {
            val errorBody = response.body<String>()
            Log.e("MatrixApiClient", "Upload failed with status ${response.status}: $errorBody")
            error("Failed to upload media: ${response.status}")
        }
        
        mxcUri
    }

    // Send image event with mxc:// URI
    suspend fun sendImageEvent(
        mxcUri: String,
        caption: String,
        mimeType: String = "image/jpeg",
        txnId: String = java.util.UUID.randomUUID().toString()
    ): Result<Unit> = runCatching {
        val baseUrl = configStore.homeserverUrl.trimEnd('/')
        val encodedRoomId = URLEncoder.encode(configStore.roomId, "UTF-8")
        val url = "$baseUrl/_matrix/client/v3" +
                  "/rooms/$encodedRoomId/send/m.room.message/$txnId"
        val response: HttpResponse = client.put(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("msgtype", JsonPrimitive("m.image"))
                put("body", JsonPrimitive(caption))
                put("url", JsonPrimitive(mxcUri))
                put("info", buildJsonObject {
                    put("mimetype", JsonPrimitive(mimeType))
                })
            })
        }
        if (response.status.value !in 200..299) {
            val errorBody = response.body<String>()
            Log.e("MatrixApiClient", "Send image failed with status ${response.status}: $errorBody")
            error("Failed to send image event: ${response.status}")
        }
    }

    // Send audio event with mxc:// URI
    suspend fun sendAudioEvent(
        mxcUri: String,
        caption: String,
        mimeType: String = "audio/mp4",
        txnId: String = java.util.UUID.randomUUID().toString()
    ): Result<Unit> = runCatching {
        val baseUrl = configStore.homeserverUrl.trimEnd('/')
        val encodedRoomId = URLEncoder.encode(configStore.roomId, "UTF-8")
        val url = "$baseUrl/_matrix/client/v3" +
                  "/rooms/$encodedRoomId/send/m.room.message/$txnId"
        val response: HttpResponse = client.put(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("msgtype", JsonPrimitive("m.audio"))
                put("body", JsonPrimitive(caption))
                put("url", JsonPrimitive(mxcUri))
                put("info", buildJsonObject {
                    put("mimetype", JsonPrimitive(mimeType))
                })
            })
        }
        if (response.status.value !in 200..299) {
            val errorBody = response.body<String>()
            Log.e("MatrixApiClient", "Send audio failed with status ${response.status}: $errorBody")
            error("Failed to send audio event: ${response.status}")
        }
    }

    suspend fun testConnection(): Result<Unit> = runCatching {
        val baseUrl = configStore.homeserverUrl.trimEnd('/')
        
        // Step 1: Verify token with /whoami and get userId
        val whoamiUrl = "$baseUrl/_matrix/client/v3/account/whoami"
        Log.d("MatrixApiClient", "Testing connection (whoami) at: $whoamiUrl")
        val whoamiResponse: HttpResponse = client.get(whoamiUrl) {
            header("Authorization", "Bearer ${configStore.accessToken}")
        }
        if (whoamiResponse.status.value !in 200..299) {
            val errorBody = whoamiResponse.body<String>()
            Log.e("MatrixApiClient", "Token validation failed: $errorBody")
            error("Access token invalid: ${whoamiResponse.status}")
        }
        
        val whoamiBody = whoamiResponse.body<JsonObject>()
        val userId = whoamiBody["user_id"]?.jsonPrimitive?.content
            ?: error("Failed to get user_id from whoami")

        // Step 2: Verify Room ID by checking OUR membership in that room
        val encodedRoomId = URLEncoder.encode(configStore.roomId, "UTF-8")
        val encodedUserId = URLEncoder.encode(userId, "UTF-8")
        val memberUrl = "$baseUrl/_matrix/client/v3/rooms/$encodedRoomId/state/m.room.member/$encodedUserId"
        
        Log.d("MatrixApiClient", "Testing room membership at: $memberUrl")
        val memberResponse: HttpResponse = client.get(memberUrl) {
            header("Authorization", "Bearer ${configStore.accessToken}")
        }
        
        if (memberResponse.status.value !in 200..299) {
            val errorBody = memberResponse.body<String>()
            Log.e("MatrixApiClient", "Room membership check failed with status ${memberResponse.status}: $errorBody")
            error("Failed to verify room membership: ${memberResponse.status}")
        }
        
        val memberBody = memberResponse.body<JsonObject>()
        val membership = memberBody["membership"]?.jsonPrimitive?.content
        if (membership != "join") {
            error("User is not a member of the room (status: $membership)")
        }
        
        Log.d("MatrixApiClient", "Test connection successful")
        Unit
    }.onFailure {
        Log.e("MatrixApiClient", "Exception during testConnection", it)
    }

}
