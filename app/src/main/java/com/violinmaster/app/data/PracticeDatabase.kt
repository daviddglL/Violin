package com.violinmaster.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for Violin Master app.
 *
 * Version history:
 * - v1: Initial schema (4 entities)
 * - v2: Current production schema (identityHash preserved)
 * - v3: No schema changes — migration establishes safe upgrade path,
 *       replacing destructive fallback with explicit [MIGRATION_2_3].
 */
@Database(
    entities = [
        PracticeSession::class,
        LessonProgress::class,
        UserAccount::class,
        Assignment::class
    ],
    version = 3,
    exportSchema = true
)
abstract class PracticeDatabase : RoomDatabase() {
    abstract fun practiceDao(): PracticeDao

    companion object {
        /**
         * Migration from version 2 to version 3.
         *
         * No-op: schema unchanged. This migration exists to replace
         * [fallbackToDestructiveMigration] with a safe upgrade path
         * that preserves all user data.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema changes — data preserved as-is
            }
        }
    }
}
