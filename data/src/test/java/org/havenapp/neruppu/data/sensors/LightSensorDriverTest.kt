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

package org.havenapp.neruppu.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LightSensorDriverTest {

    private val context = mockk<Context>()
    private val sensorManager = mockk<SensorManager>()
    private val lightSensor = mockk<Sensor>()

    @Before
    fun setup() {
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns lightSensor
        every { lightSensor.type } returns Sensor.TYPE_LIGHT
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `TC-LGT-01 Delta filtering works`() = runTest {
        val listenerSlot = slot<SensorEventListener>()
        every { 
            sensorManager.registerListener(capture(listenerSlot), any(), any(), any() as Int) 
        } returns true
        every { sensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        val driver = LightSensorDriver(context)
        
        // Start observing
        val results = mutableListOf<Float>()
        val job = launch(Dispatchers.Unconfined) {
            driver.observeLightChanges(threshold = 2f).collect { results.add(it) }
        }

        val constructor = SensorEvent::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        val event = constructor.newInstance()
        
        val sensorField = SensorEvent::class.java.getField("sensor")
        sensorField.set(event, lightSensor)
        val valuesField = SensorEvent::class.java.getField("values")
        
        // First sample: 10 lux (should emit because lastValue is null)
        valuesField.set(event, floatArrayOf(10f))
        listenerSlot.captured.onSensorChanged(event)
        assertEquals(1, results.size)
        assertEquals(10f, results[0])

        // Second sample: 11 lux (delta 1 < 2, should NOT emit)
        valuesField.set(event, floatArrayOf(11f))
        listenerSlot.captured.onSensorChanged(event)
        assertEquals(1, results.size)

        // Third sample: 13 lux (delta 3 > 2, should emit)
        valuesField.set(event, floatArrayOf(13f))
        listenerSlot.captured.onSensorChanged(event)
        assertEquals(2, results.size)
        assertEquals(13f, results[1])

        job.cancel()
    }

    @Test
    fun `TC-LGT-02 Batch latency is set to 2s`() = runTest {
        every { 
            sensorManager.registerListener(any(), any(), any(), 2_000_000 as Int) 
        } returns true
        every { sensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        val driver = LightSensorDriver(context)
        val job = launch(Dispatchers.Unconfined) {
            driver.observeLightChanges().first()
        }
        
        verify { sensorManager.registerListener(any(), any(), any(), 2_000_000 as Int) }
        job.cancel()
    }
}
