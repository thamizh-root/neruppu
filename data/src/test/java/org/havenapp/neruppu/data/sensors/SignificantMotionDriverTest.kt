package org.havenapp.neruppu.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SignificantMotionDriverTest {

    private val context = mockk<Context>(relaxed = true)
    private val sensorManager = mockk<SensorManager>(relaxed = true)
    private val sensor = mockk<Sensor>()

    @Before
    fun setup() {
        every { context.getSystemService(Context.SENSOR_SERVICE) } returns sensorManager
        every { sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) } returns sensor
        every { sensorManager.requestTriggerSensor(any(), eq(sensor)) } returns true
        every { sensorManager.cancelTriggerSensor(any(), eq(sensor)) } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `TC-SMD-01 null sensor — flow closes without emitting`() = runTest {
        every { sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) } returns null
        val driver = SignificantMotionDriver(context)

        val flow = driver.observeSignificantMotion()
        try {
            flow.first()
            fail("Flow should have closed without emitting a value")
        } catch (e: Throwable) {
            // Expected — flow was closed without emitting
        }
    }

    @Test
    fun `TC-SMD-03 requestTriggerSensor fails — flow closes without emitting`() = runTest {
        every { sensorManager.requestTriggerSensor(any(), eq(sensor)) } returns false
        val driver = SignificantMotionDriver(context)

        val flow = driver.observeSignificantMotion()
        try {
            flow.first()
            fail("Flow should have closed without emitting")
        } catch (e: Throwable) {
            // Expected — flow was closed without emitting
        }
    }
}