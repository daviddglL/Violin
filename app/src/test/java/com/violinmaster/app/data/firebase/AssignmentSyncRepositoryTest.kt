package com.violinmaster.app.data.firebase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.AssignmentDao
import com.violinmaster.app.data.PracticeDatabase
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
 * Integration tests for [AssignmentSyncRepository] with in-memory Room and FakeFirestoreCollection.
 *
 * Verifies Assignment ↔ AssignmentDoc mapping and DAO integration
 * for the top-level assignments collection.
 *
 * REQ-CSYNC-004: Assignments collection with Room auto-generated Int ID as String docId.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AssignmentSyncRepositoryTest {

    private lateinit var database: PracticeDatabase
    private lateinit var assignmentDao: AssignmentDao
    private lateinit var fakeCollection: FakeFirestoreCollection<AssignmentDoc>
    private lateinit var repo: AssignmentSyncRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        assignmentDao = database.assignmentDao()
        fakeCollection = FakeFirestoreCollection()
        repo = AssignmentSyncRepository(fakeCollection, assignmentDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Write Tests ──────────────────────────────────────────────────────

    @Test
    fun `write stores assignment in both Firestore and Room`() = runTest {
        val assignment = Assignment(
            id = 0,
            title = "Practice Scales",
            description = "Play all major scales slowly",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob",
            videoTitle = "Scale Tutorial",
            videoDurationSeconds = 120,
            videoResourceUrl = "https://storage.example.com/scale.mp4",
            timestamp = 1717000000000L,
            completed = false
        )

        repo.write(assignment)
        advanceUntilIdle()

        val firestoreDocs = fakeCollection.getAllDocuments()
        assertEquals("Firestore should have 1 assignment", 1, firestoreDocs.size)
        assertEquals("Practice Scales", firestoreDocs[0].second.title)
        assertEquals("teacher_jane", firestoreDocs[0].second.teacherUsername)

        val roomAssignments = assignmentDao.getAllAssignments().first()
        assertEquals("Room should have 1 assignment", 1, roomAssignments.size)
        assertEquals("Practice Scales", roomAssignments[0].title)
    }

    @Test
    fun `write preserves all assignment fields through sync`() = runTest {
        val assignment = Assignment(
            id = 15,
            title = "Memorize Piece",
            description = "First movement of the concerto",
            teacherUsername = "teacher_sam",
            studentUsername = "ALL",
            videoTitle = "Concerto Demo",
            videoDurationSeconds = 180,
            videoResourceUrl = "https://storage.example.com/concerto.mp4",
            timestamp = 1716883200000L,
            completed = true
        )

        repo.write(assignment)
        advanceUntilIdle()

        val roomAssignment = assignmentDao.getAllAssignments().first().first()
        assertEquals(15, roomAssignment.id)
        assertEquals("Memorize Piece", roomAssignment.title)
        assertEquals("First movement of the concerto", roomAssignment.description)
        assertEquals("teacher_sam", roomAssignment.teacherUsername)
        assertEquals("ALL", roomAssignment.studentUsername)
        assertEquals("Concerto Demo", roomAssignment.videoTitle)
        assertEquals(180, roomAssignment.videoDurationSeconds)
        assertEquals("https://storage.example.com/concerto.mp4", roomAssignment.videoResourceUrl)
        assertEquals(1716883200000L, roomAssignment.timestamp)
        assertTrue(roomAssignment.completed)
    }

    // ── Observe Tests ────────────────────────────────────────────────────

    @Test
    fun `observe emits assignments from Room cache`() = runTest {
        assignmentDao.insertAssignment(
            Assignment(
                id = 0,
                title = "Cached Assignment",
                description = "From cache",
                teacherUsername = "teacher_jane",
                studentUsername = "student_bob",
                timestamp = 1717000000000L,
                completed = false
            )
        )
        advanceUntilIdle()

        val emitted = repo.observe().first()
        assertEquals("Should emit cached assignment", 1, emitted.size)
        assertEquals("Cached Assignment", emitted[0].title)
    }

    @Test
    fun `observe syncs assignments from Firestore to Room`() = runTest {
        fakeCollection.setDocument(
            "1",
            AssignmentDoc(
                id = "1",
                title = "Remote Assignment",
                description = "Synced from cloud",
                teacherUsername = "teacher_jane",
                studentUsername = "student_bob",
                timestamp = 1717000000000L,
                completed = false
            )
        )

        val emitted = repo.observe().first()

        assertEquals("Should sync from Firestore", 1, emitted.size)
        assertEquals("Remote Assignment", emitted[0].title)
        assertEquals(1, emitted[0].id)
    }

    // ── Delete Tests ─────────────────────────────────────────────────────

    @Test
    fun `delete removes assignment from both Firestore and Room`() = runTest {
        val assignment = Assignment(
            id = 77,
            title = "To Delete",
            description = "Will be removed",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob",
            timestamp = 1717000000000L,
            completed = false
        )
        repo.write(assignment)
        advanceUntilIdle()
        assertEquals(1, fakeCollection.getAllDocuments().size)

        repo.delete("77")
        advanceUntilIdle()

        assertTrue("Firestore should be empty", fakeCollection.getAllDocuments().isEmpty())
        val roomAssignments = assignmentDao.getAllAssignments().first()
        assertTrue("Room should be empty", roomAssignments.isEmpty())
    }
}
