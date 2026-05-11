package org.havenapp.neruppu.data.matrix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.transport.AlertTransport
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatrixAlertTransport @Inject constructor(
    private val apiClient: MatrixApiClient,
    private val configStore: MatrixConfigStore,
    @ApplicationContext private val context: Context
) : AlertTransport {

    override val isConfigured: Boolean get() = configStore.isComplete

    override suspend fun send(payload: AlertPayload): Result<Unit> = runCatching {
        Log.d("MatrixAlertTransport", "Sending alert: $payload")
        when (payload) {
            is AlertPayload.TextAlert -> {
                val msg = "[🔥 Neruppu] ${payload.sensorType.name}: ${payload.message}"
                apiClient.sendTextEvent(msg).getOrThrow()
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
                val bytes: ByteArray = if (file.absolutePath.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(file.absolutePath))
                        ?.use { it.readBytes() } ?: error("Failed to read content URI")
                } else {
                    File(file.absolutePath).readBytes()
                }

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
                if (file.mimeType == "image/jpeg") {
                    apiClient.sendImageEvent(mxcUri, textMsg).getOrThrow()
                } else {
                    apiClient.sendAudioEvent(mxcUri, textMsg).getOrThrow()
                }
                Log.d("MatrixAlertTransport", "Media event sent successfully.")
            }
        }
        Unit
    }.onFailure {
        Log.e("MatrixAlertTransport", "Failed to send alert", it)
    }

    override suspend fun testConnection(): Result<Unit> = apiClient.testConnection()

    private fun compressJpeg(bytes: ByteArray, targetKb: Int): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        return try {
            val scaled = if (bitmap.width > 1280) {
                Bitmap.createScaledBitmap(bitmap, 1280,
                    (1280f * bitmap.height / bitmap.width).toInt(), true)
                    .also { if (it !== bitmap) bitmap.recycle() }
            } else bitmap
            
            ByteArrayOutputStream().also { out ->
                var quality = 85
                do {
                    out.reset()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    quality -= 10
                } while (out.size() > targetKb * 1024 && quality > 20)
            }.toByteArray()
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}
