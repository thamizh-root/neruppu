package org.havenapp.neruppu.data.matrix

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatrixApiClient @Inject constructor(
    private val configStore: MatrixConfigStore
) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json() }
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
        val url = "$baseUrl/_matrix/client/v3" +
                  "/rooms/${configStore.roomId}/send/m.room.message/$txnId"
        val response: HttpResponse = client.put(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("msgtype", JsonPrimitive("m.text"))
                put("body", JsonPrimitive(message))
            })
        }
        if (response.status.value !in 200..299) {
            val errorBody = response.body<String>()
            Log.e("MatrixApiClient", "Send text failed with status ${response.status}: $errorBody")
            error("Failed to send text event: ${response.status}")
        }
    }

    // Modern Upload Flow (Matrix v1.7+)
    // Step 1: Create a media URI
    private suspend fun createMediaUri(): Result<String> = runCatching {
        val baseUrl = configStore.homeserverUrl.trimEnd('/')
        val url = "$baseUrl/_matrix/media/v1/create"
        val response: HttpResponse = client.post(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            contentType(ContentType.Application.Json)
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
        val uriParts = mxcUri.removePrefix("mxc://").split("/")
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
        txnId: String = java.util.UUID.randomUUID().toString()
    ): Result<Unit> = runCatching {
        val baseUrl = configStore.homeserverUrl.trimEnd('/')
        val url = "$baseUrl/_matrix/client/v3" +
                  "/rooms/${configStore.roomId}/send/m.room.message/$txnId"
        val response: HttpResponse = client.put(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("msgtype", JsonPrimitive("m.image"))
                put("body", JsonPrimitive(caption))
                put("url", JsonPrimitive(mxcUri))
                put("info", buildJsonObject {
                    put("mimetype", JsonPrimitive("image/jpeg"))
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
        txnId: String = java.util.UUID.randomUUID().toString()
    ): Result<Unit> = runCatching {
        val baseUrl = configStore.homeserverUrl.trimEnd('/')
        val url = "$baseUrl/_matrix/client/v3" +
                  "/rooms/${configStore.roomId}/send/m.room.message/$txnId"
        val response: HttpResponse = client.put(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("msgtype", JsonPrimitive("m.audio"))
                put("body", JsonPrimitive(caption))
                put("url", JsonPrimitive(mxcUri))
                put("info", buildJsonObject {
                    put("mimetype", JsonPrimitive("audio/mp4"))
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
        val url = "$baseUrl/_matrix/client/v3/account/whoami"
        val response: HttpResponse = client.get(url) {
            header("Authorization", "Bearer ${configStore.accessToken}")
        }
        if (response.status.value !in 200..299) {
            val errorBody = response.body<String>()
            Log.e("MatrixApiClient", "Test connection failed with status ${response.status}: $errorBody")
            error("Connection failed: ${response.status}")
        }
    }
}
