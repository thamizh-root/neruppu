package org.havenapp.neruppu.data.audio

import android.media.AudioRecord
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.havenapp.neruppu.data.sensors.MicrophoneDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MicrophoneDriverTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
    }

    @Test
    fun `test observeNoise emits values when AudioRecord reads data`() = runBlocking {
        val mockFactory = mockk<AudioRecordFactory>()
        val mockRecorder = mockk<AudioRecord>(relaxed = true)
        
        every { mockFactory.create(any(), any(), any(), any(), any()) } returns mockRecorder
        every { mockRecorder.state } returns AudioRecord.STATE_INITIALIZED
        
        // Mocking read to return some values then 0 to stop
        val bufferSlot = slot<ShortArray>()
        var readCount = 0
        every { mockRecorder.read(capture(bufferSlot), any(), any()) } answers {
            if (readCount < 3) {
                val buf = bufferSlot.captured
                for (i in buf.indices) buf[i] = 100 // RMS should be 100
                readCount++
                buf.size
            } else {
                -1 // Stop the loop in driver
            }
        }

        val driver = MicrophoneDriver(mockFactory)
        
        val values = withTimeout(2000) {
            driver.observeNoise().take(1).toList()
        }
        
        assertTrue(values.isNotEmpty())
        assertEquals(100, values[0])
        
        verify { mockRecorder.startRecording() }
        verify { mockRecorder.stop() }
        verify { mockRecorder.release() }
    }
}
