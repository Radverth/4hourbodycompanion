package com.tom.fourhourbody.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tom.fourhourbody.FourHourBodyApp
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * A daily safety net: re-arms every alarm from the current settings. Alarms are dropped on
 * reboot, on app update, and when the system trims them, so something has to put them back.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as FourHourBodyApp).container
        val settings = container.settingsRepository.current()
        ReminderScheduler.reschedule(applicationContext, settings, LocalDateTime.now())
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "daily-reminder-refresh"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
