package com.violinmaster.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Manual migration tests for v6 → v7: adding security question, answer salt,
 * and answer hash columns to user_accounts.
 *
 * REQ-PINREC-006: Additive columns with empty-string defaults. Existing data preserved.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationV6ToV7Test {

    private lateinit var db: SQLiteDatabase

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = context.openOrCreateDatabase("test-migration-v7.db", Context.MODE_PRIVATE, null)

        // Create v6 schema — user_accounts with firebaseUid but no recovery fields
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS practice_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateString TEXT NOT NULL,
                durationSeconds INTEGER NOT NULL,
                category TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS lesson_progress (
                lessonId TEXT NOT NULL PRIMARY KEY,
                lessonTitle TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                completed INTEGER NOT NULL,
                totalPracticedSeconds INTEGER NOT NULL,
                lastPracticedTimestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_accounts (
                username TEXT NOT NULL PRIMARY KEY,
                role TEXT NOT NULL,
                hashedPassword TEXT NOT NULL,
                salt TEXT NOT NULL,
                teacherCode TEXT NOT NULL,
                points INTEGER NOT NULL,
                skillLevel TEXT NOT NULL,
                birthYear INTEGER NOT NULL,
                firebaseUid TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS student_assignments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                teacherUsername TEXT NOT NULL,
                studentUsername TEXT NOT NULL,
                videoTitle TEXT NOT NULL,
                videoDurationSeconds INTEGER NOT NULL,
                videoResourceUrl TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                completed INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cached_messages (
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
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `migrate 6 to 7 adds three recovery columns preserving existing data`() {
        // Insert v6 users (no recovery columns)
        db.insert("user_accounts", null, ContentValues().apply {
            put("username", "existing_user")
            put("role", "STUDENT")
            put("hashedPassword", "hash_pin")
            put("salt", "salt_pin")
            put("teacherCode", "TC99")
            put("points", 250)
            put("skillLevel", "Intermediate")
            put("birthYear", 2005)
            put("firebaseUid", "uid123")
        })
        db.insert("user_accounts", null, ContentValues().apply {
            put("username", "teacher_user")
            put("role", "TEACHER")
            put("hashedPassword", "hash_t")
            put("salt", "salt_t")
            put("teacherCode", "TEACH-0001")
            put("points", 1000)
            put("skillLevel", "Advanced")
            put("birthYear", 1990)
        })

        // Apply migration: ALTER TABLE ADD COLUMN for 3 new columns
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN securityQuestion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN securityAnswerSalt TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN securityAnswerHash TEXT NOT NULL DEFAULT ''")

        // Verify data preserved and new columns default to empty string
        val cursor = db.rawQuery(
            "SELECT * FROM user_accounts ORDER BY username ASC", null
        )
        assertEquals("Should have 2 users after migration", 2, cursor.count)

        cursor.moveToFirst()
        assertEquals("existing_user", cursor.getString(cursor.getColumnIndexOrThrow("username")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("securityQuestion")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("securityAnswerSalt")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("securityAnswerHash")))
        assertEquals("uid123", cursor.getString(cursor.getColumnIndexOrThrow("firebaseUid")))

        cursor.moveToNext()
        assertEquals("teacher_user", cursor.getString(cursor.getColumnIndexOrThrow("username")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("securityQuestion")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("securityAnswerSalt")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("securityAnswerHash")))

        cursor.close()
    }

    @Test
    fun `migrate 6 to 7 then set and read recovery fields`() {
        db.insert("user_accounts", null, ContentValues().apply {
            put("username", "recovery_user")
            put("role", "FREELANCER")
            put("hashedPassword", "hash_r")
            put("salt", "salt_r")
            put("teacherCode", "")
            put("points", 0)
            put("skillLevel", "Beginner")
            put("birthYear", 2000)
        })

        // Migrate
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN securityQuestion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN securityAnswerSalt TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN securityAnswerHash TEXT NOT NULL DEFAULT ''")

        // Set recovery fields
        val values = ContentValues().apply {
            put("securityQuestion", "recovery_q_first_pet")
            put("securityAnswerSalt", "salt_ans")
            put("securityAnswerHash", "hash_ans")
        }
        db.update("user_accounts", values, "username = ?", arrayOf("recovery_user"))

        // Verify
        val cursor = db.rawQuery(
            "SELECT * FROM user_accounts WHERE username = 'recovery_user'", null
        )
        cursor.moveToFirst()
        assertEquals("recovery_q_first_pet", cursor.getString(cursor.getColumnIndexOrThrow("securityQuestion")))
        assertEquals("salt_ans", cursor.getString(cursor.getColumnIndexOrThrow("securityAnswerSalt")))
        assertEquals("hash_ans", cursor.getString(cursor.getColumnIndexOrThrow("securityAnswerHash")))
        cursor.close()
    }

    @Test
    fun `recovery columns present after migration via PRAGMA`() {
        // Migrate on empty DB
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN securityQuestion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN securityAnswerSalt TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN securityAnswerHash TEXT NOT NULL DEFAULT ''")

        val pragmaCursor = db.rawQuery("PRAGMA table_info('user_accounts')", null)
        val columnNames = mutableListOf<String>()
        while (pragmaCursor.moveToNext()) {
            columnNames.add(pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")))
        }
        pragmaCursor.close()

        assertTrue("securityQuestion column must exist", columnNames.contains("securityQuestion"))
        assertTrue("securityAnswerSalt column must exist", columnNames.contains("securityAnswerSalt"))
        assertTrue("securityAnswerHash column must exist", columnNames.contains("securityAnswerHash"))
        assertTrue("username still present", columnNames.contains("username"))
        assertTrue("firebaseUid still present", columnNames.contains("firebaseUid"))
        assertEquals("Should have 12 columns (9 original + 3 recovery)", 12, columnNames.size)
    }
}
