package com.tom.fourhourbody.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrations live here from the first schema change onward — never
 * `fallbackToDestructiveMigration`, because the log history is the whole point of the app.
 *
 * Adding one: bump `version` in [AppDatabase], write the migration, add it to
 * [ALL_MIGRATIONS], and check the exported JSON under app/schemas/ — that is what
 * MigrationTestHelper verifies against.
 */

/**
 * Adds the behavioural fields: a per-pillar implementation intention, and the cheat day the
 * nutrition pillar counts down to.
 *
 * The nullable TEXT columns are added without a DEFAULT so the live schema matches what Room
 * expects from a `String?` field; `cheatDay` is NOT NULL, which SQLite requires a default
 * for, so the entity declares the same default through @ColumnInfo and the two agree.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "trainingIntention",
            "stretchesIntention",
            "nutritionIntention",
            "sleepIntention",
            "coldIntention",
            "creatineIntention"
        ).forEach { column ->
            db.execSQL("ALTER TABLE settings ADD COLUMN $column TEXT")
        }
        db.execSQL("ALTER TABLE settings ADD COLUMN cheatDay INTEGER NOT NULL DEFAULT 6")
    }
}

/**
 * Drops meal tagging. The calorie side of eating is handled by a dedicated tracker, so
 * logging protein/legume/veg tags here was duplicate data entry; what this app uniquely
 * answers is rule compliance, which lives on diet_day_logs and is untouched.
 *
 * This drops a table deliberately, which is not the same thing as a destructive fallback:
 * the feature is gone, so the rows have no reader. Every other table, including the diet
 * days the streaks are built from, is preserved.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS meal_logs")
    }
}

/**
 * Adds the alternating shift rota, so reminders can be kept out of working hours.
 *
 * Every added column is NOT NULL with a default that matches the entity's @ColumnInfo, except
 * the anchor Monday, which is genuinely absent until the rota is set up.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE settings ADD COLUMN shiftEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE settings ADD COLUMN shiftAnchorMonday INTEGER")
        db.execSQL("ALTER TABLE settings ADD COLUMN shiftAStartMinutes INTEGER NOT NULL DEFAULT 480")
        db.execSQL("ALTER TABLE settings ADD COLUMN shiftAEndMinutes INTEGER NOT NULL DEFAULT 1020")
        db.execSQL("ALTER TABLE settings ADD COLUMN shiftBStartMinutes INTEGER NOT NULL DEFAULT 540")
        db.execSQL("ALTER TABLE settings ADD COLUMN shiftBEndMinutes INTEGER NOT NULL DEFAULT 1080")
        db.execSQL("ALTER TABLE settings ADD COLUMN canStretchAtWork INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Adds runs — the training block between one stall and the next — and links sessions to them.
 *
 * Existing sessions keep a null runId rather than being back-filled into an invented run:
 * they happened before runs were tracked, and pretending otherwise would put fabricated
 * history in front of someone who would have no way to tell.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `runs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `runNumber` INTEGER NOT NULL,
                `startDate` INTEGER NOT NULL,
                `endDate` INTEGER,
                `endedBy` TEXT,
                `restDaysAtStart` INTEGER NOT NULL,
                `restDaysAtEnd` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("ALTER TABLE sessions ADD COLUMN runId INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_runId` ON `sessions` (`runId`)")
    }
}

/**
 * Body by Science replaces Occam's Protocol: every non-training pillar (stretches, nutrition,
 * sleep, cold exposure, creatine) is dropped, and training itself moves from rep targets to
 * time under load. Nothing here is a destructive fallback — every table drop below is a
 * feature that no longer exists, not a shortcut around a real migration, and every table that
 * still exists keeps its rows.
 *
 * Old sessions and exercise logs are preserved as raw history: they were logged under the old
 * protocol, so their `tulSec` is genuinely unknown (defaulted to 0) rather than backfilled
 * with a fabricated number. `sessions.stalled` keeps its column name via `@ColumnInfo` — the
 * Kotlin property renamed to `plateaued`, the stored data did not need to move.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        listOf(
            "stretch_configs",
            "stretch_logs",
            "diet_day_logs",
            "damage_control_logs",
            "sleep_logs",
            "cold_exposure_logs",
            "creatine_logs",
            "kettlebell_rounds"
        ).forEach { table -> db.execSQL("DROP TABLE IF EXISTS `$table`") }

        db.execSQL("ALTER TABLE sessions ADD COLUMN plateauedOnExercise TEXT")

        db.execSQL(
            """
            CREATE TABLE `exercise_logs_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `exerciseName` TEXT NOT NULL,
                `equipment` TEXT NOT NULL,
                `weightKg` REAL NOT NULL,
                `tulSec` INTEGER NOT NULL DEFAULT 0,
                `reps` INTEGER NOT NULL DEFAULT 0,
                `restSecActual` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `exercise_logs_new`
                (`id`, `sessionId`, `exerciseName`, `equipment`, `weightKg`, `tulSec`, `reps`, `restSecActual`)
            SELECT `id`, `sessionId`, `exerciseName`, `equipment`, `weightKg`, 0, `reps`, `restSecActual`
            FROM `exercise_logs`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `exercise_logs`")
        db.execSQL("ALTER TABLE `exercise_logs_new` RENAME TO `exercise_logs`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_logs_sessionId` ON `exercise_logs` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_logs_exerciseName` ON `exercise_logs` (`exerciseName`)")

        db.execSQL(
            """
            CREATE TABLE `exercise_configs_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `slotName` TEXT NOT NULL,
                `exerciseName` TEXT NOT NULL,
                `equipment` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL DEFAULT 1,
                `orderIndex` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        // The kettlebell slot has no Big Five equivalent — it is dropped, not carried over
        // as a dead row someone would otherwise find sitting inactive in Exercises.
        db.execSQL(
            """
            INSERT INTO `exercise_configs_new`
                (`id`, `slotName`, `exerciseName`, `equipment`, `isActive`, `orderIndex`)
            SELECT `id`, `slotName`, `exerciseName`, `equipment`, `isActive`, `orderIndex`
            FROM `exercise_configs`
            WHERE `exerciseName` != 'Kettlebell swings'
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `exercise_configs`")
        db.execSQL("ALTER TABLE `exercise_configs_new` RENAME TO `exercise_configs`")

        db.execSQL(
            """
            CREATE TABLE `settings_new` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `reminderTimeMinutes` INTEGER NOT NULL DEFAULT 1080,
                `weighInDay` INTEGER NOT NULL DEFAULT 6,
                `weighInTimeMinutes` INTEGER NOT NULL DEFAULT 480,
                `trainingIntention` TEXT,
                `firstSetCueDismissed` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `settings_new`
                (`id`, `reminderTimeMinutes`, `weighInDay`, `weighInTimeMinutes`,
                 `trainingIntention`, `firstSetCueDismissed`)
            SELECT `id`, `reminderTimeMinutes`, `weighInDay`, `weighInTimeMinutes`,
                   `trainingIntention`, `lockedPositionCueDismissed`
            FROM `settings`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `settings`")
        db.execSQL("ALTER TABLE `settings_new` RENAME TO `settings`")
    }
}

val ALL_MIGRATIONS: Array<Migration> =
    arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
