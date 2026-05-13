package org.havenapp.neruppu.data.audio

import android.annotation.SuppressLint
import android.media.AudioRecord

interface AudioRecordFactory {
    fun create(
        audioSource: Int,
        sampleRateInHz: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSizeInBytes: Int
    ): AudioRecord
}

class DefaultAudioRecordFactory : AudioRecordFactory {
    @SuppressLint("MissingPermission")
    override fun create(
        audioSource: Int,
        sampleRateInHz: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSizeInBytes: Int
    ): AudioRecord {
        return AudioRecord(
            audioSource,
            sampleRateInHz,
            channelConfig,
            audioFormat,
            bufferSizeInBytes
        )
    }
}
