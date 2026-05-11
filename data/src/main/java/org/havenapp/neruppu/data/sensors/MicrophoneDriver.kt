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
import kotlin.math.sqrt

class MicrophoneDriver {
    private val sampleRate = 16000 // Reduced from 44100 for battery optimization
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val silenceFloor = 50 // below this = silence, skip emit

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
            var silenceCount = 0
            while (isActive) {
                val read = recorder.read(buffer, 0, bufferSize)
                if (read > 0) {
                    var sumSq = 0.0
                    for (i in 0 until read) {
                        val s = buffer[i].toDouble()
                        sumSq += s * s
                    }
                    // RMS (Root Mean Square) — better for impulsive sounds
                    val rms = sqrt(sumSq / read).toInt()
                    
                    // Only emit if above a minimum noise floor OR every 5s for baseline update
                    if (rms > silenceFloor || silenceCount++ > 50) {
                        silenceCount = 0
                        trySend(rms)
                    }
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
