package org.havenapp.neruppu.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LightSensorDriver(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private class LightListener(
        private val threshold: Float,
        private val onLightChange: (Float) -> Unit,
    ) : SensorEventListener {
        private var lastValue: Float? = null
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                val currentValue = event.values[0]
                if (lastValue == null || (kotlin.math.abs(currentValue - (lastValue ?: 0f)) > threshold)) {
                    lastValue = currentValue
                    onLightChange(currentValue)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun observeLightChanges(threshold: Float = 2f): Flow<Float> = callbackFlow {
        Log.d("LightSensorDriver", "Registering light sensor listener")
        val listener = LightListener(threshold) { lux ->
            Log.i("LightSensorDriver", "Light change detected: $lux lux")
            trySend(lux)
        }

        val registered = sensorManager.registerListener(
            listener, 
            lightSensor, 
            SensorManager.SENSOR_DELAY_UI,
            2_000_000 // 2 second batch latency
        )
        Log.d("LightSensorDriver", "Sensor listener registered: $registered")

        if (!registered) {
            Log.e("LightSensorDriver", "Failed to register light sensor listener!")
        }

        awaitClose {
            Log.d("LightSensorDriver", "Unregistering light sensor listener")
            sensorManager.unregisterListener(listener)
        }
    }
}
