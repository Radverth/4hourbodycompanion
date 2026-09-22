package com.tom.fourhourbody.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tom.fourhourbody.data.dao.MeasurementDao
import com.tom.fourhourbody.data.dao.SettingsDao
import com.tom.fourhourbody.data.dao.TrainingDao
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.MeasurementEntity
import com.tom.fourhourbody.data.entity.RunEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import com.tom.fourhourbody.data.entity.SettingsEntity

/**
 * One database file for the whole app.
 *
 * Schema export is on (see app/build.gradle.kts) and [ALL_MIGRATIONS] is wired up from the
 * start — no destructive fallback.
 */
@Database(
    entities = [
        RunEntity::class,
        SessionEntity::class,
        ExerciseLogEntity::class,
        ExerciseConfigEntity::class,
        FrequencySettingEntity::class,
        MeasurementEntity::class,
        SettingsEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trainingDao(): TrainingDao
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
