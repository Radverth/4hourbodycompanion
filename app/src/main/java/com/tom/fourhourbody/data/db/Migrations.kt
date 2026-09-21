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

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
