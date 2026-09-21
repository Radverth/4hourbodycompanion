package com.tom.fourhourbody.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tom.fourhourbody.data.dao.ColdDao
import com.tom.fourhourbody.data.dao.CreatineDao
import com.tom.fourhourbody.data.dao.MeasurementDao
import com.tom.fourhourbody.data.dao.NutritionDao
import com.tom.fourhourbody.data.dao.SettingsDao
import com.tom.fourhourbody.data.dao.SleepDao
import com.tom.fourhourbody.data.dao.StretchDao
import com.tom.fourhourbody.data.dao.TrainingDao
import com.tom.fourhourbody.data.entity.ColdExposureLogEntity
import com.tom.fourhourbody.data.entity.CreatineLogEntity
import com.tom.fourhourbody.data.entity.DamageControlLogEntity
import com.tom.fourhourbody.data.entity.DietDayLogEntity
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.KettlebellRoundEntity
import com.tom.fourhourbody.data.entity.MeasurementEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.data.entity.SleepLogEntity
import com.tom.fourhourbody.data.entity.StretchConfigEntity
import com.tom.fourhourbody.data.entity.StretchLogEntity

/**
 * One database file for every pillar.
 *
 * Schema export is on (see app/build.gradle.kts) and [ALL_MIGRATIONS] is wired up from the
 * start — this app is expected to grow pillar by pillar, so no destructive fallback.
 */
@Database(
    entities = [
        SessionEntity::class,
        ExerciseLogEntity::class,
        ExerciseConfigEntity::class,
        KettlebellRoundEntity::class,
        FrequencySettingEntity::class,
        StretchLogEntity::class,
        StretchConfigEntity::class,
        DietDayLogEntity::class,
        DamageControlLogEntity::class,
        SleepLogEntity::class,
        ColdExposureLogEntity::class,
        CreatineLogEntity::class,
        MeasurementEntity::class,
        SettingsEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trainingDao(): TrainingDao
    abstract fun stretchDao(): StretchDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun sleepDao(): SleepDao
    abstract fun coldDao(): ColdDao
    abstract fun creatineDao(): CreatineDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        private const val NAME = "fourhourbody.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
                .build()
                .also { instance = it }
        }
    }
}
