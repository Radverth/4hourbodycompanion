package com.tom.fourhourbody.data.db

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.data.entity.StretchConfigEntity
import com.tom.fourhourbody.data.entity.StretchMode
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.domain.training.TrainingConstants

/**
 * Idempotent seeding. Runs on every launch and only fills what is missing, so a pillar added
 * in a later release gets its defaults without wiping anything already logged.
 */
object DatabaseSeeder {

    suspend fun seedIfNeeded(db: AppDatabase) {
        seedSettings(db)
        seedExercises(db)
        seedStretches(db)
    }

    private suspend fun seedSettings(db: AppDatabase) {
        if (db.settingsDao().get() == null) {
            // Every pillar on by default; all of it editable in Settings.
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

    private suspend fun seedStretches(db: AppDatabase) {
        if (db.stretchDao().countConfigs() > 0) return
        db.stretchDao().insertConfigs(DEFAULT_STRETCHES)
    }

    /**
     * Leg press carries the book's 10+ rep target; everything else is 7+.
     * Kettlebell swings are seeded here as the conditioning slot — the session player runs
     * them through the Tabata block rather than the 5/5 tempo loop.
     */
    val DEFAULT_EXERCISES = listOf(
        ExerciseConfigEntity(
            slotName = "Legs",
            exerciseName = "Leg press",
            equipment = "Machine",
            targetReps = TrainingConstants.LEG_PRESS_TARGET_REPS,
            orderIndex = 0
        ),
        ExerciseConfigEntity(
            slotName = "Push",
            exerciseName = "Chest press",
            equipment = "Machine",
            targetReps = TrainingConstants.DEFAULT_TARGET_REPS,
            orderIndex = 1
        ),
        ExerciseConfigEntity(
            slotName = "Pull",
            exerciseName = "Row / pulldown",
            equipment = "Machine",
            targetReps = TrainingConstants.DEFAULT_TARGET_REPS,
            orderIndex = 2
        ),
        ExerciseConfigEntity(
            slotName = "Overhead",
            exerciseName = "Barbell overhead press",
            equipment = "Barbell",
            targetReps = TrainingConstants.DEFAULT_TARGET_REPS,
            orderIndex = 3
        ),
        ExerciseConfigEntity(
            slotName = "Conditioning",
            exerciseName = "Kettlebell swings",
            equipment = TrainingConstants.KETTLEBELL_EQUIPMENT,
            targetReps = 0,
            orderIndex = 4
        )
    )

    /** Section 2.3 of the brief, verbatim, grouped by routine. */
    val DEFAULT_STRETCHES = listOf(
        StretchConfigEntity(
            stretchName = "Hip flexor stretch",
            routine = StretchRoutine.PRE_KETTLEBELL,
            mode = StretchMode.HOLD,
            defaultHoldSec = 30,
            defaultSide = "Non-dominant,Dominant",
            orderIndex = 0,
            notes = "Static exception — 30s–2 min before kettlebell swings, to put the hip " +
                "flexors to sleep. Non-dominant side first."
        ),
        StretchConfigEntity(
            stretchName = "Double-leg glute bridge",
            routine = StretchRoutine.PRE_WORKOUT,
            mode = StretchMode.REPS,
            defaultReps = 10,
            defaultSets = 1,
            orderIndex = 0,
            notes = "Pre-lifting activation — runs before the first exercise of every session."
        ),
        StretchConfigEntity(
            stretchName = "Single-leg glute bridge",
            routine = StretchRoutine.PRE_WORKOUT,
            mode = StretchMode.REPS,
            defaultReps = 15,
            defaultSets = 1,
            defaultSide = "Left,Right",
            orderIndex = 1,
            notes = "15 reps per side, after the double-leg bridge."
        ),
        StretchConfigEntity(
            stretchName = "Super quad (couch) stretch",
            routine = StretchRoutine.REST_DAY_MOBILITY,
            mode = StretchMode.HOLD,
            defaultHoldSec = 90,
            defaultSide = "Left,Right",
            orderIndex = 0
        ),
        StretchConfigEntity(
            stretchName = "Pelvic symmetry / glute flexibility",
            routine = StretchRoutine.REST_DAY_MOBILITY,
            mode = StretchMode.HOLD,
            defaultHoldSec = 90,
            defaultSide = "Left — position 1,Left — position 2,Right — position 1,Right — position 2",
            orderIndex = 1,
            notes = "Table-supported pigeon-pose variant. 90s per position, multiple positions per side."
        ),
        StretchConfigEntity(
            stretchName = "Pelvis repositioning",
            routine = StretchRoutine.REST_DAY_MOBILITY,
            mode = StretchMode.HOLD,
            defaultHoldSec = 90,
            defaultSide = "Left,Right",
            orderIndex = 2,
            notes = "All fours, weight off one knee. 90s–2 min per side."
        ),
        StretchConfigEntity(
            stretchName = "Static Back",
            routine = StretchRoutine.DESK_RESET,
            mode = StretchMode.HOLD,
            defaultHoldSec = 300,
            orderIndex = 0,
            notes = "Legs up on a chair or block. Every 2–3 hours at a desk."
        ),
        StretchConfigEntity(
            stretchName = "Static Extension on Elbows",
            routine = StretchRoutine.DESK_RESET,
            mode = StretchMode.HOLD,
            defaultHoldSec = 60,
            orderIndex = 1,
            notes = "Every 2–3 hours at a desk."
        ),
        StretchConfigEntity(
            stretchName = "Shoulder Bridge with Pillow",
            routine = StretchRoutine.DESK_RESET,
            mode = StretchMode.HOLD,
            defaultHoldSec = 60,
            orderIndex = 2,
            notes = "Every 2–3 hours at a desk."
        ),
        StretchConfigEntity(
            stretchName = "Active Bridges with Pillow",
            routine = StretchRoutine.DESK_RESET,
            mode = StretchMode.REPS,
            defaultReps = 15,
            defaultSets = 3,
            orderIndex = 3,
            isWeeklyOnly = true,
            notes = "Rep-based, not a hold. Part of the weekly desk-reset set."
        ),
        StretchConfigEntity(
            stretchName = "Supine Groin Progressive",
            routine = StretchRoutine.DESK_RESET,
            mode = StretchMode.HOLD,
            defaultHoldSec = 600,
            defaultSide = "Left,Right",
            orderIndex = 4,
            isWeeklyOnly = true,
            notes = "10–25 min per side (chair variant is fine). The book's single most " +
                "effective tool for hip flexor/psoas tightness from sitting."
        ),
        StretchConfigEntity(
            stretchName = "Air Bench",
            routine = StretchRoutine.DESK_RESET,
            mode = StretchMode.HOLD,
            defaultHoldSec = 120,
            orderIndex = 5,
            isWeeklyOnly = true,
            notes = "Wall-sit hold. Part of the weekly desk-reset set."
        )
    )
}
