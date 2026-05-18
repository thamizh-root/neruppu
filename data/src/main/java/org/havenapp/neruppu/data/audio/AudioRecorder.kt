package org.havenapp.neruppu.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
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
        var newRecorder: MediaRecorder? = null
        try {
            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis())
            val outputDir = File(context.filesDir, "audio_captures").apply { mkdirs() }
            val file = File(outputDir, "$name.mp4")
            currentOutputFile = file

            newRecorder = createMediaRecorder()
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setOutputFile(file.absolutePath)
            newRecorder.prepare()
            newRecorder.start()
            
            mediaRecorder = newRecorder
            newRecorder = null
            Log.d("AudioRecorder", "Started recording to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            newRecorder?.release()
            currentOutputFile = null
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
        val recorder = mediaRecorder
        var success = true
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
            success = false
        } finally {
            recorder?.release()
            mediaRecorder = null
        }
        val file = currentOutputFile
        currentOutputFile = null
        Log.d("AudioRecorder", "Stopped recording")
        if (success && file != null) Uri.fromFile(file) else null
    }
}
