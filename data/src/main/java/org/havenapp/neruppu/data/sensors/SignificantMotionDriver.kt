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
