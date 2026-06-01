package com.violinmaster.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.violinmaster.app.data.local.CachedMessage

/**
 * Room database for Violin Master app.
 *
 * Version history:
 * - v1: Initial schema (4 entities)
 * - v2: Current production schema (identityHash preserved)
 * - v3: No schema changes — migration establishes safe upgrade path,
 *       replacing destructive fallback with explicit [MIGRATION_2_3].
 * - v4: Add birthYear column to UserAccount for age verification.
 * - v5: Add cached_messages table for teacher-student chat offline cache.
 */
@Database(
    entities = [
        PracticeSession::class,
        LessonProgress::class,
        UserAccount::class,
        Assignment::class,
        CachedMessage::class
    ],
    version = 5,
    exportSchema = true
)
abstract class PracticeDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun lessonDao(): LessonDao
    abstract fun userDao(): UserDao
    abstract fun assignmentDao(): AssignmentDao

    @Deprecated(
        "Use individual DAOs (sessionDao, lessonDao, userDao, assignmentDao) instead.",
        replaceWith = ReplaceWith("sessionDao()")
    )
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

        /**
         * Migration from version 4 to version 5.
         *
         * Adds cached_messages table for teacher-student chat offline cache.
         * REQ-CC-002: Room migration 4→5.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE cached_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        assignmentId TEXT NOT NULL,
                        senderUsername TEXT NOT NULL,
                        senderRole TEXT NOT NULL,
                        text TEXT NOT NULL,
                        attachmentUrl TEXT NOT NULL DEFAULT '',
                        attachmentType TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL,
                        read INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX idx_cached_msg_assignment ON cached_messages(assignmentId, timestamp ASC)"
                )
            }
        }
    }
}
