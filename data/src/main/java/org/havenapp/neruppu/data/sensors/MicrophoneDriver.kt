package org.havenapp.neruppu.data.sensors

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class MicrophoneDriver {
    private val sampleRate = 16000 // Reduced from 44100 for battery optimization
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun observeNoise(): Flow<Int> = callbackFlow {
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
            )
        } catch (e: Exception) {
            Log.e("MicrophoneDriver", "Failed to create AudioRecord", e)
            close(e)
            return@callbackFlow
        }

        val buffer = ShortArray(bufferSize)
        try {
            recorder.startRecording()
            Log.d("MicrophoneDriver", "Started noise observation (16kHz optimized)")
        } catch (e: Exception) {
            Log.e("MicrophoneDriver", "Failed to start recording", e)
            close(e)
            return@callbackFlow
        }

        val job = launch(Dispatchers.IO) {
            while (isActive) {
                val read = recorder.read(buffer, 0, bufferSize)
                if (read > 0) {
                    var sum = 0L
                    for (i in 0 until read) {
                        sum += abs(buffer[i].toInt())
                    }
                    // Average amplitude is more stable and less CPU intensive than Max for large buffers
                    val avgAmplitude = (sum / read).toInt()
                    trySend(avgAmplitude)
                }
            }
        }

        awaitClose {
            job.cancel()
            recorder.stop()
            recorder.release()
        }
    }
}
