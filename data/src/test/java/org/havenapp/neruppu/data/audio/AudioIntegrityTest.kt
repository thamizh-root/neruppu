package org.havenapp.neruppu.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class AudioIntegrityTest {

    @Test
    fun `test EMA spike detection logic`() {
        var fastBaseline = 0f
        var slowBaseline = 0f
        val threshold = 200f
        
        // Helper to simulate EMA update
        fun update(amp: Float): Float {
            if (fastBaseline == 0f) {
                fastBaseline = amp
                slowBaseline = amp
            }
            fastBaseline = 0.5f * fastBaseline + 0.5f * amp
            slowBaseline = 0.999f * slowBaseline + 0.001f * amp
            return fastBaseline - slowBaseline
        }

        // 1. Establish baseline with "quiet" noise (RMS 100)
        repeat(100) { update(100f) }
        
        val baselineSpike = update(100f)
        assertTrue("Baseline spike should be near zero, got $baselineSpike", baselineSpike < 10f)

        // 2. Simulate a sudden spike (RMS 1000)
        val spikeValue = update(1000f)
        
        // With alpha 0.5:
        // New Fast = 0.5 * 100 + 0.5 * 1000 = 550
        // New Slow = 0.999 * 100 + 0.001 * 1000 = 100.9
        // Spike = 550 - 100.9 = 449.1
        
        assertTrue("Spike should be detected! Expected > $threshold, got $spikeValue", spikeValue > threshold)
        assertEquals(449.1f, spikeValue, 0.1f)
    }

    @Test
    fun `test RMS calculation accuracy`() {
        val buffer = ShortArray(1024) { 1000 } // Constant amplitude
        var sumSq = 0.0
        for (i in buffer.indices) {
            val s = buffer[i].toDouble()
            sumSq += s * s
        }
        val rms = sqrt(sumSq / buffer.size).toInt()
        assertEquals(1000, rms)
    }
}
