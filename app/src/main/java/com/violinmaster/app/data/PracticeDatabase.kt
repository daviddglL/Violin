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
 * - v4: Add birthYear column to UserAccount for age verification.
 */
@Database(
    entities = [
        PracticeSession::class,
        LessonProgress::class,
        UserAccount::class,
        Assignment::class
    ],
    version = 4,
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

        /**
         * Migration from version 3 to version 4.
         *
         * Adds birthYear INTEGER NOT NULL DEFAULT 0 to user_accounts.
         * Existing users get 0 (legacy, not a minor).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_accounts ADD COLUMN birthYear INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
