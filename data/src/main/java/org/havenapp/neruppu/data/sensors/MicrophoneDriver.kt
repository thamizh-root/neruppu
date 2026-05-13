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
import org.havenapp.neruppu.data.audio.AudioRecordFactory
import org.havenapp.neruppu.data.audio.DefaultAudioRecordFactory
import kotlin.math.abs
import kotlin.math.sqrt

class MicrophoneDriver(
    private val audioRecordFactory: AudioRecordFactory = DefaultAudioRecordFactory()
) {
    private val sampleRate = 16000 // Reduced from 44100 for battery optimization
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val silenceFloor = 20 // Lowered from 50 for better sensitivity

    private fun getBufferSize(): Int {
        return try {
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        } catch (e: Exception) {
            // Fallback for tests if not mocked
            2048
        }
    }

    @SuppressLint("MissingPermission")
    fun observeNoise(): Flow<Int> = callbackFlow {
        val bufferSize = getBufferSize().coerceAtLeast(1024)
        val recorder = try {
            audioRecordFactory.create(
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
                val read = try {
                    recorder.read(buffer, 0, bufferSize)
                } catch (e: Exception) {
                    Log.e("MicrophoneDriver", "Error reading from AudioRecord", e)
                    -1
                }
                
                if (read > 0) {
                    var sumSq = 0.0
                    for (i in 0 until read) {
                        val s = buffer[i].toDouble()
                        sumSq += s * s
                    }
                    // RMS (Root Mean Square) — better for impulsive sounds
                    val rms = sqrt(sumSq / read).toInt()
                    
                    // Log RMS in debug to verify it's working
                    if (rms > silenceFloor) {
                         Log.d("MicrophoneDriver", "SPIKE detected: $rms (Floor: $silenceFloor)")
                    }
                    
                    // Only emit if above a minimum noise floor OR every 3s for baseline update
                    if (rms > silenceFloor || silenceCount++ > 30) {
                        silenceCount = 0
                        trySend(rms)
                    } else {
                        // Battery optimization: don't spin 100% CPU when silent
                        kotlinx.coroutines.delay(10)
                    }
                } else if (read < 0) {
                    Log.e("MicrophoneDriver", "AudioRecord read error: $read")
                    break // Exit loop on error
                } else {
                    // Small delay if read returned 0
                    kotlinx.coroutines.delay(10)
                }
            }
        }

        awaitClose {
            job.cancel()
            try {
                recorder.stop()
            } catch (e: Exception) {
                Log.e("MicrophoneDriver", "Error stopping AudioRecord", e)
            } finally {
                recorder.release()
                Log.d("MicrophoneDriver", "AudioRecord released")
            }
        }
    }
}
