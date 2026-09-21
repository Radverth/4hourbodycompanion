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

val ALL_MIGRATIONS: Array<Migration> =
    arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
