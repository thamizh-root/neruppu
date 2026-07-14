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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

class AccelerometerDriver(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private class MotionListener(
        private val threshold: Float,
        private val onMotion: (Float) -> Unit,
    ) : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt((x * x) + (y * y) + (z * z))
                if (magnitude > threshold) {
                    onMotion(magnitude)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun observeMotion(threshold: Float = 12f): Flow<Float> = callbackFlow {
        Log.d("AccelerometerDriver", "Registering accelerometer listener (Batched)")
        val listener = MotionListener(threshold) { magnitude ->
            Log.d("AccelerometerDriver", "Motion detected! Magnitude: $magnitude")
            trySend(magnitude)
        }

        // Register with batching (500ms latency) to save battery
        val registered = sensorManager.registerListener(
            listener, 
            accelerometer, 
            SensorManager.SENSOR_DELAY_UI,
            500000 // 500ms maxReportLatencyUs
        )
        Log.d("AccelerometerDriver", "Sensor listener registered: $registered")

        if (!registered) {
            Log.e("AccelerometerDriver", "Failed to register accelerometer listener!")
        }

        awaitClose {
            Log.d("AccelerometerDriver", "Unregistering accelerometer listener")
            sensorManager.unregisterListener(listener)
        }
    }
}
