package org.havenapp.neruppu.data.audio

import android.media.AudioRecord
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import org.havenapp.neruppu.data.sensors.MicrophoneDriver
import org.junit.After
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
        
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `TC-MIC-01 Factory is used to create AudioRecord`() = runTest {
        val mockFactory = mockk<AudioRecordFactory>()
        val mockRecorder = mockk<AudioRecord>(relaxed = true)
        every { mockFactory.create(any(), any(), any(), any(), any()) } returns mockRecorder
        
        val driver = MicrophoneDriver(mockFactory)
        val job = launch {
            driver.observeNoise().collect { }
        }
        
        // Give it a moment to reach the factory call
        runCurrent()
        
        verify { mockFactory.create(any(), any(), any(), any(), any()) }
        job.cancel()
    }

    @Test
    fun `TC-MIC-02 RMS calculation is accurate`() = runTest {
        val mockFactory = mockk<AudioRecordFactory>()
        val mockRecorder = mockk<AudioRecord>(relaxed = true)
        every { mockFactory.create(any(), any(), any(), any(), any()) } returns mockRecorder
        
        val bufferSlot = slot<ShortArray>()
        every { mockRecorder.read(capture(bufferSlot), any(), any()) } answers {
            val buf = bufferSlot.captured
            for (i in buf.indices) buf[i] = 1000 // Constant amplitude
            buf.size
        }

        val driver = MicrophoneDriver(mockFactory)
        val rms = driver.observeNoise().first()
        
        assertEquals(1000, rms)
    }

    @Test
    fun `TC-MIC-03 Silence floor suppresses emissions`() = runTest {
        val mockFactory = mockk<AudioRecordFactory>()
        val mockRecorder = mockk<AudioRecord>(relaxed = true)
        every { mockFactory.create(any(), any(), any(), any(), any()) } returns mockRecorder
        
        val bufferSlot = slot<ShortArray>()
        // Return RMS 10 (below silence floor 20)
        every { mockRecorder.read(capture(bufferSlot), any(), any()) } answers {
            val buf = bufferSlot.captured
            for (i in buf.indices) buf[i] = 10
            buf.size
        }

        val driver = MicrophoneDriver(mockFactory)
        val results = mutableListOf<Int>()
        val job = launch {
            driver.observeNoise().collect { results.add(it) }
        }
        
        // Wait a bit to ensure nothing is emitted (less than 30 iterations)
        advanceTimeBy(100)
        runCurrent()
        assertTrue(results.isEmpty())
        
        job.cancel()
    }

    @Test
    fun `TC-MIC-08 Resources are released on cancellation`() = runTest {
        val mockFactory = mockk<AudioRecordFactory>()
        val mockRecorder = mockk<AudioRecord>(relaxed = true)
        every { mockFactory.create(any(), any(), any(), any(), any()) } returns mockRecorder
        
        val driver = MicrophoneDriver(mockFactory)
        val job = launch {
            driver.observeNoise().collect { }
        }
        
        runCurrent()
        job.cancel()
        runCurrent()

        verify { mockRecorder.stop() }
        verify { mockRecorder.release() }
    }
}
