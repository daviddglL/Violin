package com.violinmaster.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DAO tests for UserDao covering all CRUD operations.
 *
 * Uses a minimal in-memory database with only [UserAccount] entity.
 * REQ-ARCH-004-S2: UserDao manages user_accounts table.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserDaoTest {

    @Database(entities = [UserAccount::class], version = 1)
    abstract class TestUserDatabase : RoomDatabase() {
        abstract fun userDao(): UserDao
    }

    private lateinit var database: TestUserDatabase
    private lateinit var dao: UserDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TestUserDatabase::class.java)
            .build()
        dao = database.userDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Insert + Query ──────────────────────────────────────────────────

    @Test
    fun `insertUser then getUserByUsername returns the user`() = runTest {
        val user = UserAccount(
            username = "teacher_jane",
            role = "TEACHER",
            hashedPassword = "abc123hash",
            salt = "salt123",
            teacherCode = "TEACH-0001",
            points = 150,
            skillLevel = "Advanced"
        )
        dao.insertUser(user)
        advanceUntilIdle()

        val retrieved = dao.getUserByUsername("teacher_jane")
        assertNotNull(retrieved)
        assertEquals("TEACHER", retrieved!!.role)
        assertEquals("TEACH-0001", retrieved.teacherCode)
        assertEquals(150, retrieved.points)
        assertEquals("Advanced", retrieved.skillLevel)
    }

    @Test
    fun `getUserByUsername returns null for unknown user`() = runTest {
        val retrieved = dao.getUserByUsername("ghost_user")
        assertNull(retrieved)
    }

    // ── Upsert (OnConflict) ─────────────────────────────────────────────

    @Test
    fun `insertUser overwrites on conflict`() = runTest {
        val original = UserAccount(
            username = "student_bob",
            role = "STUDENT",
            hashedPassword = "hash1",
            salt = "salt1",
            points = 50
        )
        dao.insertUser(original)
        advanceUntilIdle()

        val updated = UserAccount(
            username = "student_bob",
            role = "STUDENT",
            hashedPassword = "hash1",
            salt = "salt1",
            points = 100
        )
        dao.insertUser(updated)
        advanceUntilIdle()

        val retrieved = dao.getUserByUsername("student_bob")
        assertNotNull(retrieved)
        assertEquals(100, retrieved!!.points)
    }

    // ── Get All ─────────────────────────────────────────────────────────

    @Test
    fun `getAllUsers returns users ordered by username`() = runTest {
        dao.insertUser(
            UserAccount(username = "zoe", role = "STUDENT", hashedPassword = "h", salt = "s")
        )
        dao.insertUser(
            UserAccount(username = "alice", role = "STUDENT", hashedPassword = "h", salt = "s")
        )
        advanceUntilIdle()

        val users = dao.getAllUsers().first()
        assertEquals(2, users.size)
        assertEquals("alice", users[0].username)
        assertEquals("zoe", users[1].username)
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Test
    fun `updateUser modifies an existing user`() = runTest {
        val original = UserAccount(
            username = "update_me",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            points = 10,
            skillLevel = "Beginner"
        )
        dao.insertUser(original)
        advanceUntilIdle()

        val updated = original.copy(points = 200, skillLevel = "Intermediate")
        dao.updateUser(updated)
        advanceUntilIdle()

        val retrieved = dao.getUserByUsername("update_me")
        assertNotNull(retrieved)
        assertEquals(200, retrieved!!.points)
        assertEquals("Intermediate", retrieved.skillLevel)
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Test
    fun `deleteUser removes the user`() = runTest {
        val user = UserAccount(
            username = "delete_me",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt"
        )
        dao.insertUser(user)
        advanceUntilIdle()

        dao.deleteUser(user)
        advanceUntilIdle()

        val retrieved = dao.getUserByUsername("delete_me")
        assertNull(retrieved)
    }

    @Test
    fun `getAllUsers returns empty for empty table`() = runTest {
        val users = dao.getAllUsers().first()
        assertTrue(users.isEmpty())
    }

    // ── firebaseUid field (REQ-AUTH-001, REQ-DB-006) ────────────────────

    @Test
    fun `UserAccount firebaseUid defaults to null`() = runTest {
        val user = UserAccount(
            username = "test_firebaseUid",
            role = "FREELANCER",
            hashedPassword = "hash_fbu",
            salt = "salt_fbu",
            firebaseUid = null
        )
        assertNull(user.firebaseUid)
    }

    @Test
    fun `UserAccount can store non-null firebaseUid`() = runTest {
        val user = UserAccount(
            username = "cloud_user_1",
            role = "FREELANCER",
            hashedPassword = "hash_fbu2",
            salt = "salt_fbu2",
            firebaseUid = "abc123def456"
        )
        assertEquals("abc123def456", user.firebaseUid)
        assertEquals("cloud_user_1", user.username)
    }

    // ── Firestore UID queries (REQ-AUTH-001) ────────────────────────────

    @Test
    fun `getUserByFirebaseUid returns user when found`() = runTest {
        dao.insertUser(
            UserAccount(
                username = "cloud_user_uid",
                role = "FREELANCER",
                hashedPassword = "hash_uid",
                salt = "salt_uid",
                firebaseUid = "firebase_abc_123"
            )
        )
        advanceUntilIdle()

        val retrieved = dao.getUserByFirebaseUid("firebase_abc_123")
        assertNotNull("User with matching firebaseUid should be found", retrieved)
        assertEquals("cloud_user_uid", retrieved!!.username)
        assertEquals("FREELANCER", retrieved.role)
    }

    @Test
    fun `getUserByFirebaseUid returns null when not found`() = runTest {
        val retrieved = dao.getUserByFirebaseUid("nonexistent_uid")
        assertNull("No user should match a non-existent firebaseUid", retrieved)
    }

    @Test
    fun `getUserByFirebaseUid returns null for null firebaseUid users`() = runTest {
        dao.insertUser(
            UserAccount(
                username = "pin_only",
                role = "STUDENT",
                hashedPassword = "hash_pin",
                salt = "salt_pin",
                firebaseUid = null
            )
        )
        advanceUntilIdle()

        // Querying by any non-null UID should not return the pin-only user
        val retrieved = dao.getUserByFirebaseUid("some_uid")
        assertNull("Null-firebaseUid user should not match any UID query", retrieved)
    }

    @Test
    fun `updateFirebaseUid sets firebaseUid on existing user`() = runTest {
        dao.insertUser(
            UserAccount(
                username = "link_me_uid",
                role = "FREELANCER",
                hashedPassword = "hash_link",
                salt = "salt_link"
            )
        )
        advanceUntilIdle()

        dao.updateFirebaseUid("link_me_uid", "linked_uid_xyz")
        advanceUntilIdle()

        val user = dao.getUserByUsername("link_me_uid")
        assertNotNull(user)
        assertEquals("linked_uid_xyz", user!!.firebaseUid)
    }

    @Test
    fun `updateFirebaseUid overwrites previous firebaseUid`() = runTest {
        dao.insertUser(
            UserAccount(
                username = "relink_user",
                role = "FREELANCER",
                hashedPassword = "hash_rl",
                salt = "salt_rl",
                firebaseUid = "old_uid"
            )
        )
        advanceUntilIdle()

        dao.updateFirebaseUid("relink_user", "new_uid_2026")
        advanceUntilIdle()

        val user = dao.getUserByUsername("relink_user")
        assertNotNull(user)
        assertEquals("new_uid_2026", user!!.firebaseUid)
    }

    @Test
    fun `persist and retrieve user with firebaseUid via Room round-trip`() = runTest {
        val original = UserAccount(
            username = "roundtrip_uid",
            role = "TEACHER",
            hashedPassword = "hash_rt",
            salt = "salt_rt",
            teacherCode = "TC-RT",
            points = 300,
            skillLevel = "Advanced",
            firebaseUid = "uid_roundtrip_test"
        )
        dao.insertUser(original)
        advanceUntilIdle()

        val byUid = dao.getUserByFirebaseUid("uid_roundtrip_test")
        assertNotNull(byUid)
        assertEquals("roundtrip_uid", byUid!!.username)
        assertEquals("TEACHER", byUid.role)
        assertEquals("TC-RT", byUid.teacherCode)
        assertEquals(300, byUid.points)
        assertEquals("Advanced", byUid.skillLevel)
        assertEquals("uid_roundtrip_test", byUid.firebaseUid)
    }
}
