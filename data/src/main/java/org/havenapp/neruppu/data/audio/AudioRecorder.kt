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

package org.havenapp.neruppu.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    suspend fun startRecording(): File? = withContext(Dispatchers.IO) {
        try {
            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis())
            val outputDir = File(context.filesDir, "audio_captures").apply { mkdirs() }
            val file = File(outputDir, "$name.mp4")
            currentOutputFile = file

            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            Log.d("AudioRecorder", "Started recording to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun stopRecording(): Uri? = withContext(Dispatchers.IO) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val file = currentOutputFile
            currentOutputFile = null
            Log.d("AudioRecorder", "Stopped recording")
            file?.let { Uri.fromFile(it) }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
            null
        }
    }
}
