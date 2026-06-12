package com.violinmaster.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.firebase.AssignmentDoc
import com.violinmaster.app.data.firebase.AssignmentSyncRepository
import com.violinmaster.app.data.firebase.FakeFirestoreCollection
import com.violinmaster.app.data.firebase.IFirestoreCollection
import com.violinmaster.app.data.firebase.LessonDoc
import com.violinmaster.app.data.firebase.LessonSyncRepository
import com.violinmaster.app.data.firebase.SessionDoc
import com.violinmaster.app.data.firebase.SessionSyncRepository
import com.violinmaster.app.data.firebase.UserDoc
import com.violinmaster.app.data.firebase.UserSyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validates that PracticeRepository correctly routes operations based on
 * the cloudSyncEnabled feature flag while preserving the IPracticeRepository
 * interface contract.
 *
 * REQ-DI-009: IPracticeRepository interface preserved.
 * REQ-DI-003: PracticeRepository receives Firestore sync repos + Room DAOs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PracticeRepositoryFacadeTest {

    private lateinit var database: PracticeDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var lessonDao: LessonDao
    private lateinit var userDao: UserDao
    private lateinit var assignmentDao: AssignmentDao

    // Sync repos with FakeFirestoreCollection for testability
    private lateinit var sessionCollection: FakeFirestoreCollection<SessionDoc>
    private lateinit var lessonCollection: FakeFirestoreCollection<LessonDoc>
    private lateinit var userCollection: FakeFirestoreCollection<UserDoc>
    private lateinit var assignmentCollection: FakeFirestoreCollection<AssignmentDoc>

    private lateinit var sessionSync: SessionSyncRepository
    private lateinit var lessonSync: LessonSyncRepository
    private lateinit var userSync: UserSyncRepository
    private lateinit var assignmentSync: AssignmentSyncRepository

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

        sessionSync = SessionSyncRepository(sessionCollection, sessionDao)
        lessonSync = LessonSyncRepository(lessonCollection, lessonDao)
        userSync = UserSyncRepository(userCollection, userDao)
        assignmentSync = AssignmentSyncRepository(assignmentCollection, assignmentDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Feature Flag: Cloud DISABLED (default) ─────────────────────────────

    @Test
    fun `insertSession writes to Room when cloud disabled`() = runTest {
        val config = CloudConfig() // cloudSyncEnabled = false
        val repo = PracticeRepository(
            sessionSync = sessionSync,
            lessonSync = lessonSync,
            userSync = userSync,
            assignmentSync = assignmentSync,
            sessionDao = sessionDao,
            lessonDao = lessonDao,
            userDao = userDao,
            assignmentDao = assignmentDao,
            cloudConfig = config
        )

        val session = PracticeSession(
            dateString = "2026-06-12",
            durationSeconds = 600,
            category = "Scales"
        )
        repo.insertSession(session)
        advanceUntilIdle()

        // Room should have the session
        val sessions = sessionDao.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("Scales", sessions[0].category)

        // Firestore should NOT have the session (cloud disabled)
        assertTrue("Firestore should be empty when cloud is disabled",
            sessionCollection.getAllDocuments().isEmpty())
    }

    @Test
    fun `allSessions reads from Room when cloud disabled`() = runTest {
        val config = CloudConfig()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        // Pre-seed Room directly (simulating existing data)
        sessionDao.insertSession(
            PracticeSession(dateString = "2026-06-01", durationSeconds = 300, category = "Tuner")
        )
        advanceUntilIdle()

        val sessions = repo.allSessions.first()
        assertEquals(1, sessions.size)
        assertEquals("Tuner", sessions[0].category)
    }

    // ── Feature Flag: Cloud ENABLED ────────────────────────────────────────

    @Test
    fun `insertSession writes to Firestore AND Room when cloud enabled`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        val session = PracticeSession(
            dateString = "2026-06-12",
            durationSeconds = 600,
            category = "Metronome"
        )
        repo.insertSession(session)
        advanceUntilIdle()

        // Firestore should have the session
        val firestoreDocs = sessionCollection.getAllDocuments()
        assertEquals("Firestore should have 1 session", 1, firestoreDocs.size)
        assertEquals("Metronome", firestoreDocs[0].second.category)

        // Room should also have the session (cached by sync repo)
        val roomSessions = sessionDao.getAllSessions().first()
        assertEquals("Room should have 1 session", 1, roomSessions.size)
    }

    @Test
    fun `deleteSession removes from Firestore when cloud enabled`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        // Insert via cloud-enabled path
        val session = PracticeSession(id = 42, dateString = "2026-06-01", durationSeconds = 300, category = "Test")
        repo.insertSession(session)
        advanceUntilIdle()

        assertEquals(1, sessionCollection.getAllDocuments().size)

        repo.deleteSession(42)
        advanceUntilIdle()

        assertTrue("Firestore should be empty after delete",
            sessionCollection.getAllDocuments().isEmpty())
        val roomSessions = sessionDao.getAllSessions().first()
        assertTrue("Room should be empty after delete", roomSessions.isEmpty())
    }

    // ── Filtered reads always use DAO (Room is kept in sync by observe flows) ─

    @Test
    fun `getSessionsByDate uses DAO regardless of cloud flag`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        sessionDao.insertSession(PracticeSession(dateString = "2026-06-12", durationSeconds = 100, category = "A"))
        sessionDao.insertSession(PracticeSession(dateString = "2026-06-13", durationSeconds = 200, category = "B"))
        advanceUntilIdle()

        val sessions = repo.getSessionsByDate("2026-06-12").first()
        assertEquals(1, sessions.size)
        assertEquals("A", sessions[0].category)
    }

    // ── Lesson operations ──────────────────────────────────────────────────

    @Test
    fun `insertLessonProgress writes to Firestore when cloud enabled`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        val lesson = LessonProgress("L001", "Scales Basics", "Beginner", false, 0)
        repo.insertLessonProgress(lesson)
        advanceUntilIdle()

        assertEquals(1, lessonCollection.getAllDocuments().size)
        val roomLessons = lessonDao.getAllLessons().first()
        assertEquals(1, roomLessons.size)
    }

    @Test
    fun `getLessonProgressById uses DAO regardless of cloud flag`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        lessonDao.insertLessonProgress(LessonProgress("L99", "Test", "Beginner"))
        advanceUntilIdle()

        val found = repo.getLessonProgressById("L99")
        assertNotNull(found)
        assertEquals("Test", found!!.lessonTitle)
    }

    // ── User operations ────────────────────────────────────────────────────

    @Test
    fun `insertUser writes to Firestore when cloud enabled`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        val user = UserAccount(
            username = "cloud_user", role = "STUDENT",
            hashedPassword = "hash", salt = "salt",
            firebaseUid = "uid_123"
        )
        repo.insertUser(user)
        advanceUntilIdle()

        assertEquals(1, userCollection.getAllDocuments().size)
        val roomUsers = userDao.getAllUsers().first()
        assertEquals(1, roomUsers.size)
    }

    @Test
    fun `getUserByUsername uses DAO regardless of cloud flag`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        userDao.insertUser(UserAccount("local_user", "FREELANCER", "hash", "salt"))
        advanceUntilIdle()

        val found = repo.getUserByUsername("local_user")
        assertNotNull(found)
        assertEquals("FREELANCER", found!!.role)
    }

    // ── Assignment operations ──────────────────────────────────────────────

    @Test
    fun `insertAssignment writes to Firestore when cloud enabled`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        val assignment = Assignment(
            title = "Cloud Task", description = "Test",
            teacherUsername = "T1", studentUsername = "S1"
        )
        repo.insertAssignment(assignment)
        advanceUntilIdle()

        assertEquals(1, assignmentCollection.getAllDocuments().size)
        val roomAssignments = assignmentDao.getAllAssignments().first()
        assertEquals(1, roomAssignments.size)
    }

    @Test
    fun `deleteAssignmentById removes from Firestore when cloud enabled`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        val assignment = Assignment(id = 77, title = "Del Me", description = "",
            teacherUsername = "T", studentUsername = "S")
        repo.insertAssignment(assignment)
        advanceUntilIdle()
        assertEquals(1, assignmentCollection.getAllDocuments().size)

        repo.deleteAssignmentById(77)
        advanceUntilIdle()

        assertTrue("Firestore should be empty", assignmentCollection.getAllDocuments().isEmpty())
        val roomAssignments = assignmentDao.getAllAssignments().first()
        assertTrue("Room should be empty", roomAssignments.isEmpty())
    }

    @Test
    fun `updateAssignmentCompletion uses DAO (reads from synced cache) when cloud enabled`() = runTest {
        val config = CloudConfigWithSync()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        assignmentDao.insertAssignment(Assignment(id = 1, title = "T", description = "",
            teacherUsername = "T", studentUsername = "S"))
        advanceUntilIdle()

        repo.updateAssignmentCompletion(1, true)
        advanceUntilIdle()

        val assignments = repo.allAssignments.first()
        assertTrue(assignments[0].completed)
    }

    @Test
    fun `clearSessions works when cloud disabled`() = runTest {
        val config = CloudConfig()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        sessionDao.insertSession(PracticeSession(dateString = "2026-01-01", durationSeconds = 60, category = "A"))
        sessionDao.insertSession(PracticeSession(dateString = "2026-01-01", durationSeconds = 90, category = "B"))
        advanceUntilIdle()
        assertEquals(2, sessionDao.getAllSessions().first().size)

        repo.clearSessions()
        advanceUntilIdle()

        assertTrue(sessionDao.getAllSessions().first().isEmpty())
    }

    // ── Interface contract verification ────────────────────────────────────

    @Test
    fun `PracticeRepository implements IPracticeRepository`() {
        val config = CloudConfig()
        val repo = PracticeRepository(
            sessionSync = sessionSync, lessonSync = lessonSync,
            userSync = userSync, assignmentSync = assignmentSync,
            sessionDao = sessionDao, lessonDao = lessonDao,
            userDao = userDao, assignmentDao = assignmentDao,
            cloudConfig = config
        )

        assertTrue("PracticeRepository must implement IPracticeRepository",
            repo is IPracticeRepository)
    }
}

