package com.tom.fourhourbody.data.db

import androidx.room.migration.Migration

/**
 * Migrations live here from the first schema change onward — never
 * `fallbackToDestructiveMigration`, because the log history is the whole point of the app.
 *
 * Adding one:
 *  1. bump `version` in [AppDatabase];
 *  2. add `val MIGRATION_1_2 = Migration(1, 2) { db -> db.execSQL("ALTER TABLE ...") }`;
 *  3. add it to [ALL_MIGRATIONS];
 *  4. the exported JSON under app/schemas/ is what MigrationTestHelper verifies against.
 */
val ALL_MIGRATIONS: Array<Migration> = emptyArray()
