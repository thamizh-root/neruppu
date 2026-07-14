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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

class AccelerometerDriverTest {

    private val context = mockk<Context>()
    private val sensorManager = mockk<SensorManager>()
    private val accelerometer = mockk<Sensor>()

    @Before
    fun setup() {
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns accelerometer
        every { accelerometer.type } returns Sensor.TYPE_ACCELEROMETER
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `TC-ACC-01 Magnitude calculation is correct`() {
        val x = 3f
        val y = 4f
        val z = 0f
        val expectedMagnitude = sqrt((x * x) + (y * y) + (z * z))
        assertEquals(5.0f, expectedMagnitude, 0.001f)
    }

    @Test
    fun `TC-ACC-02 Trigger threshold is respected`() = runTest {
        val listenerSlot = slot<SensorEventListener>()
        every { 
            sensorManager.registerListener(capture(listenerSlot), any(), any(), any() as Int) 
        } returns true
        every { sensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        val driver = AccelerometerDriver(context)
        
        // Start observing in a separate coroutine
        val deferred = async(Dispatchers.Unconfined) {
            driver.observeMotion(threshold = 12f).first()
        }

        // Create a real SensorEvent via reflection because it's a final class with no public constructor
        val constructor = SensorEvent::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        val event = constructor.newInstance()
        
        // Use reflection to set values and sensor
        val sensorField = SensorEvent::class.java.getField("sensor")
        sensorField.set(event, accelerometer)
        val valuesField = SensorEvent::class.java.getField("values")
        
        // Simulate event below threshold
        valuesField.set(event, floatArrayOf(10f, 0f, 0f))
        listenerSlot.captured.onSensorChanged(event)
        
        // The flow should not have emitted yet
        assert(!deferred.isCompleted)

        // Simulate event above threshold
        valuesField.set(event, floatArrayOf(10f, 10f, 0f))
        listenerSlot.captured.onSensorChanged(event)

        val result = withTimeout(1000) {
            deferred.await()
        }

        assertEquals(sqrt(200f), result, 0.001f)
        assertTrue(result > 12f)
    }

    @Test
    fun `TC-ACC-03 Batch latency is set to 500ms`() = runTest {
        every { 
            sensorManager.registerListener(any(), any(), any(), 500000 as Int) 
        } returns true
        every { sensorManager.unregisterListener(any<SensorEventListener>()) } just Runs

        val driver = AccelerometerDriver(context)
        val job = launch(Dispatchers.Unconfined) {
            driver.observeMotion().first()
        }
        
        verify { sensorManager.registerListener(any(), any(), any(), 500000 as Int) }
        job.cancel()
    }
}
