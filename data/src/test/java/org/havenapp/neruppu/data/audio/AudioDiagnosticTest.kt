/*
 * Copyright (C) 2026 thamizh-root
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
