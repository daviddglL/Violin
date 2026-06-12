package com.violinmaster.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.firebase.AssignmentDoc
import com.violinmaster.app.data.firebase.AssignmentSyncRepository
import com.violinmaster.app.data.firebase.FailingFirestoreCollection
import com.violinmaster.app.data.firebase.FakeFirestoreCollection
import com.violinmaster.app.data.firebase.IFirestoreCollection
import com.violinmaster.app.data.firebase.LessonDoc
import com.violinmaster.app.data.firebase.LessonSyncRepository
import com.violinmaster.app.data.firebase.SessionDoc
import com.violinmaster.app.data.firebase.SessionSyncRepository
import com.violinmaster.app.data.firebase.UserDoc
import com.violinmaster.app.data.firebase.UserSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Edge case and error recovery tests for the cloud migration.
 *
 * Covers offline write failures, concurrent operations, Firestore error
 * resilience, cache recovery, and reinstall-style scenarios per the
 * cloud-sync spec (REQ-CSYNC-003, REQ-AUTH-004).
 *
 * REQ-CSYNC-003: Offline resilience — writes queue, reads serve cache.
 * REQ-AUTH-004: Edge cases — reinstall recovery, username conflict.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudSyncEdgeCasesTest {

    private lateinit var database: PracticeDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var lessonDao: LessonDao
    private lateinit var userDao: UserDao
    private lateinit var assignmentDao: AssignmentDao

    private lateinit var sessionCollection: FakeFirestoreCollection<SessionDoc>
    private lateinit var lessonCollection: FakeFirestoreCollection<LessonDoc>
    private lateinit var userCollection: FakeFirestoreCollection<UserDoc>
    private lateinit var assignmentCollection: FakeFirestoreCollection<AssignmentDoc>

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        sessionDao = database.sessionDao()
        lessonDao = database.lessonDao()
        userDao = database.userDao()
        assignmentDao = database.assignmentDao()

        sessionCollection = FakeFirestoreCollection()
        lessonCollection = FakeFirestoreCollection()
        userCollection = FakeFirestoreCollection()
        assignmentCollection = FakeFirestoreCollection()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private fun createRepo(
        sessionColl: IFirestoreCollection<SessionDoc> = sessionCollection,
        lessonColl: IFirestoreCollection<LessonDoc> = lessonCollection,
        userColl: IFirestoreCollection<UserDoc> = userCollection,
        assignmentColl: IFirestoreCollection<AssignmentDoc> = assignmentCollection
    ): PracticeRepository {
        val config = CloudConfigWithSync()
        return PracticeRepository(
            sessionSync = SessionSyncRepository(sessionColl, sessionDao, Dispatchers.Unconfined),
            lessonSync = LessonSyncRepository(lessonColl, lessonDao, Dispatchers.Unconfined),
            userSync = UserSyncRepository(userColl, userDao, Dispatchers.Unconfined),
            assignmentSync = AssignmentSyncRepository(assignmentColl, assignmentDao, Dispatchers.Unconfined),
            sessionDao = sessionDao,
            lessonDao = lessonDao,
            userDao = userDao,
            assignmentDao = assignmentDao,
            cloudConfig = config
        )
    }

    // ── Offline Write Failure ──────────────────────────────────────────────

    @Test
    fun `write failure does not update Room cache`() = runTest {
        val failingCollection = FailingFirestoreCollection<SessionDoc>()
        // nextWriteSucceeds defaults to false → throws on write
        val repo = createRepo(sessionColl = failingCollection)

        val session = PracticeSession(
            id = 500,
            dateString = "2026-06-12",
            durationSeconds = 600,
            category = "Should Fail"
        )

        try {
            repo.insertSession(session)
            advanceUntilIdle()
            fail("Expected write to fail with simulated Firestore error")
        } catch (e: RuntimeException) {
            // Expected: Firestore write failed
            assertTrue(e.message!!.contains("Simulated Firestore write failure"))
        }

        // Room should NOT have the session (write-through was not completed)
        val roomSessions = sessionDao.getAllSessions().first()
        assertTrue("Room should be empty after failed write", roomSessions.isEmpty())
    }

    @Test
    fun `successful write after failed write works correctly`() = runTest {
        val failingCollection = FailingFirestoreCollection<SessionDoc>()
        val repo = createRepo(sessionColl = failingCollection)

        // First write fails
        try {
            repo.insertSession(
                PracticeSession(id = 501, dateString = "2026-01-01",
                    durationSeconds = 100, category = "Fail")
            )
            advanceUntilIdle()
            fail("Expected first write to fail")
        } catch (_: RuntimeException) {
            // expected
        }

        // Allow next write to succeed
        failingCollection.nextWriteSucceeds = true

        val session = PracticeSession(
            id = 502,
            dateString = "2026-01-02",
            durationSeconds = 200,
            category = "Success"
        )
        repo.insertSession(session)
        advanceUntilIdle()

        // Room should have exactly 1 session (the successful one)
        val roomSessions = sessionDao.getAllSessions().first()
        assertEquals("Room should have 1 session after successful write", 1, roomSessions.size)
        assertEquals("Success", roomSessions[0].category)
        assertEquals(502, roomSessions[0].id)
    }

    // ── Concurrent Writes ─────────────────────────────────────────────────

    @Test
    fun `rapid consecutive writes store all entities correctly`() = runTest {
        val repo = createRepo()

        // Simulate rapid successive writes (like user practicing quickly)
        for (i in 1..10) {
            repo.insertSession(
                PracticeSession(id = 600 + i, dateString = "2026-06-12",
                    durationSeconds = i * 60, category = "Rapid $i")
            )
        }
        advanceUntilIdle()

        assertEquals("Firestore should have 10 sessions", 10, sessionCollection.getAllDocuments().size)
        val roomSessions = sessionDao.getAllSessions().first()
        assertEquals("Room should have 10 sessions", 10, roomSessions.size)

        // All categories should be present
        val categories = roomSessions.map { it.category }.toSet()
        for (i in 1..10) {
            assertTrue("Category Rapid $i should be present", categories.contains("Rapid $i"))
        }
    }

    @Test
    fun `concurrent writes to different entities do not interfere`() = runTest {
        val repo = createRepo()

        // Write to all 4 entity types simultaneously
        repo.insertSession(
            PracticeSession(id = 700, dateString = "2026-06-12",
                durationSeconds = 100, category = "Concurrent")
        )
        repo.insertLessonProgress(
            LessonProgress("L_CONCUR", "Concurrent Lesson", "Advanced")
        )
        repo.insertUser(
            UserAccount("concurrent_user", "STUDENT", "hash", "salt", firebaseUid = "uid_conc")
        )
        repo.insertAssignment(
            Assignment(id = 701, title = "Concurrent Assign", description = "Desc",
                teacherUsername = "T", studentUsername = "concurrent_user")
        )
        advanceUntilIdle()

        assertEquals("Session collection should have 1 doc", 1, sessionCollection.getAllDocuments().size)
        assertEquals("Lesson collection should have 1 doc", 1, lessonCollection.getAllDocuments().size)
        assertEquals("User collection should have 1 doc", 1, userCollection.getAllDocuments().size)
        assertEquals("Assignment collection should have 1 doc", 1, assignmentCollection.getAllDocuments().size)
    }

    // ── Firestore Error Resilience ────────────────────────────────────────

    @Test
    fun `observe continues to serve Room cache when Firestore listener errors`() = runTest {
        val repo = createRepo()

        // Pre-seed Room with data (simulates previously synced state)
        sessionDao.insertSession(
            PracticeSession(id = 800, dateString = "2026-06-01",
                durationSeconds = 300, category = "PreError")
        )
        advanceUntilIdle()

        // Trigger a Firestore listener error
        sessionCollection.simulateError(RuntimeException("Permission denied"))

        // Room cache should still serve data despite Firestore error
        val roomSessions = sessionDao.getAllSessions().first()
        assertEquals("Room should still have 1 session after Firestore error", 1, roomSessions.size)
        assertEquals("PreError", roomSessions[0].category)

        // Observe Flow should still emit cache data
        val observed = repo.allSessions.first()
        assertEquals("Observe should emit 1 session despite error", 1, observed.size)
    }

    @Test
    fun `room continues to serve stale data after Firestore listener error`() = runTest {
        val repo = createRepo()

        // Write one session, then simulate error
        repo.insertSession(
            PracticeSession(id = 801, dateString = "2026-06-01",
                durationSeconds = 500, category = "Stale")
        )
        advanceUntilIdle()

        // Verify it's in both stores
        assertEquals(1, sessionCollection.getAllDocuments().size)
        assertEquals(1, sessionDao.getAllSessions().first().size)

        // Simulate error
        sessionCollection.simulateError(RuntimeException("Permission denied"))

        // Write another session through DAO directly (simulating another source)
        sessionDao.insertSession(
            PracticeSession(id = 802, dateString = "2026-06-02",
                durationSeconds = 600, category = "NewSource")
        )
        advanceUntilIdle()

        // Room should have both sessions
        val roomSessions = sessionDao.getAllSessions().first()
        assertEquals("Room should have 2 sessions", 2, roomSessions.size)
    }

    // ── Cache Recovery: Firestore → Room on Observe ───────────────────────

    @Test
    fun `seeding Firestore before observe populates Room cache`() = runTest {
        val repo = createRepo()

        // Seed Firestore independently (simulates another device writing)
        sessionCollection.setDocument(
            "900",
            SessionDoc(
                id = "900",
                dateString = "2026-06-01",
                durationSeconds = 400,
                category = "RemoteWrite"
            )
        )
        advanceUntilIdle()

        // Observe should sync Firestore data into Room
        val observed = repo.allSessions.first()
        assertEquals("Observe should sync 1 session from Firestore", 1, observed.size)
        assertEquals("RemoteWrite", observed[0].category)

        // Room cache should now have the synced data
        val roomSessions = sessionDao.getAllSessions().first()
        assertEquals("Room should have the synced session", 1, roomSessions.size)
        assertEquals("RemoteWrite", roomSessions[0].category)
    }

    @Test
    fun `empty Firestore and empty Room produces empty observe`() = runTest {
        val repo = createRepo()

        // Both stores are empty — observe should emit empty list
        val observed = repo.allSessions.first()
        assertTrue("Observe should emit empty list for empty stores", observed.isEmpty())

        val lessonObserved = repo.allLevelProgress.first()
        assertTrue("Lesson observe should be empty", lessonObserved.isEmpty())

        val userObserved = repo.allUsers.first()
        assertTrue("User observe should be empty", userObserved.isEmpty())
    }

    // ── Reinstall Recovery Style: Firestore Data Restored to Room ─────────

    @Test
    fun `reinstall recovery seeds user and their data from Firestore`() = runTest {
        val repo = createRepo()

        // Simulate "reinstall": Firestore has user data, Room is empty
        userCollection.setDocument(
            "uid_reinstall",
            UserDoc(
                username = "returning_user",
                role = "STUDENT",
                firebaseUid = "uid_reinstall",
                points = 500,
                skillLevel = "Intermediate"
            )
        )
        // Also seed their sessions in Firestore
        sessionCollection.setDocument(
            "1001",
            SessionDoc(
                id = "1001",
                dateString = "2026-06-10",
                durationSeconds = 900,
                category = "Recovery Practice"
            )
        )
        advanceUntilIdle()

        // "App launches" → observe should sync Firestore → Room
        val users = repo.allUsers.first()
        assertEquals("Should sync 1 user from Firestore", 1, users.size)
        assertEquals("returning_user", users[0].username)
        assertEquals(500, users[0].points)

        val sessions = repo.allSessions.first()
        assertEquals("Should sync 1 session from Firestore", 1, sessions.size)
        assertEquals("Recovery Practice", sessions[0].category)
    }

    // ── Cloud Disabled vs Enabled Behavior ────────────────────────────────

    @Test
    fun `cloud disabled routes all operations to Room only`() = runTest {
        val config = CloudConfig() // cloudSyncEnabled = false
        val repo = PracticeRepository(
            sessionSync = SessionSyncRepository(sessionCollection, sessionDao, Dispatchers.Unconfined),
            lessonSync = LessonSyncRepository(lessonCollection, lessonDao, Dispatchers.Unconfined),
            userSync = UserSyncRepository(userCollection, userDao, Dispatchers.Unconfined),
            assignmentSync = AssignmentSyncRepository(assignmentCollection, assignmentDao, Dispatchers.Unconfined),
            sessionDao = sessionDao,
            lessonDao = lessonDao,
            userDao = userDao,
            assignmentDao = assignmentDao,
            cloudConfig = config
        )

        repo.insertSession(
            PracticeSession(id = 2000, dateString = "2026-06-12",
                durationSeconds = 300, category = "LocalOnly")
        )
        advanceUntilIdle()

        // Room should have the session
        assertEquals(1, sessionDao.getAllSessions().first().size)

        // Firestore should NOT have the session
        assertTrue("Firestore should be empty when cloud disabled",
            sessionCollection.getAllDocuments().isEmpty())
    }
}
