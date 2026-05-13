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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LightSensorDriverTest {

    private val context = mockk<Context>()
    private val sensorManager = mockk<SensorManager>()
    private val lightSensor = mockk<Sensor>()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0

        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) } returns lightSensor
        every { lightSensor.type } returns Sensor.TYPE_LIGHT
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
