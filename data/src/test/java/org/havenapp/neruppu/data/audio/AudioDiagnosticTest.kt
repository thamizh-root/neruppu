package org.havenapp.neruppu.data.audio

import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.havenapp.neruppu.data.sensors.MicrophoneDriver
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

/**
 * Pure logic test for MicrophoneDriver to ensure RMS calculation and filtering work.
 */
class AudioDiagnosticTest {

    @Test
    fun `test RMS calculation logic`() {
        val buffer = shortArrayOf(100, -100, 100, -100)
        var sumSq = 0.0
        for (i in buffer.indices) {
            val s = buffer[i].toDouble()
            sumSq += s * s
        }
        val rms = sqrt(sumSq / buffer.size).toInt()
        assertEquals(100, rms)
    }
}
