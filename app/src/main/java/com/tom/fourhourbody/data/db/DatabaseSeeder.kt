package com.tom.fourhourbody.data.db

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.domain.training.Slots
import com.tom.fourhourbody.domain.training.TrainingConstants

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

    /**
     * Seeded in two parts, because the two sets arrive at different times.
     *
     * The Big Five only go in on a genuinely empty table — an install that has had its slots
     * edited should keep them. The board rows are a later addition, so they are filled in
     * whenever they are missing: an install already carrying the five machine slots would
     * otherwise never get them, and the no-equipment session would find nothing to run.
     *
     * Both halves are idempotent and neither touches a row that already exists.
     */
    private suspend fun seedExercises(db: AppDatabase) {
        val dao = db.trainingDao()
        if (dao.countConfigs() == 0) {
            dao.insertConfigs(DEFAULT_EXERCISES)
        }
        val existing = dao.getAllConfigs().map { it.exerciseName }.toSet()
        val missingBoard = BOARD_EXERCISES.filterNot { it.exerciseName in existing }
        if (missingBoard.isNotEmpty()) {
            dao.insertConfigs(missingBoard)
        }
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

    /**
     * The same five slots again, for a night without machines: the folding push-up board plus
     * one bodyweight move for the slot it cannot cover.
     *
     * These are picked by equipment when a no-equipment session starts, so they never appear
     * in a normal session and never need toggling off. Progression here is the handle position
     * rather than the weight — wide, standard, narrow, decline — which is why a position only
     * advances once it has held past the ceiling twice.
     *
     * The wall sit covers legs deliberately. It is a static hold, so it measures in seconds
     * under load exactly as the protocol wants, where any rep-based leg movement would not.
     */
    val BOARD_EXERCISES = listOf(
        ExerciseConfigEntity(
            slotName = Slots.LEGS,
            exerciseName = "Wall sit",
            equipment = TrainingConstants.EQUIPMENT_BODYWEIGHT,
            orderIndex = 10
        ),
        ExerciseConfigEntity(
            slotName = Slots.PULL,
            exerciseName = "Board row (lat position)",
            equipment = TrainingConstants.EQUIPMENT_BOARD,
            orderIndex = 11
        ),
        ExerciseConfigEntity(
            slotName = Slots.ROW,
            exerciseName = "Board row (row position)",
            equipment = TrainingConstants.EQUIPMENT_BOARD,
            orderIndex = 12
        ),
        ExerciseConfigEntity(
            slotName = Slots.PUSH,
            exerciseName = "Wide-grip push-up",
            equipment = TrainingConstants.EQUIPMENT_BOARD,
            orderIndex = 13
        ),
        ExerciseConfigEntity(
            slotName = Slots.OVERHEAD,
            exerciseName = "Pike push-up",
            equipment = TrainingConstants.EQUIPMENT_BOARD,
            orderIndex = 14
        )
    )
}
