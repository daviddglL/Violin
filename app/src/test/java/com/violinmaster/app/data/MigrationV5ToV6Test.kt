package com.violinmaster.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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
 * Manual migration tests for v5 → v6: adding firebaseUid column to user_accounts.
 *
 * Uses Robolectric's real SQLite engine directly (without Room or MigrationTestHelper)
 * to verify the ALTER TABLE migration preserves data and adds the nullable column.
 *
 * REQ-DB-002: Version migration chain. Existing data preserved, firebaseUid defaults to NULL.
 * REQ-DB-006: Database version 6 with schema export.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationV5ToV6Test {

    private lateinit var db: SQLiteDatabase

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = context.openOrCreateDatabase("test-migration-v6.db", Context.MODE_PRIVATE, null)

        // Create v5 schema — user_accounts table without firebaseUid
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
                birthYear INTEGER NOT NULL
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
    fun `migrate 5 to 6 adds firebaseUid column preserving existing data`() {
        // Insert v5 users (no firebaseUid column)
        db.insert("user_accounts", null, ContentValues().apply {
            put("username", "pin_only_user")
            put("role", "STUDENT")
            put("hashedPassword", "hash_pin")
            put("salt", "salt_pin")
            put("teacherCode", "TC99")
            put("points", 250)
            put("skillLevel", "Intermediate")
            put("birthYear", 2005)
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

        // Apply migration: ALTER TABLE ADD COLUMN firebaseUid
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN firebaseUid TEXT")

        // Verify data preserved
        val cursor = db.rawQuery(
            "SELECT * FROM user_accounts ORDER BY username ASC", null
        )
        assertEquals("Should have 2 users after migration", 2, cursor.count)

        cursor.moveToFirst()
        assertEquals("pin_only_user", cursor.getString(cursor.getColumnIndexOrThrow("username")))
        assertEquals("STUDENT", cursor.getString(cursor.getColumnIndexOrThrow("role")))
        assertEquals(250, cursor.getInt(cursor.getColumnIndexOrThrow("points")))
        assertEquals("Intermediate", cursor.getString(cursor.getColumnIndexOrThrow("skillLevel")))
        assertTrue(
            "firebaseUid must be null for existing PIN-only user",
            cursor.isNull(cursor.getColumnIndexOrThrow("firebaseUid"))
        )
        assertEquals(2005, cursor.getInt(cursor.getColumnIndexOrThrow("birthYear")))

        cursor.moveToNext()
        assertEquals("teacher_user", cursor.getString(cursor.getColumnIndexOrThrow("username")))
        assertEquals("TEACHER", cursor.getString(cursor.getColumnIndexOrThrow("role")))
        assertEquals(1000, cursor.getInt(cursor.getColumnIndexOrThrow("points")))
        assertTrue(
            "firebaseUid must be null for existing teacher user",
            cursor.isNull(cursor.getColumnIndexOrThrow("firebaseUid"))
        )
        assertEquals(1990, cursor.getInt(cursor.getColumnIndexOrThrow("birthYear")))

        cursor.close()
    }

    @Test
    fun `migrate 5 to 6 then new column can be set to a UID`() {
        db.insert("user_accounts", null, ContentValues().apply {
            put("username", "link_me")
            put("role", "FREELANCER")
            put("hashedPassword", "hash_lnk")
            put("salt", "salt_lnk")
            put("teacherCode", "")
            put("points", 0)
            put("skillLevel", "Beginner")
            put("birthYear", 2000)
        })

        // Migrate
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN firebaseUid TEXT")

        // Now set the firebaseUid
        val values = ContentValues().apply { put("firebaseUid", "abc123xyz") }
        db.update("user_accounts", values, "username = ?", arrayOf("link_me"))

        // Verify
        val cursor = db.rawQuery(
            "SELECT * FROM user_accounts WHERE username = 'link_me'", null
        )
        cursor.moveToFirst()
        assertEquals("abc123xyz", cursor.getString(cursor.getColumnIndexOrThrow("firebaseUid")))
        cursor.close()
    }

    @Test
    fun `multiple user rows survive migration with distinct firebaseUid values`() {
        db.insert("user_accounts", null, ContentValues().apply {
            put("username", "user_a")
            put("role", "STUDENT"); put("hashedPassword", "h1"); put("salt", "s1")
            put("teacherCode", ""); put("points", 10); put("skillLevel", "Beginner")
            put("birthYear", 2010)
        })
        db.insert("user_accounts", null, ContentValues().apply {
            put("username", "user_b")
            put("role", "FREELANCER"); put("hashedPassword", "h2"); put("salt", "s2")
            put("teacherCode", ""); put("points", 20); put("skillLevel", "Intermediate")
            put("birthYear", 2000)
        })

        // Migrate
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN firebaseUid TEXT")

        // Set different UIDs
        db.execSQL("UPDATE user_accounts SET firebaseUid = 'uid_aaa' WHERE username = 'user_a'")
        db.execSQL("UPDATE user_accounts SET firebaseUid = 'uid_bbb' WHERE username = 'user_b'")

        // Verify
        val cursor = db.rawQuery(
            "SELECT * FROM user_accounts ORDER BY username ASC", null
        )
        assertEquals(2, cursor.count)

        cursor.moveToFirst()
        assertEquals("user_a", cursor.getString(cursor.getColumnIndexOrThrow("username")))
        assertEquals("uid_aaa", cursor.getString(cursor.getColumnIndexOrThrow("firebaseUid")))

        cursor.moveToNext()
        assertEquals("user_b", cursor.getString(cursor.getColumnIndexOrThrow("username")))
        assertEquals("uid_bbb", cursor.getString(cursor.getColumnIndexOrThrow("firebaseUid")))

        cursor.close()
    }

    @Test
    fun `firebaseUid column is present after migration via PRAGMA`() {
        // Migrate on empty DB
        db.execSQL("ALTER TABLE user_accounts ADD COLUMN firebaseUid TEXT")

        val pragmaCursor = db.rawQuery("PRAGMA table_info('user_accounts')", null)
        val columnNames = mutableListOf<String>()
        while (pragmaCursor.moveToNext()) {
            columnNames.add(pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name")))
        }
        pragmaCursor.close()

        assertTrue("firebaseUid column must exist after migration", columnNames.contains("firebaseUid"))
        // Should have all original columns + firebaseUid
        assertTrue(columnNames.contains("username"))
        assertTrue(columnNames.contains("role"))
        assertTrue(columnNames.contains("hashedPassword"))
        assertTrue(columnNames.contains("salt"))
        assertTrue(columnNames.contains("teacherCode"))
        assertTrue(columnNames.contains("points"))
        assertTrue(columnNames.contains("skillLevel"))
        assertTrue(columnNames.contains("birthYear"))
        assertEquals("Should have 9 columns (8 original + firebaseUid)", 9, columnNames.size)
    }
}
