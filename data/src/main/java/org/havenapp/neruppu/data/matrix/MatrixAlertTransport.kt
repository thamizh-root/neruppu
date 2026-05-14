package org.havenapp.neruppu.data.matrix

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.transport.AlertTransport
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatrixAlertTransport @Inject constructor(
    private val apiClient: MatrixApiClient,
    private val configStore: MatrixConfigStore
) : AlertTransport {

    override val isConfigured: Boolean get() = configStore.isComplete

    override suspend fun send(payload: AlertPayload): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isConfigured) {
                Log.w("MatrixAlertTransport", "Transport not configured, skipping alert.")
                return@runCatching
            }
            Log.d("MatrixAlertTransport", "Sending alert payload type: ${payload::class.simpleName}")
            when (payload) {
                is AlertPayload.TextAlert -> {
                    val msg = "[🔥 Neruppu] ${payload.sensorType.name}: ${payload.message}"
                    Log.d("MatrixAlertTransport", "Sending text event...")
                    apiClient.sendTextEvent(msg).getOrThrow()
                    Log.d("MatrixAlertTransport", "Text event sent successfully.")
                }

                is AlertPayload.MediaAlert -> {
                    // Send text first (instant) — media upload follows
                    val textMsg = "[🔥 Neruppu] ${payload.sensorType.name}: ${payload.message}"
                    Log.d("MatrixAlertTransport", "Sending initial text alert...")
                    apiClient.sendTextEvent(textMsg).onFailure {
                        Log.w("MatrixAlertTransport", "Initial text alert failed: ${it.message}")
                    }

                    // Read media bytes
                    val file = payload.mediaFile
                    Log.d("MatrixAlertTransport", "Reading media file: ${file.absolutePath}")
                    val bytes = try {
                        File(file.absolutePath).readBytes()
                    } catch (e: Exception) {
                        Log.e("MatrixAlertTransport", "Failed to read media file", e)
                        throw e
                    }
                    Log.d("MatrixAlertTransport", "Read ${bytes.size} bytes.")

                    // Compress image if needed (max 800KB)
                    val uploadBytes = if (file.mimeType == "image/jpeg" && bytes.size > 800_000) {
                        Log.d("MatrixAlertTransport", "Compressing image (${bytes.size} bytes)...")
                        compressJpeg(bytes, targetKb = 800)
                    } else bytes

                    // Upload then send media event
                    val fileName = file.absolutePath.substringAfterLast("/")
                    Log.d("MatrixAlertTransport", "Uploading media: $fileName (${uploadBytes.size} bytes)...")
                    val mxcUri = apiClient.uploadMedia(uploadBytes, file.mimeType, fileName)
                        .getOrThrow()

                    Log.d("MatrixAlertTransport", "Media uploaded. MXC URI: $mxcUri. Sending media event...")
                    val mediaResult = if (file.mimeType.startsWith("image/")) {
                        apiClient.sendImageEvent(mxcUri, textMsg, file.mimeType)
                    } else if (file.mimeType.startsWith("audio/")) {
                        apiClient.sendAudioEvent(mxcUri, textMsg, file.mimeType)
                    } else {
                        Log.w("MatrixAlertTransport", "Unsupported media type: ${file.mimeType}")
                        Result.success(Unit)
                    }
                    
                    mediaResult.getOrThrow()
                    Log.d("MatrixAlertTransport", "Media event sent successfully.")
                }
            }
            Unit
        }.onFailure {
            Log.e("MatrixAlertTransport", "CRITICAL: Failed to send Matrix alert", it)
        }
    }

    override suspend fun testConnection(): Result<Unit> = apiClient.testConnection()

    private fun compressJpeg(bytes: ByteArray, targetKb: Int): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        var scaled: Bitmap? = null
        return try {
            scaled = if (bitmap.width > 1280) {
                Bitmap.createScaledBitmap(bitmap, 1280,
                    (1280f * bitmap.height / bitmap.width).toInt(), true)
            } else null
            
            val targetBitmap = scaled ?: bitmap
            
            ByteArrayOutputStream().also { out ->
                var quality = 85
                do {
                    out.reset()
                    targetBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    quality -= 10
                    if (out.size() <= targetKb * 1024) break
                } while (quality > 20)
            }.toByteArray()
        } finally {
            scaled?.recycle()
            bitmap.recycle()
        }
    }
}
