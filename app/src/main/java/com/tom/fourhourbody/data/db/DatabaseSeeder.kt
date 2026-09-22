package com.tom.fourhourbody.data.db

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.SettingsEntity

/**
 * Idempotent seeding. Runs on every launch and only fills what is missing, so nothing already
 * logged is ever touched.
 */
object DatabaseSeeder {

    suspend fun seedIfNeeded(db: AppDatabase) {
        seedSettings(db)
        seedExercises(db)
    }

    private suspend fun seedSettings(db: AppDatabase) {
        if (db.settingsDao().get() == null) {
            db.settingsDao().upsert(SettingsEntity())
        }
        if (db.trainingDao().getFrequency() == null) {
            db.trainingDao().upsertFrequency(FrequencySettingEntity())
        }
    }

    private suspend fun seedExercises(db: AppDatabase) {
        if (db.trainingDao().countConfigs() > 0) return
        db.trainingDao().insertConfigs(DEFAULT_EXERCISES)
    }

    /** The Big Five, on the equipment the book itself recommends. */
    val DEFAULT_EXERCISES = listOf(
        ExerciseConfigEntity(
            slotName = "Legs",
            exerciseName = "Leg press",
            equipment = "Machine",
            orderIndex = 0
        ),
        ExerciseConfigEntity(
            slotName = "Pull",
            exerciseName = "Pulldown",
            equipment = "Machine",
            orderIndex = 1
        ),
        ExerciseConfigEntity(
            slotName = "Row",
            exerciseName = "Seated row",
            equipment = "Machine",
            orderIndex = 2
        ),
        ExerciseConfigEntity(
            slotName = "Push",
            exerciseName = "Chest press",
            equipment = "Machine",
            orderIndex = 3
        ),
        ExerciseConfigEntity(
            slotName = "Overhead",
            exerciseName = "Overhead press",
            equipment = "Machine",
            orderIndex = 4
        )
    )
}
