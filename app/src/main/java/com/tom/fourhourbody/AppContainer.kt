package com.tom.fourhourbody

import android.content.Context
import com.tom.fourhourbody.data.db.AppDatabase
import com.tom.fourhourbody.data.reference.ReferenceRepository
import com.tom.fourhourbody.data.repo.ColdRepository
import com.tom.fourhourbody.data.repo.CreatineRepository
import com.tom.fourhourbody.data.repo.DashboardRepository
import com.tom.fourhourbody.data.repo.MeasurementRepository
import com.tom.fourhourbody.data.repo.MotivationRepository
import com.tom.fourhourbody.data.repo.NutritionRepository
import com.tom.fourhourbody.data.repo.ProgressRepository
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.SleepRepository
import com.tom.fourhourbody.data.repo.StretchRepository
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
    val stretchRepository by lazy { StretchRepository(database.stretchDao()) }
    val nutritionRepository by lazy { NutritionRepository(database.nutritionDao()) }
    val sleepRepository by lazy { SleepRepository(database.sleepDao()) }
    val coldRepository by lazy { ColdRepository(database.coldDao()) }
    val creatineRepository by lazy { CreatineRepository(database.creatineDao()) }
    val measurementRepository by lazy { MeasurementRepository(database.measurementDao()) }
    val referenceRepository by lazy { ReferenceRepository(appContext) }

    val motivationRepository by lazy {
        MotivationRepository(
            settingsRepository = settingsRepository,
            nutritionRepository = nutritionRepository,
            sleepRepository = sleepRepository,
            creatineRepository = creatineRepository
        )
    }

    val progressRepository by lazy {
        ProgressRepository(
            trainingRepository = trainingRepository,
            motivationRepository = motivationRepository
        )
    }

    val dashboardRepository by lazy {
        DashboardRepository(
            settingsRepository = settingsRepository,
            trainingRepository = trainingRepository,
            stretchRepository = stretchRepository,
            nutritionRepository = nutritionRepository,
            sleepRepository = sleepRepository,
            coldRepository = coldRepository,
            creatineRepository = creatineRepository
        )
    }

    /** Called whenever settings change, so a pillar toggle takes effect on alarms immediately. */
    suspend fun rescheduleReminders() {
        ReminderScheduler.reschedule(appContext, settingsRepository.current(), LocalDateTime.now())
    }
}
