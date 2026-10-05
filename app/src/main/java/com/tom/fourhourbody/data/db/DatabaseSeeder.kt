package com.tom.fourhourbody.data.db

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.data.entity.StretchConfigEntity
import com.tom.fourhourbody.data.entity.StretchMode
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.domain.training.Slots
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

    /**
     * Refilled rather than merged when the slot list is stale.
     *
     * The protocol change replaced the exercises themselves, so an install carrying the four
     * old Occam's slots has nothing worth merging — and a row-by-row merge would leave a
     * half-and-half list that matches neither book. Checking for the slots the current
     * protocol expects makes this self-correcting: the migration empties the table, this
     * refills it, and a future protocol change only has to change [DEFAULT_EXERCISES].
     *
     * Dropping config rows is not dropping history. The logs are keyed by exercise name and
     * live in their own table, untouched.
     */
    private suspend fun seedExercises(db: AppDatabase) {
        val existing = db.trainingDao().getActiveConfigs()
        val expectedSlots = DEFAULT_EXERCISES.map { it.slotName }.toSet()
        val stale = existing.isNotEmpty() && existing.none { it.slotName in expectedSlots }
        if (stale) db.trainingDao().deleteAllConfigs()
        if (stale || db.trainingDao().countConfigs() == 0) {
            db.trainingDao().insertConfigs(DEFAULT_EXERCISES)
        }
    }

    private suspend fun seedStretches(db: AppDatabase) {
        if (db.stretchDao().countConfigs() > 0) return
        db.stretchDao().insertConfigs(DEFAULT_STRETCHES)
    }

    /**
     * The Big Five in the book's fixed order — row, chest press, pulldown, overhead press,
     * leg press — each with the free-weight exercise that stands in for it when there is no
     * machine, and the 60–90 second window it is aiming for.
     *
     * The pulldown has no free-weight equivalent on purpose. There isn't an honest one: a
     * pull-up is not a scalable substitute at arbitrary load, and naming something here that
     * trains a different movement would quietly corrupt the comparison with last session,
     * which is the only thing this protocol decides anything on.
     *
     * The board and bodyweight rows below are the same five slots again for a night without
     * equipment. They are seeded inactive-by-equipment rather than inactive-by-flag: the
     * session planner picks them by equipment when a no-equipment session is started, so they
     * never appear in a normal session and never need toggling.
     */
    val DEFAULT_EXERCISES = listOf(
        ExerciseConfigEntity(
            slotName = Slots.PULL_ROW,
            exerciseName = "Seated row",
            equipment = "Machine",
            freeWeightEquivalent = "Bent-over barbell row",
            orderIndex = 0
        ),
        ExerciseConfigEntity(
            slotName = Slots.PUSH,
            exerciseName = "Chest press",
            equipment = "Machine",
            freeWeightEquivalent = "Bench press",
            orderIndex = 1
        ),
        ExerciseConfigEntity(
            slotName = Slots.PULL_LAT,
            exerciseName = "Pulldown",
            equipment = "Machine",
            freeWeightEquivalent = null,
            orderIndex = 2
        ),
        ExerciseConfigEntity(
            slotName = Slots.OVERHEAD,
            exerciseName = "Overhead press",
            equipment = "Machine",
            freeWeightEquivalent = "Standing overhead press",
            orderIndex = 3
        ),
        ExerciseConfigEntity(
            slotName = Slots.LEGS,
            exerciseName = "Leg press",
            equipment = "Machine",
            freeWeightEquivalent = "Squat or deadlift",
            orderIndex = 4
        ),

        // ---- no-equipment night: push-up board and bodyweight ----
        ExerciseConfigEntity(
            slotName = Slots.PULL_ROW,
            exerciseName = "Board row",
            equipment = TrainingConstants.EQUIPMENT_BOARD,
            orderIndex = 10
        ),
        ExerciseConfigEntity(
            slotName = Slots.PUSH,
            exerciseName = "Wide-grip push-up",
            equipment = TrainingConstants.EQUIPMENT_BOARD,
            orderIndex = 11
        ),
        ExerciseConfigEntity(
            slotName = Slots.PULL_LAT,
            exerciseName = "Board row (second handle position)",
            equipment = TrainingConstants.EQUIPMENT_BOARD,
            orderIndex = 12
        ),
        ExerciseConfigEntity(
            slotName = Slots.OVERHEAD,
            exerciseName = "Pike push-up",
            equipment = TrainingConstants.EQUIPMENT_BOARD,
            orderIndex = 13
        ),
        ExerciseConfigEntity(
            slotName = Slots.LEGS,
            exerciseName = "Wall sit",
            equipment = TrainingConstants.EQUIPMENT_BODYWEIGHT,
            orderIndex = 14
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
            notes = "Static exception — 30s per side before any explosive or jump-type " +
                "work, to put the hip flexors to sleep. Non-dominant side first."
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
