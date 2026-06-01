package com.violinmaster.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for Room database migration from version 2 to version 3.
 *
 * Validates:
 * - Data preservation across migration (all rows survive)
 * - Schema integrity (tables exist with expected columns)
 * - Migration infrastructure works correctly
 *
 * Uses Room's [MigrationTestHelper] to create v2 databases and migrate to v3.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {

    companion object {
        private const val TEST_DB_NAME = "migration-test-db"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PracticeDatabase::class.java
    )

    @Test
    fun `migrate 2 to 3 preserves practice sessions`() {
        // Create database at version 2 and insert test data
        val dbV2 = helper.createDatabase(TEST_DB_NAME, 2).apply {
            insertPracticeSession(this, "2024-01-15", 600, "Smart Tuner", 1705315200000)
            insertPracticeSession(this, "2024-01-15", 300, "Metronome", 1705315300000)
            close()
        }

        // Migrate to version 3
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, PracticeDatabase.MIGRATION_2_3)
        dbV3.use { db ->
            // Verify practice_sessions table still exists and has data
            val cursor = db.query("SELECT * FROM practice_sessions ORDER BY id ASC")
            assertEquals("Should have 2 practice sessions after migration", 2, cursor.count)

            cursor.moveToFirst()
            assertEquals("Smart Tuner", cursor.getString(cursor.getColumnIndexOrThrow("category")))
            assertEquals(600, cursor.getInt(cursor.getColumnIndexOrThrow("durationSeconds")))

            cursor.moveToNext()
            assertEquals("Metronome", cursor.getString(cursor.getColumnIndexOrThrow("category")))
            assertEquals(300, cursor.getInt(cursor.getColumnIndexOrThrow("durationSeconds")))

            cursor.close()
        }
    }

    @Test
    fun `migrate 2 to 3 preserves lesson progress`() {
        val dbV2 = helper.createDatabase(TEST_DB_NAME, 2).apply {
            insertLessonProgress(
                this, "lesson_001", "Bow Hold Basics", "Beginner",
                completed = true, totalPracticedSeconds = 120, lastPracticedTimestamp = 1705315200000
            )
            insertLessonProgress(
                this, "lesson_002", "Vibrato Introduction", "Intermediate",
                completed = false, totalPracticedSeconds = 45, lastPracticedTimestamp = 1705315300000
            )
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, PracticeDatabase.MIGRATION_2_3)
        dbV3.use { db ->
            val cursor = db.query("SELECT * FROM lesson_progress ORDER BY lessonId ASC")
            assertEquals("Should have 2 lesson progress rows after migration", 2, cursor.count)

            cursor.moveToFirst()
            assertEquals("Bow Hold Basics", cursor.getString(cursor.getColumnIndexOrThrow("lessonTitle")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("completed")))

            cursor.moveToNext()
            assertEquals("Vibrato Introduction", cursor.getString(cursor.getColumnIndexOrThrow("lessonTitle")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("completed")))

            cursor.close()
        }
    }

    @Test
    fun `migrate 2 to 3 preserves user accounts`() {
        val dbV2 = helper.createDatabase(TEST_DB_NAME, 2).apply {
            insertUserAccount(
                this, "teacher1", "TEACHER",
                hashedPassword = "hash123", salt = "salt123", teacherCode = "TC001", points = 500
            )
            insertUserAccount(
                this, "student1", "STUDENT",
                hashedPassword = "hash456", salt = "salt456", teacherCode = "TC001", points = 200
            )
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, PracticeDatabase.MIGRATION_2_3)
        dbV3.use { db ->
            val cursor = db.query("SELECT * FROM user_accounts ORDER BY username ASC")
            assertEquals("Should have 2 user accounts after migration", 2, cursor.count)

            cursor.moveToFirst()
            assertEquals("student1", cursor.getString(cursor.getColumnIndexOrThrow("username")))
            assertEquals("STUDENT", cursor.getString(cursor.getColumnIndexOrThrow("role")))
            assertEquals(200, cursor.getInt(cursor.getColumnIndexOrThrow("points")))

            cursor.moveToNext()
            assertEquals("teacher1", cursor.getString(cursor.getColumnIndexOrThrow("username")))
            assertEquals("TEACHER", cursor.getString(cursor.getColumnIndexOrThrow("role")))
            assertEquals(500, cursor.getInt(cursor.getColumnIndexOrThrow("points")))

            cursor.close()
        }
    }

    @Test
    fun `migrate 2 to 3 preserves assignments`() {
        val dbV2 = helper.createDatabase(TEST_DB_NAME, 2).apply {
            insertAssignment(
                this, "Scale Practice", "Practice G major scale",
                teacherUsername = "teacher1", studentUsername = "student1",
                videoTitle = "G Major Tutorial", videoDurationSeconds = 60,
                videoResourceUrl = "https://cdn.example.com/video1",
                timestamp = 1705315200000, completed = false
            )
            insertAssignment(
                this, "Etude #1", "Play Kreutzer Etude #1",
                teacherUsername = "teacher1", studentUsername = "ALL",
                videoTitle = "", videoDurationSeconds = 0,
                videoResourceUrl = "",
                timestamp = 1705315300000, completed = true
            )
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, PracticeDatabase.MIGRATION_2_3)
        dbV3.use { db ->
            val cursor = db.query("SELECT * FROM student_assignments ORDER BY id ASC")
            assertEquals("Should have 2 assignments after migration", 2, cursor.count)

            cursor.moveToFirst()
            assertEquals("Scale Practice", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("student1", cursor.getString(cursor.getColumnIndexOrThrow("studentUsername")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("completed")))

            cursor.moveToNext()
            assertEquals("Etude #1", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("ALL", cursor.getString(cursor.getColumnIndexOrThrow("studentUsername")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("completed")))

            cursor.close()
        }
    }

    @Test
    fun `schema version 3 has all expected tables`() {
        val dbV2 = helper.createDatabase(TEST_DB_NAME, 2).apply { close() }
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, PracticeDatabase.MIGRATION_2_3)
        dbV3.use { db ->
            // Query sqlite_master to verify all tables exist
            val cursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
                    "AND name NOT LIKE 'room_master%' AND name NOT LIKE 'android_%' ORDER BY name"
            )

            val tableNames = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tableNames.add(cursor.getString(0))
            }
            cursor.close()

            assertTrue("Should have practice_sessions table", tableNames.contains("practice_sessions"))
            assertTrue("Should have lesson_progress table", tableNames.contains("lesson_progress"))
            assertTrue("Should have user_accounts table", tableNames.contains("user_accounts"))
            assertTrue("Should have student_assignments table", tableNames.contains("student_assignments"))
            assertEquals("Should have exactly 4 entity tables", 4, tableNames.size)
        }
    }

    @Test
    fun `version 2 data with all column types survives migration`() {
        val dbV2 = helper.createDatabase(TEST_DB_NAME, 2).apply {
            // Insert one row per entity to test column-level preservation
            insertPracticeSession(this, "2025-05-27", 900, "Advanced Bowing", 1748380000000L)
            insertLessonProgress(
                this, "lesson_full", "Complete Test Lesson", "Advanced",
                completed = true, totalPracticedSeconds = 3600, lastPracticedTimestamp = 1748380000000L
            )
            insertUserAccount(
                this, "fulluser", "FREELANCER",
                hashedPassword = "pwd_hash_full", salt = "salt_full",
                teacherCode = "FREE001", points = 1500,
                skillLevel = "Advanced"
            )
            insertAssignment(
                this, "Full Assignment", "Complete description text",
                teacherUsername = "fulluser", studentUsername = "ALL",
                videoTitle = "Full Tutorial Video", videoDurationSeconds = 180,
                videoResourceUrl = "https://cdn.example.com/full_video",
                timestamp = 1748380000000L, completed = true
            )
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, PracticeDatabase.MIGRATION_2_3)
        dbV3.use { db ->
            // Verify each entity type
            val sessionCursor = db.query("SELECT * FROM practice_sessions")
            assertEquals(1, sessionCursor.count)
            sessionCursor.moveToFirst()
            assertEquals("2025-05-27", sessionCursor.getString(sessionCursor.getColumnIndexOrThrow("dateString")))
            assertEquals(900, sessionCursor.getInt(sessionCursor.getColumnIndexOrThrow("durationSeconds")))
            assertEquals("Advanced Bowing", sessionCursor.getString(sessionCursor.getColumnIndexOrThrow("category")))
            assertEquals(1748380000000L, sessionCursor.getLong(sessionCursor.getColumnIndexOrThrow("timestamp")))
            sessionCursor.close()

            val lessonCursor = db.query("SELECT * FROM lesson_progress")
            assertEquals(1, lessonCursor.count)
            lessonCursor.moveToFirst()
            assertEquals("lesson_full", lessonCursor.getString(lessonCursor.getColumnIndexOrThrow("lessonId")))
            assertEquals("Complete Test Lesson", lessonCursor.getString(lessonCursor.getColumnIndexOrThrow("lessonTitle")))
            assertEquals("Advanced", lessonCursor.getString(lessonCursor.getColumnIndexOrThrow("difficulty")))
            assertEquals(1, lessonCursor.getInt(lessonCursor.getColumnIndexOrThrow("completed")))
            assertEquals(3600, lessonCursor.getInt(lessonCursor.getColumnIndexOrThrow("totalPracticedSeconds")))
            lessonCursor.close()

            val userCursor = db.query("SELECT * FROM user_accounts")
            assertEquals(1, userCursor.count)
            userCursor.moveToFirst()
            assertEquals("fulluser", userCursor.getString(userCursor.getColumnIndexOrThrow("username")))
            assertEquals("FREELANCER", userCursor.getString(userCursor.getColumnIndexOrThrow("role")))
            assertEquals("pwd_hash_full", userCursor.getString(userCursor.getColumnIndexOrThrow("hashedPassword")))
            assertEquals("salt_full", userCursor.getString(userCursor.getColumnIndexOrThrow("salt")))
            assertEquals(1500, userCursor.getInt(userCursor.getColumnIndexOrThrow("points")))
            assertEquals("Advanced", userCursor.getString(userCursor.getColumnIndexOrThrow("skillLevel")))
            userCursor.close()

            val assignmentCursor = db.query("SELECT * FROM student_assignments")
            assertEquals(1, assignmentCursor.count)
            assignmentCursor.moveToFirst()
            assertEquals("Full Assignment", assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow("title")))
            assertEquals("Complete description text", assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow("description")))
            assertEquals(180, assignmentCursor.getInt(assignmentCursor.getColumnIndexOrThrow("videoDurationSeconds")))
            assertEquals("https://cdn.example.com/full_video", assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow("videoResourceUrl")))
            assertEquals(1, assignmentCursor.getInt(assignmentCursor.getColumnIndexOrThrow("completed")))
            assignmentCursor.close()
        }
    }

    // ---- Insert helpers for raw SQL (pre-migration, non-Room inserts) ----

    private fun insertPracticeSession(
        db: SupportSQLiteDatabase,
        dateString: String,
        durationSeconds: Int,
        category: String,
        timestamp: Long
    ) {
        db.execSQL(
            """INSERT INTO practice_sessions (dateString, durationSeconds, category, timestamp) 
               VALUES (?, ?, ?, ?)""",
            arrayOf(dateString, durationSeconds, category, timestamp)
        )
    }

    private fun insertLessonProgress(
        db: SupportSQLiteDatabase,
        lessonId: String,
        lessonTitle: String,
        difficulty: String,
        completed: Boolean,
        totalPracticedSeconds: Int,
        lastPracticedTimestamp: Long
    ) {
        db.execSQL(
            """INSERT INTO lesson_progress (lessonId, lessonTitle, difficulty, completed, totalPracticedSeconds, lastPracticedTimestamp) 
               VALUES (?, ?, ?, ?, ?, ?)""",
            arrayOf(lessonId, lessonTitle, difficulty, if (completed) 1 else 0, totalPracticedSeconds, lastPracticedTimestamp)
        )
    }

    private fun insertUserAccount(
        db: SupportSQLiteDatabase,
        username: String,
        role: String,
        hashedPassword: String,
        salt: String,
        teacherCode: String,
        points: Int,
        skillLevel: String = "Beginner"
    ) {
        db.execSQL(
            """INSERT INTO user_accounts (username, role, hashedPassword, salt, teacherCode, points, skillLevel) 
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            arrayOf(username, role, hashedPassword, salt, teacherCode, points, skillLevel)
        )
    }

    private fun insertAssignment(
        db: SupportSQLiteDatabase,
        title: String,
        description: String,
        teacherUsername: String,
        studentUsername: String,
        videoTitle: String,
        videoDurationSeconds: Int,
        videoResourceUrl: String,
        timestamp: Long,
        completed: Boolean
    ) {
        db.execSQL(
            """INSERT INTO student_assignments (title, description, teacherUsername, studentUsername, videoTitle, videoDurationSeconds, videoResourceUrl, timestamp, completed) 
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            arrayOf(title, description, teacherUsername, studentUsername, videoTitle, videoDurationSeconds, videoResourceUrl, timestamp, if (completed) 1 else 0)
        )
    }

    /** Extension to safely close SupportSQLiteDatabase that may already be closed. */
    private fun SupportSQLiteDatabase.use(block: (SupportSQLiteDatabase) -> Unit) {
        try {
            block(this)
        } finally {
            try { close() } catch (_: Exception) { }
        }
    }
}
