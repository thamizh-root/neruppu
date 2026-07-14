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
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SignificantMotionDriver(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val significantMotion = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)

    fun observeSignificantMotion(): Flow<Unit> = callbackFlow {
        if (significantMotion == null) {
            Log.w("SignificantMotionDriver", "Significant motion sensor not available")
            close()
            return@callbackFlow
        }

        val triggerListener = object : TriggerEventListener() {
            override fun onTrigger(event: TriggerEvent?) {
                Log.i("SignificantMotionDriver", "Significant motion triggered!")
                trySend(Unit)
                // Sensor is auto-disabled after trigger, need to re-register
                sensorManager.requestTriggerSensor(this, significantMotion)
            }
        }

        Log.d("SignificantMotionDriver", "Requesting significant motion trigger")
        val success = sensorManager.requestTriggerSensor(triggerListener, significantMotion)
        
        if (!success) {
            Log.e("SignificantMotionDriver", "Failed to request significant motion trigger")
            close()
        }

        awaitClose {
            Log.d("SignificantMotionDriver", "Canceling significant motion trigger")
            sensorManager.cancelTriggerSensor(triggerListener, significantMotion)
        }
    }
}
