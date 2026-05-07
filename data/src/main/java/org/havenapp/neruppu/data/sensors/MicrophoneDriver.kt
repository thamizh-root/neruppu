package org.havenapp.neruppu.data.sensors

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class MicrophoneDriver {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun observeNoise(threshold: Int = 1500): Flow<Int> = callbackFlow {
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize,
        )

        val buffer = ShortArray(bufferSize)
        recorder.startRecording()

        val job = launch(Dispatchers.IO) {
            while (isActive) {
                val read = recorder.read(buffer, 0, bufferSize)
                if (read > 0) {
                    var maxAmplitude = 0
                    for (i in 0 until read) {
                        val amplitude = abs(buffer[i].toInt())
                        if (amplitude > maxAmplitude) {
                            maxAmplitude = amplitude
                        }
                    }
                    if (maxAmplitude > threshold) {
                        trySend(maxAmplitude)
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
