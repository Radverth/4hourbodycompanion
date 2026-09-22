package com.tom.fourhourbody

import android.content.Context
import com.tom.fourhourbody.data.db.AppDatabase
import com.tom.fourhourbody.data.repo.DashboardRepository
import com.tom.fourhourbody.data.repo.MeasurementRepository
import com.tom.fourhourbody.data.repo.ProgressRepository
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.notifications.ReminderScheduler
import java.time.LocalDateTime

/**
 * Manual dependency container. The app has one database, a handful of repositories and no
 * network layer, so a service locator is the right size for it — a DI framework would be more
 * ceremony than the graph justifies.
 */
class AppContainer(private val appContext: Context) {

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }

    val settingsRepository by lazy { SettingsRepository(database.settingsDao()) }
    val trainingRepository by lazy { TrainingRepository(database.trainingDao()) }
    val measurementRepository by lazy { MeasurementRepository(database.measurementDao()) }

    val progressRepository by lazy { ProgressRepository(trainingRepository) }

    val dashboardRepository by lazy { DashboardRepository(trainingRepository) }

    /** Called whenever settings change, so a reminder-time edit takes effect on alarms immediately. */
    suspend fun rescheduleReminders() {
        ReminderScheduler.reschedule(appContext, settingsRepository.current(), LocalDateTime.now())
    }
}
