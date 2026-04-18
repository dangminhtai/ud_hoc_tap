package com.duong.udhoctap

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.duong.udhoctap.core.notification.StudyReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class UdHocTapApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "study_reminder"
        private const val STUDY_REMINDER_WORK_NAME = "study_reminder_work"
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleStudyReminder()
    }

    private fun scheduleStudyReminder() {
        val periodicReminderRequest = PeriodicWorkRequestBuilder<StudyReminderWorker>(
            repeatInterval = 24L,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            STUDY_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicReminderRequest
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Nhắc nhở ôn tập flashcard hàng ngày"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}
