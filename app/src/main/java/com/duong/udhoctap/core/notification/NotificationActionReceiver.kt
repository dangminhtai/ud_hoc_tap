package com.duong.udhoctap.core.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.duong.udhoctap.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.duong.udhoctap.ACTION_SNOOZE"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)

        if (intent.action == ACTION_SNOOZE) {
            val snoozeRequest = OneTimeWorkRequestBuilder<StudyReminderWorker>()
                .setInitialDelay(1L, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueue(snoozeRequest)
        }
    }
}
