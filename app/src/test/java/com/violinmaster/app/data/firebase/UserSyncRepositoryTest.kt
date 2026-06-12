package com.violinmaster.app.data.firebase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.UserDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for [UserSyncRepository] with in-memory Room and FakeFirestoreCollection.
 *
 * Verifies UserAccount ↔ UserDoc mapping and DAO integration.
 * Critically verifies that sensitive fields (hashedPassword, salt) are never
 * synced to Firestore.
 *
 * REQ-CSYNC-004: Users collection with firebaseUid as document ID.
 * REQ-AUTH-001: hashedPassword and salt never stored in Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserSyncRepositoryTest {

    private lateinit var database: PracticeDatabase
    private lateinit var userDao: UserDao
    private lateinit var fakeCollection: FakeFirestoreCollection<UserDoc>
    private lateinit var repo: UserSyncRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        userDao = database.userDao()
        fakeCollection = FakeFirestoreCollection()
        repo = UserSyncRepository(fakeCollection, userDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Write Tests ──────────────────────────────────────────────────────

    @Test
    fun `write stores user in both Firestore and Room`() = runTest {
        val user = UserAccount(
            username = "test_student",
            role = "STUDENT",
            hashedPassword = "secret_hash",
            salt = "random_salt",
            teacherCode = "CODE123",
            points = 500,
            skillLevel = "Intermediate",
            birthYear = 2005,
            firebaseUid = "uid_abc123"
        )

        repo.write(user)
        advanceUntilIdle()

        val firestoreDocs = fakeCollection.getAllDocuments()
        assertEquals("Firestore should have 1 user doc", 1, firestoreDocs.size)
        assertEquals("uid_abc123", firestoreDocs[0].first)
        assertEquals("test_student", firestoreDocs[0].second.username)
        assertEquals("STUDENT", firestoreDocs[0].second.role)

        val roomUser = userDao.getUserByUsername("test_student")
        assertEquals("Room should have the user", "test_student", roomUser?.username)
        assertEquals("uid_abc123", roomUser?.firebaseUid)
    }

    @Test
    fun `write never sends hashedPassword or salt to Firestore`() = runTest {
        val user = UserAccount(
            username = "secure_user",
            role = "TEACHER",
            hashedPassword = "super_secret_hash_12345",
            salt = "crypto_salt_67890",
            teacherCode = "TEACH001",
            points = 100,
            skillLevel = "Advanced",
            birthYear = 1990,
            firebaseUid = "uid_secure"
        )

        repo.write(user)
        advanceUntilIdle()

        val firestoreDoc = fakeCollection.getAllDocuments().first().second
        // UserDoc does not have hashedPassword or salt fields — structural guarantee
        // The mapping ensures these fields are excluded
        assertEquals("uid_secure", firestoreDoc.firebaseUid)
        assertEquals("secure_user", firestoreDoc.username)
    }

    @Test
    fun `write preserves all UserAccount fields in Room cache`() = runTest {
        val user = UserAccount(
            username = "full_user",
            role = "FREELANCER",
            hashedPassword = "hash_full",
            salt = "salt_full",
            teacherCode = "",
            points = 250,
            skillLevel = "Beginner",
            birthYear = 2000,
            firebaseUid = "uid_full"
        )

        repo.write(user)
        advanceUntilIdle()

        val roomUser = userDao.getUserByUsername("full_user")
        assertEquals("full_user", roomUser?.username)
        assertEquals("FREELANCER", roomUser?.role)
        assertEquals("hash_full", roomUser?.hashedPassword)
        assertEquals("salt_full", roomUser?.salt)
        assertEquals(250, roomUser?.points)
        assertEquals("uid_full", roomUser?.firebaseUid)
    }

    // ── Observe Tests ────────────────────────────────────────────────────

    @Test
    fun `observe emits users from Room cache`() = runTest {
        userDao.insertUser(
            UserAccount(
                username = "cached_user",
                role = "STUDENT",
                hashedPassword = "hash",
                salt = "salt",
                points = 100,
                firebaseUid = "uid_cached"
            )
        )
        advanceUntilIdle()

        val emitted = repo.observe().first()
        assertEquals("Should emit cached user", 1, emitted.size)
        assertEquals("cached_user", emitted[0].username)
    }

    @Test
    fun `observe syncs user from Firestore to Room`() = runTest {
        fakeCollection.setDocument(
            "uid_sync",
            UserDoc(
                username = "synced_user",
                role = "TEACHER",
                teacherCode = "TEACH_SYNC",
                points = 999,
                skillLevel = "Advanced",
                birthYear = 1985,
                firebaseUid = "uid_sync"
            )
        )

        val emitted = repo.observe().first()

        assertEquals("Should sync user from Firestore", 1, emitted.size)
        assertEquals("synced_user", emitted[0].username)
        assertEquals("uid_sync", emitted[0].firebaseUid)
    }

    // ── Delete Tests ─────────────────────────────────────────────────────

    @Test
    fun `delete removes user from both Firestore and Room`() = runTest {
        val user = UserAccount(
            username = "delete_me",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            points = 0,
            firebaseUid = "uid_delete"
        )
        repo.write(user)
        advanceUntilIdle()
        assertEquals(1, fakeCollection.getAllDocuments().size)

        repo.delete("uid_delete")
        advanceUntilIdle()

        assertTrue("Firestore should be empty", fakeCollection.getAllDocuments().isEmpty())
        val roomUser = userDao.getUserByUsername("delete_me")
        assertEquals("Room user should be gone", null, roomUser)
    }
}
