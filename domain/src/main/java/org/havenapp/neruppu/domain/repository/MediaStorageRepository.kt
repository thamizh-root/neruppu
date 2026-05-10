package org.havenapp.neruppu.domain.repository

import org.havenapp.neruppu.domain.model.MediaFile
import java.io.File

interface MediaStorageRepository {
    suspend fun saveImage(jpegBytes: ByteArray, timestamp: Long): Result<MediaFile>
    suspend fun saveAudio(audioFile: File, timestamp: Long): Result<MediaFile>
    fun getNeruppuFolder(): File
}
