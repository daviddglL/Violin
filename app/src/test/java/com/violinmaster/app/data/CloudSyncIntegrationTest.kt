package com.violinmaster.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.firebase.AssignmentDoc
import com.violinmaster.app.data.firebase.AssignmentSyncRepository
import com.violinmaster.app.data.firebase.FakeFirestoreCollection
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end integration tests for the complete cloud sync flow through
 * PracticeRepository with cloud enabled.
 *
 * Verifies that writes, reads, observations, updates, and deletes flow
 * correctly through the full chain: PracticeRepository → SyncRepository →
 * Firestore (Fake) + Room (in-memory).
 *
 * Uses [Dispatchers.Unconfined] for sync repo dispatchers so that
 * [callbackFlow] coroutines execute synchronously in [runTest].
 *
 * REQ-CSYNC-001: Dual-write pattern (Firestore first, then Room cache).
 * REQ-CSYNC-002: Real-time read via Observable Flow.
 * REQ-CSYNC-003: Offline resilience (Room serves cached data).
 * REQ-CSYNC-004: Entity coverage for all 4 entity types.
 * REQ-DI-003: PracticeRepository receives Firestore sync repos + Room DAOs.
 * REQ-DI-009: IPracticeRepository interface contract preserved.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudSyncIntegrationTest {

    private lateinit var database: PracticeDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var lessonDao: LessonDao
    private lateinit var userDao: UserDao
    private lateinit var assignmentDao: AssignmentDao

    private lateinit var sessionCollection: FakeFirestoreCollection<SessionDoc>
    private lateinit var lessonCollection: FakeFirestoreCollection<LessonDoc>
    private lateinit var userCollection: FakeFirestoreCollection<UserDoc>
    private lateinit var assignmentCollection: FakeFirestoreCollection<AssignmentDoc>

    private lateinit var repo: PracticeRepository

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

        // Use Unconfined so callbackFlow dispatches synchronously in runTest
        val sessionSync = SessionSyncRepository(sessionCollection, sessionDao, Dispatchers.Unconfined)
        val lessonSync = LessonSyncRepository(lessonCollection, lessonDao, Dispatchers.Unconfined)
        val userSync = UserSyncRepository(userCollection, userDao, Dispatchers.Unconfined)
        val assignmentSync = AssignmentSyncRepository(assignmentCollection, assignmentDao, Dispatchers.Unconfined)

        val config = CloudConfigWithSync()
        repo = PracticeRepository(
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
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Session: Full Lifecycle ────────────────────────────────────────────

    @Test
    fun `full session lifecycle insert observe delete through cloud facade`() = runTest {
        val session = PracticeSession(
            id = 1,
            dateString = "2026-06-12",
            durationSeconds = 1800,
            category = "Scales",
            timestamp = 1717000000000L
        )
        repo.insertSession(session)
        advanceUntilIdle()

        // Firestore should have the session
        assertEquals("Firestore should have 1 session", 1, sessionCollection.getAllDocuments().size)
        val firestoreDoc = sessionCollection.getAllDocuments().first().second
        assertEquals("Scales", firestoreDoc.category)
        assertEquals(1800, firestoreDoc.durationSeconds)
        assertEquals("2026-06-12", firestoreDoc.dateString)

        // Room DAO should have the session (via cache)
        val roomSessions = sessionDao.getAllSessions().first()
        assertEquals("Room should have 1 session", 1, roomSessions.size)
        assertEquals("Scales", roomSessions[0].category)
        assertEquals(1800, roomSessions[0].durationSeconds)
        assertEquals("2026-06-12", roomSessions[0].dateString)

        // Observe Flow (uses Unconfined → synchronous in runTest)
        val observed = repo.allSessions.first()
        assertEquals("Observe Flow should emit 1 session", 1, observed.size)
        assertEquals("Scales", observed[0].category)

        // Delete via cloud-enabled PracticeRepository
        repo.deleteSession(1)
        advanceUntilIdle()

        // Both stores should be empty
        assertTrue("Firestore should be empty after delete",
            sessionCollection.getAllDocuments().isEmpty())
        val roomAfterDelete = sessionDao.getAllSessions().first()
        assertTrue("Room should be empty after delete", roomAfterDelete.isEmpty())
    }

    @Test
    fun `multiple sessions written through cloud facade are correctly stored`() = runTest {
        val categories = listOf("Tuner", "Metronome", "Advanced Bowing")
        for ((index, category) in categories.withIndex()) {
            repo.insertSession(
                PracticeSession(
                    id = 10 + index,
                    dateString = "2026-06-${12 + index}",
                    durationSeconds = (index + 1) * 600,
                    category = category,
                    timestamp = 1717000000000L + index * 1000
                )
            )
        }
        advanceUntilIdle()

        assertEquals("Firestore should have 3 sessions", 3, sessionCollection.getAllDocuments().size)
        val allCategories = sessionCollection.getAllDocuments().map { it.second.category }.toSet()
        assertTrue("Should contain Tuner", allCategories.contains("Tuner"))
        assertTrue("Should contain Metronome", allCategories.contains("Metronome"))
        assertTrue("Should contain Advanced Bowing", allCategories.contains("Advanced Bowing"))

        val roomSessions = sessionDao.getAllSessions().first()
        assertEquals("Room should have 3 sessions", 3, roomSessions.size)

        val observed = repo.allSessions.first()
        assertEquals("Observe Flow should emit 3 sessions", 3, observed.size)
    }

    // ── Lesson: Full Lifecycle ─────────────────────────────────────────────

    @Test
    fun `full lesson lifecycle insert observe update through cloud facade`() = runTest {
        val lesson = LessonProgress(
            lessonId = "L001",
            lessonTitle = "Scales Basics",
            difficulty = "Beginner",
            completed = false,
            totalPracticedSeconds = 0,
            lastPracticedTimestamp = 0
        )
        repo.insertLessonProgress(lesson)
        advanceUntilIdle()

        // Verify Firestore
        assertEquals("Firestore should have 1 lesson", 1, lessonCollection.getAllDocuments().size)
        val firestoreDoc = lessonCollection.getAllDocuments().first().second
        assertEquals("L001", firestoreDoc.lessonId)
        assertEquals("Scales Basics", firestoreDoc.lessonTitle)
        assertEquals("Beginner", firestoreDoc.difficulty)

        // Verify Room
        val roomLesson = repo.getLessonProgressById("L001")
        assertNotNull("Room should have the lesson", roomLesson)
        assertEquals("Scales Basics", roomLesson!!.lessonTitle)
        assertTrue("Lesson should not be completed", !roomLesson.completed)

        // Verify Observe Flow
        val observed = repo.allLevelProgress.first()
        assertEquals("Observe Flow should emit 1 lesson", 1, observed.size)
        assertEquals("L001", observed[0].lessonId)

        // Update completion
        repo.updateLessonCompletion("L001", true)
        advanceUntilIdle()

        // Verify update persisted in Room and Firestore
        val updatedRoom = repo.getLessonProgressById("L001")
        assertNotNull("Updated lesson should exist", updatedRoom)
        assertTrue("Lesson should now be completed", updatedRoom!!.completed)

        val updatedFirestore = lessonCollection.getAllDocuments().first().second
        assertTrue("Firestore should have completed flag", updatedFirestore.completed)
    }

    // ── User: Full Lifecycle ───────────────────────────────────────────────

    @Test
    fun `full user lifecycle insert observe read through cloud facade`() = runTest {
        val user = UserAccount(
            username = "cloud_user",
            role = "STUDENT",
            hashedPassword = "hash",
            salt = "salt",
            firebaseUid = "firebase_uid_001"
        )
        repo.insertUser(user)
        advanceUntilIdle()

        // Verify Firestore
        assertEquals("Firestore should have 1 user", 1, userCollection.getAllDocuments().size)
        val firestoreDoc = userCollection.getAllDocuments().first().second
        assertEquals("cloud_user", firestoreDoc.username)
        assertEquals("STUDENT", firestoreDoc.role)
        assertEquals("firebase_uid_001", firestoreDoc.firebaseUid)

        // Verify Room by username (DAO, always works)
        val foundUser = repo.getUserByUsername("cloud_user")
        assertNotNull("Room should have the user", foundUser)
        assertEquals("STUDENT", foundUser!!.role)
        assertEquals("firebase_uid_001", foundUser.firebaseUid)

        // Verify Observe Flow
        val observed = repo.allUsers.first()
        assertEquals("Observe Flow should emit 1 user", 1, observed.size)
        assertEquals("cloud_user", observed[0].username)

        // Verify getByUsername for non-existing user
        val notFound = repo.getUserByUsername("nonexistent")
        assertNull("Non-existing user should return null", notFound)
    }

    @Test
    fun `user without firebaseUid is stored correctly in both stores`() = runTest {
        val user = UserAccount(
            username = "pin_user",
            role = "FREELANCER",
            hashedPassword = "hash",
            salt = "salt",
            firebaseUid = null
        )
        repo.insertUser(user)
        advanceUntilIdle()

        val firestoreDoc = userCollection.getAllDocuments().first().second
        assertEquals("Firestore firebaseUid should be empty string for null",
            "", firestoreDoc.firebaseUid)

        val foundUser = repo.getUserByUsername("pin_user")
        assertNotNull("Room should have the user", foundUser)
        assertNull("Room firebaseUid should be null", foundUser!!.firebaseUid)
    }

    // ── Assignment: Full Lifecycle ─────────────────────────────────────────

    @Test
    fun `full assignment lifecycle insert observe delete through cloud facade`() = runTest {
        val assignment = Assignment(
            id = 50,
            title = "Practice Scales",
            description = "Practice major scales for 30 minutes",
            teacherUsername = "teacher1",
            studentUsername = "student1"
        )
        repo.insertAssignment(assignment)
        advanceUntilIdle()

        // Verify Firestore
        assertEquals("Firestore should have 1 assignment", 1, assignmentCollection.getAllDocuments().size)
        val firestoreDoc = assignmentCollection.getAllDocuments().first().second
        assertEquals("Practice Scales", firestoreDoc.title)
        assertEquals("teacher1", firestoreDoc.teacherUsername)
        assertEquals("student1", firestoreDoc.studentUsername)

        // Verify Room via observe Flow
        val roomAssignments = repo.allAssignments.first()
        assertEquals("Room should have 1 assignment", 1, roomAssignments.size)
        assertEquals("Practice Scales", roomAssignments[0].title)

        // Verify filtered reads (always use DAO regardless of cloud flag)
        val byStudent = repo.getAssignmentsForStudent("student1").first()
        assertEquals("Should find 1 assignment for student1", 1, byStudent.size)
        assertEquals("Practice Scales", byStudent[0].title)

        val byTeacher = repo.getAssignmentsByTeacher("teacher1").first()
        assertEquals("Should find 1 assignment for teacher1", 1, byTeacher.size)

        val byOtherStudent = repo.getAssignmentsForStudent("other").first()
        assertTrue("Other student should have no assignments", byOtherStudent.isEmpty())

        // Delete
        repo.deleteAssignmentById(50)
        advanceUntilIdle()

        assertTrue("Firestore should be empty after delete",
            assignmentCollection.getAllDocuments().isEmpty())
        val roomAfterDelete = repo.allAssignments.first()
        assertTrue("Room should be empty after delete", roomAfterDelete.isEmpty())
    }

    @Test
    fun `updateAssignmentCompletion marks assignment as completed`() = runTest {
        assignmentDao.insertAssignment(
            Assignment(title = "Task", description = "Desc",
                teacherUsername = "T", studentUsername = "S")
        )
        advanceUntilIdle()

        repo.updateAssignmentCompletion(1, true)
        advanceUntilIdle()

        val assignments = repo.allAssignments.first()
        assertTrue("Assignment should be marked completed", assignments[0].completed)
    }

    // ── Multi-Entity Sync ──────────────────────────────────────────────────

    @Test
    fun `all four entity types can be written and observed simultaneously`() = runTest {
        repo.insertSession(
            PracticeSession(id = 100, dateString = "2026-06-12", durationSeconds = 600, category = "Tuner")
        )
        repo.insertLessonProgress(
            LessonProgress("L99", "Test Lesson", "Beginner")
        )
        repo.insertUser(
            UserAccount("multi_user", "STUDENT", "hash", "salt", firebaseUid = "uid_multi")
        )
        repo.insertAssignment(
            Assignment(id = 200, title = "Multi Task", description = "Desc",
                teacherUsername = "T", studentUsername = "multi_user")
        )
        advanceUntilIdle()

        // Verify all 4 entities in all 4 Firestore collections
        assertEquals("Session collection should have 1 doc", 1, sessionCollection.getAllDocuments().size)
        assertEquals("Lesson collection should have 1 doc", 1, lessonCollection.getAllDocuments().size)
        assertEquals("User collection should have 1 doc", 1, userCollection.getAllDocuments().size)
        assertEquals("Assignment collection should have 1 doc", 1, assignmentCollection.getAllDocuments().size)

        // Verify all 4 observe Flows emit data
        assertEquals("Observe sessions should emit 1", 1, repo.allSessions.first().size)
        assertEquals("Observe lessons should emit 1", 1, repo.allLevelProgress.first().size)
        assertEquals("Observe users should emit 1", 1, repo.allUsers.first().size)
        assertEquals("Observe assignments should emit 1", 1, repo.allAssignments.first().size)
    }

    // ── Observe Freshness After Write ──────────────────────────────────────

    @Test
    fun `observe sessions reflects newly written session`() = runTest {
        repo.insertSession(
            PracticeSession(id = 300, dateString = "2026-07-01", durationSeconds = 1200, category = "Review")
        )
        advanceUntilIdle()

        val sessions = repo.allSessions.first()
        assertEquals("Observe should show 1 session", 1, sessions.size)
        assertEquals("Review", sessions[0].category)
        assertEquals(1200, sessions[0].durationSeconds)
    }

    @Test
    fun `getSessionsByDate works with cloud enabled using DAO cache`() = runTest {
        repo.insertSession(
            PracticeSession(id = 401, dateString = "2026-06-12", durationSeconds = 100, category = "A")
        )
        repo.insertSession(
            PracticeSession(id = 402, dateString = "2026-06-12", durationSeconds = 200, category = "B")
        )
        repo.insertSession(
            PracticeSession(id = 403, dateString = "2026-06-13", durationSeconds = 300, category = "C")
        )
        advanceUntilIdle()

        val onJun12 = repo.getSessionsByDate("2026-06-12").first()
        assertEquals("Should find 2 sessions on Jun 12", 2, onJun12.size)

        val onJun13 = repo.getSessionsByDate("2026-06-13").first()
        assertEquals("Should find 1 session on Jun 13", 1, onJun13.size)
        assertEquals("C", onJun13[0].category)

        val onJun14 = repo.getSessionsByDate("2026-06-14").first()
        assertTrue("No sessions on Jun 14", onJun14.isEmpty())
    }

    // ── IPracticeRepository Contract ───────────────────────────────────────

    @Test
    fun `PracticeRepository implements IPracticeRepository when cloud enabled`() {
        assertTrue("Must implement IPracticeRepository", repo is IPracticeRepository)
    }
}
