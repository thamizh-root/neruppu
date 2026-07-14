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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.havenapp.neruppu.data.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.havenapp.neruppu.domain.model.AlertPayload
import org.havenapp.neruppu.domain.transport.AlertTransport
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramAlertTransport @Inject constructor(
    private val apiClient: TelegramApiClient,
    private val configStore: TelegramConfigStore,
) : AlertTransport {

    override val isConfigured: Boolean get() = configStore.isComplete

    override suspend fun send(payload: AlertPayload): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isConfigured) {
                if (BuildConfig.DEBUG) {
                    Log.d("TelegramAlertTransport", "Transport not configured, skipping alert")
                }
                return@runCatching Unit
            }
            if (BuildConfig.DEBUG) {
                Log.d("TelegramAlertTransport", "Sending alert: $payload")
            }
            val textMsg = "<b>[🔥 Neruppu]</b> ${payload.sensorType.name}: ${payload.message}"

            when (payload) {
                is AlertPayload.TextAlert -> {
                    apiClient.sendMessage(textMsg).getOrThrow()
                }

                is AlertPayload.MediaAlert -> {
                    val file = payload.mediaFile
                    val bytes: ByteArray = File(file.absolutePath).readBytes()

                    if (file.mimeType == "image/jpeg") {
                        val uploadBytes = if (bytes.size > 800_000) {
                            compressJpeg(bytes, targetKb = 800)
                        } else bytes
                        apiClient.sendPhoto(uploadBytes, textMsg).getOrThrow()
                    } else {
                        apiClient.sendVoice(bytes, textMsg).getOrThrow()
                    }
                }
            }
            Unit
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.e("TelegramAlertTransport", "Failed to send Telegram alert", it)
            }
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