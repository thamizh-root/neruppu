package org.havenapp.neruppu.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.havenapp.neruppu.service.MonitoringService

class EventNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventType = inputData.getString("event_type") ?: "Unknown"
        val description = inputData.getString("description") ?: ""

        val notification = NotificationCompat.Builder(applicationContext, MonitoringService.CHANNEL_ID)
            .setContentTitle("Security Event: $eventType")
            .setContentText(description)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (e: SecurityException) {
            // Handle notification permission missing on Android 13+
            return Result.failure()
        }

        return Result.success()
    }
}
