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
 * Occam's Protocol out, Body by Science in.
 *
 * This is the largest migration in the app so far because the training pillar's primary
 * measure changed: reps at a 5/5 tempo became seconds under load at 10/10. SQLite on the
 * minimum supported API cannot drop a column, so the three tables whose shape changed are
 * rebuilt and copied rather than altered.
 *
 * The one judgement call is what happens to sets logged under the old protocol. They have a
 * rep count and no TUL. Discarding them would throw away real training; leaving them at zero
 * would make every old exercise look like a failed set. The old tempo guide enforced five
 * seconds up and five down, so a logged rep genuinely was about ten seconds under load, and
 * `tulSeconds = reps * 10` is a fair reading of what happened. It is still a reading, so the
 * rows are marked `tulDerived = 1`, the original rep count is kept beside it, and anywhere
 * these figures are shown they can say they were inferred. A number the app cannot tell apart
 * from a measured one is worse than no number.
 *
 * Rest days are also raised to the new protocol's floor of seven for anyone sitting below it.
 * Under Occam's a two-day gap was correct; under this protocol it is less than a third of the
 * starting gap, and leaving it would have the scheduler asking for sessions the protocol
 * would not.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ---- sessions: wall-clock length, and which exercise set the session ran ----
        db.execSQL("ALTER TABLE sessions ADD COLUMN elapsedSessionTimeSec INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sessions ADD COLUMN kind TEXT NOT NULL DEFAULT 'STANDARD'")

        // ---- exercise_logs: reps give way to time under load ----
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_logs_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `exerciseName` TEXT NOT NULL,
                `equipment` TEXT NOT NULL,
                `weightKg` REAL NOT NULL,
                `seatPosition` TEXT,
                `tulSeconds` INTEGER NOT NULL,
                `reps` INTEGER,
                `targetTulMinSec` INTEGER NOT NULL,
                `targetTulMaxSec` INTEGER NOT NULL,
                `repCadenceSec` TEXT NOT NULL,
                `restSecActual` INTEGER NOT NULL,
                `tulDerived` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `exercise_logs_new` (
                id, sessionId, exerciseName, equipment, weightKg, seatPosition,
                tulSeconds, reps, targetTulMinSec, targetTulMaxSec, repCadenceSec,
                restSecActual, tulDerived
            )
            SELECT
                id, sessionId, exerciseName, equipment, weightKg, NULL,
                reps * 10, reps, 60, 90, '10/10',
                restSecActual, 1
            FROM `exercise_logs`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `exercise_logs`")
        db.execSQL("ALTER TABLE `exercise_logs_new` RENAME TO `exercise_logs`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_logs_sessionId` ON `exercise_logs` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_logs_exerciseName` ON `exercise_logs` (`exerciseName`)")

        // ---- exercise_configs: a different set of exercises, in a different order ----
        //
        // Dropped rather than copied. The slots themselves changed — the Big Five are five
        // named machines in a fixed order, not the four generic slots Occam's used — so there
        // is nothing in the old rows to carry across. This is not history: the logs above are
        // keyed by exercise name and are untouched. The seeder refills this on next launch.
        db.execSQL("DROP TABLE IF EXISTS `exercise_configs`")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_configs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `slotName` TEXT NOT NULL,
                `exerciseName` TEXT NOT NULL,
                `equipment` TEXT NOT NULL,
                `freeWeightEquivalent` TEXT,
                `targetTulMinSec` INTEGER NOT NULL,
                `targetTulMaxSec` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL,
                `orderIndex` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // ---- frequency_setting: the cutting phase, and the new floor on rest days ----
        db.execSQL("ALTER TABLE frequency_setting ADD COLUMN cuttingPhaseActive INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE frequency_setting SET currentRestDaysBetweenSessions = 7 WHERE currentRestDaysBetweenSessions < 7")

        // ---- sleep_logs: hours, not just a quality rating ----
        db.execSQL("ALTER TABLE sleep_logs ADD COLUMN hoursSlept REAL")

        // ---- cold_exposure_logs: the cold-water tick has no duration ----
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cold_exposure_logs_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `durationSec` INTEGER,
                `notes` TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `cold_exposure_logs_new` (id, date, type, durationSec, notes)
            SELECT id, date, type, durationSec, notes FROM `cold_exposure_logs`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `cold_exposure_logs`")
        db.execSQL("ALTER TABLE `cold_exposure_logs_new` RENAME TO `cold_exposure_logs`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cold_exposure_logs_date` ON `cold_exposure_logs` (`date`)")

        // ---- settings: the Big Three override and an adjustable cadence ----
        db.execSQL("ALTER TABLE settings ADD COLUMN bigThreeOnly INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE settings ADD COLUMN tempoUpSec INTEGER NOT NULL DEFAULT 10")
        db.execSQL("ALTER TABLE settings ADD COLUMN tempoDownSec INTEGER NOT NULL DEFAULT 10")

        // ---- new tables ----
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `plateau_technique_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sessionId` INTEGER,
                `date` INTEGER NOT NULL,
                `exerciseName` TEXT NOT NULL,
                `technique` TEXT NOT NULL,
                `notes` TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plateau_technique_logs_sessionId` ON `plateau_technique_logs` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plateau_technique_logs_date` ON `plateau_technique_logs` (`date`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `synergize_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` INTEGER NOT NULL,
                `ateUnprocessed` INTEGER NOT NULL,
                `keptCoolToday` INTEGER NOT NULL,
                `hydrationLiters` REAL,
                `stressManaged` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_synergize_logs_date` ON `synergize_logs` (`date`)")
    }
}

val ALL_MIGRATIONS: Array<Migration> =
    arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
