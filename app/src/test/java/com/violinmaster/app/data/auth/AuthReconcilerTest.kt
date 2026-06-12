package com.violinmaster.app.data.auth

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.data.UserDao
import com.violinmaster.app.data.firebase.IFirestoreUsers
import com.violinmaster.app.data.firebase.UserDoc
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [AuthReconciler] — the UserAccount reconciliation logic after
 * Google Sign-In/Firebase Auth.
 *
 * Uses an in-memory Room database for UserDao and an in-memory fake
 * for Firestore user collection to test all reconciliation paths.
 *
 * REQ-AUTH-001: Google Sign-In links Firebase UID.
 * REQ-AUTH-002: Migration of existing PIN-only users.
 * REQ-AUTH-003: New user Google flow auto-creates account.
 * REQ-AUTH-004: Edge cases (reinstall, username conflict, cross-state).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthReconcilerTest {

    @Database(entities = [UserAccount::class], version = 1)
    abstract class TestReconcilerDatabase : RoomDatabase() {
        abstract fun userDao(): UserDao
    }

    private lateinit var database: TestReconcilerDatabase
    private lateinit var userDao: UserDao
    private lateinit var firestoreFake: FirestoreUserFake
    private lateinit var reconciler: AuthReconciler

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TestReconcilerDatabase::class.java)
            .build()
        userDao = database.userDao()
        firestoreFake = FirestoreUserFake()
        reconciler = AuthReconciler(userDao, firestoreFake)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── REQ-AUTH-001: New user — neither Firestore nor Room has account ──

    @Test
    fun `new Google user creates UserAccount in Room and Firestore`() = runTest {
        val result = reconciler.reconcileAfterGoogleSignIn(
            firebaseUid = "new_uid_001",
            googleEmail = "newuser@gmail.com",
            googleDisplayName = "New User"
        )
        advanceUntilIdle()

        assertNotNull(result)
        assertEquals("newuser", result!!.username) // Derived from email
        assertEquals("FREELANCER", result.role)
        assertEquals(0, result.points)
        assertEquals("Beginner", result.skillLevel)
        assertEquals("new_uid_001", result.firebaseUid)

        // Verify Room cache
        val cached = userDao.getUserByFirebaseUid("new_uid_001")
        assertNotNull(cached)
        assertEquals("newuser", cached!!.username)

        // Verify Firestore write
        assertEquals(1, firestoreFake.documents.size)
        assertEquals("newuser", firestoreFake.documents["new_uid_001"]?.username)
    }

    // ── REQ-AUTH-001: Returning user — Firestore has doc, Room is empty ──

    @Test
    fun `returning Google user restores account from Firestore to Room`() = runTest {
        // Pre-populate Firestore with existing user doc
        firestoreFake.documents["existing_uid"] = UserDoc(
            username = "returning_user",
            role = "STUDENT",
            points = 500,
            skillLevel = "Intermediate",
            birthYear = 2008,
            firebaseUid = "existing_uid"
        )

        val result = reconciler.reconcileAfterGoogleSignIn(
            firebaseUid = "existing_uid",
            googleEmail = "returning@gmail.com",
            googleDisplayName = "Returning User"
        )
        advanceUntilIdle()

        assertNotNull(result)
        assertEquals("returning_user", result!!.username)
        assertEquals("STUDENT", result.role)
        assertEquals(500, result.points)
        assertEquals("Intermediate", result.skillLevel)
        assertEquals(2008, result.birthYear)
        assertEquals("existing_uid", result.firebaseUid)

        // Verify Room cache was populated from Firestore
        val cached = userDao.getUserByFirebaseUid("existing_uid")
        assertNotNull(cached)
        assertEquals("returning_user", cached!!.username)
    }

    // ── REQ-AUTH-002: Existing PIN user links Google ──

    @Test
    fun `existing PIN user links Google account updates firebaseUid and uploads to Firestore`() = runTest {
        // Pre-populate Room with PIN-only user
        userDao.insertUser(
            UserAccount(
                username = "pin_user_1",
                role = "FREELANCER",
                hashedPassword = "hash_pin1",
                salt = "salt_pin1",
                points = 150,
                skillLevel = "Intermediate",
                birthYear = 2000,
                firebaseUid = null
            )
        )
        advanceUntilIdle()

        // Simulate Google sign-in on the SAME device where PIN user exists.
        // The caller knows the username (from current session) and passes it.
        val result = reconciler.linkExistingUserToFirebaseUid(
            username = "pin_user_1",
            firebaseUid = "link_uid_002",
            googleDisplayName = "Pin User"
        )
        advanceUntilIdle()

        assertNotNull(result)
        assertEquals("pin_user_1", result!!.username)
        assertEquals("link_uid_002", result.firebaseUid)
        assertEquals(150, result.points) // Preserved

        // Verify Room updated
        val cached = userDao.getUserByUsername("pin_user_1")
        assertEquals("link_uid_002", cached!!.firebaseUid)

        // Verify Firestore upload
        assertEquals(1, firestoreFake.documents.size)
        assertEquals("pin_user_1", firestoreFake.documents["link_uid_002"]?.username)
        assertEquals(150, firestoreFake.documents["link_uid_002"]?.points)
    }

    // ── REQ-AUTH-004: Reinstall — Firestore has doc, Room is empty ──

    @Test
    fun `reinstall recovers account from Firestore`() = runTest {
        firestoreFake.documents["reinstall_uid"] = UserDoc(
            username = "reinstall_user",
            role = "TEACHER",
            points = 1200,
            skillLevel = "Advanced",
            birthYear = 1995,
            firebaseUid = "reinstall_uid"
        )

        val result = reconciler.reconcileAfterGoogleSignIn(
            firebaseUid = "reinstall_uid",
            googleEmail = "reinstall@gmail.com",
            googleDisplayName = "Reinstall User"
        )
        advanceUntilIdle()

        assertNotNull(result)
        assertEquals("reinstall_user", result!!.username)
        assertEquals("TEACHER", result.role)
        assertEquals(1200, result.points)
    }

    // ── REQ-AUTH-004: Username conflict with discriminator ──

    @Test
    fun `username conflict appends discriminator for new accounts`() = runTest {
        // Existing local user occupies the username
        userDao.insertUser(
            UserAccount(
                username = "john",
                role = "STUDENT",
                hashedPassword = "hash_j",
                salt = "salt_j",
                firebaseUid = "some_other_uid"
            )
        )
        advanceUntilIdle()

        // Another Google user with same email prefix tries to register
        val result = reconciler.reconcileAfterGoogleSignIn(
            firebaseUid = "new_john_uid",
            googleEmail = "john@gmail.com",
            googleDisplayName = "John Different"
        )
        advanceUntilIdle()

        // Should get a discriminator
        assertNotNull(result)
        assertNotNull(result!!.firebaseUid)
        // The username should NOT be "john" — it should have a discriminator
        val username = result.username
        assert(username != "john") { "Username should have discriminator, got: $username" }
        assert(username.startsWith("john")) { "Username should start with base, got: $username" }
    }

    // ── REQ-AUTH-004: Firestore exists, Room also has local ──

    @Test
    fun `Firestore and Room both exist — Room is updated from Firestore`() = runTest {
        // Room has stale data
        userDao.insertUser(
            UserAccount(
                username = "synced_user",
                role = "FREELANCER",
                hashedPassword = "old_hash",
                salt = "old_salt",
                points = 50,
                skillLevel = "Beginner",
                firebaseUid = "sync_uid"
            )
        )
        advanceUntilIdle()

        // Firestore has fresher data
        firestoreFake.documents["sync_uid"] = UserDoc(
            username = "synced_user",
            role = "STUDENT",
            points = 800,
            skillLevel = "Advanced",
            birthYear = 2005,
            firebaseUid = "sync_uid"
        )

        val result = reconciler.reconcileAfterGoogleSignIn(
            firebaseUid = "sync_uid",
            googleEmail = "synced@gmail.com",
            googleDisplayName = "Synced User"
        )
        advanceUntilIdle()

        assertNotNull(result)
        assertEquals("synced_user", result!!.username)
        // Room should be updated with Firestore data (source of truth)
        assertEquals("STUDENT", result.role)
        assertEquals(800, result.points)
        assertEquals("Advanced", result.skillLevel)
    }
}

/**
 * In-memory fake for Firestore user collection — enables testing
 * AuthReconciler without Firebase emulator.
 */
class FirestoreUserFake : IFirestoreUsers {
    val documents = mutableMapOf<String, UserDoc>()

    override suspend fun getUserDoc(uid: String): UserDoc? = documents[uid]

    override suspend fun setUserDoc(uid: String, doc: UserDoc) {
        documents[uid] = doc
    }
}
