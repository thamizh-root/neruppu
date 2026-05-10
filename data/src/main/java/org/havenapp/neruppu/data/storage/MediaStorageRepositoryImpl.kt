package org.havenapp.neruppu.data.storage

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.havenapp.neruppu.domain.model.MediaFile
import org.havenapp.neruppu.domain.repository.MediaStorageRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaStorageRepository {

    override suspend fun saveImage(
        jpegBytes: ByteArray,
        timestamp: Long
    ): Result<MediaFile> = runCatching {
        withContext(Dispatchers.IO) {
            val fileName = "neruppu_motion_${timestamp}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/Neruppu")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: error("MediaStore insert returned null")

                context.contentResolver.openOutputStream(uri)?.use { it.write(jpegBytes) }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)

                MediaFile(
                    absolutePath = uri.toString(),
                    mimeType = "image/jpeg",
                    sizeBytes = jpegBytes.size.toLong(),
                    timestamp = timestamp
                )
            } else {
                val folder = getNeruppuFolder()
                val file = File(folder, fileName)
                file.writeBytes(jpegBytes)
                MediaFile(
                    absolutePath = file.absolutePath,
                    mimeType = "image/jpeg",
                    sizeBytes = file.length(),
                    timestamp = timestamp
                )
            }
        }
    }

    override suspend fun saveAudio(
        audioFile: File,
        timestamp: Long
    ): Result<MediaFile> = runCatching {
        withContext(Dispatchers.IO) {
            val fileName = "neruppu_sound_${timestamp}.mp4"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "audio/mp4")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/Neruppu")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: error("MediaStore insert returned null")

                context.contentResolver.openOutputStream(uri)?.use { out ->
                    audioFile.inputStream().copyTo(out)
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)

                MediaFile(
                    absolutePath = uri.toString(),
                    mimeType = "audio/mp4",
                    sizeBytes = audioFile.length(),
                    timestamp = timestamp
                )
            } else {
                val folder = getNeruppuFolder()
                val dest = File(folder, fileName)
                audioFile.copyTo(dest, overwrite = true)
                MediaFile(
                    absolutePath = dest.absolutePath,
                    mimeType = "audio/mp4",
                    sizeBytes = dest.length(),
                    timestamp = timestamp
                )
            }
        }
    }

    override fun getNeruppuFolder(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloads, "Neruppu").also { it.mkdirs() }
    }
}
