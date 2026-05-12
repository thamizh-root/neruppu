package org.havenapp.neruppu.data.storage

import android.content.Context
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
            val capturesDir = File(context.filesDir, "captures").apply { mkdirs() }
            val file = File(capturesDir, fileName)
            file.writeBytes(jpegBytes)
            
            MediaFile(
                absolutePath = file.absolutePath,
                mimeType = "image/jpeg",
                sizeBytes = file.length(),
                timestamp = timestamp
            )
        }
    }

    override suspend fun saveAudio(
        audioFile: File,
        timestamp: Long
    ): Result<MediaFile> = runCatching {
        withContext(Dispatchers.IO) {
            val fileName = "neruppu_sound_${timestamp}.mp4"
            val capturesDir = File(context.filesDir, "captures").apply { mkdirs() }
            val file = File(capturesDir, fileName)
            audioFile.copyTo(file, overwrite = true)
            
            MediaFile(
                absolutePath = file.absolutePath,
                mimeType = "audio/mp4",
                sizeBytes = file.length(),
                timestamp = timestamp
            )
        }
    }

    override fun getNeruppuFolder(): File {
        return File(context.filesDir, "captures").also { it.mkdirs() }
    }
}
