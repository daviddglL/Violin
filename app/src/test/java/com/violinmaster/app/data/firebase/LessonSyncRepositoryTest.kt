package com.violinmaster.app.data.firebase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.LessonDao
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for [LessonSyncRepository] with in-memory Room and FakeFirestoreCollection.
 *
 * Verifies LessonProgress ↔ LessonDoc mapping and DAO integration
 * for the flat lesson_progress collection.
 *
 * REQ-CSYNC-004: Flat collection with compound docId `{firebaseUid}_{lessonId}`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LessonSyncRepositoryTest {

    private lateinit var database: PracticeDatabase
    private lateinit var lessonDao: LessonDao
    private lateinit var fakeCollection: FakeFirestoreCollection<LessonDoc>
    private lateinit var repo: LessonSyncRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        lessonDao = database.lessonDao()
        fakeCollection = FakeFirestoreCollection()
        repo = LessonSyncRepository(fakeCollection, lessonDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Write Tests ──────────────────────────────────────────────────────

    @Test
    fun `write stores lesson progress in both Firestore and Room`() = runTest {
        val progress = LessonProgress(
            lessonId = "lesson_01",
            lessonTitle = "Introduction",
            difficulty = "Beginner",
            completed = false,
            totalPracticedSeconds = 1200,
            lastPracticedTimestamp = 1717000000000L
        )

        repo.write(progress)
        advanceUntilIdle()

        val firestoreDocs = fakeCollection.getAllDocuments()
        assertEquals("Firestore should have 1 lesson", 1, firestoreDocs.size)
        assertEquals("lesson_01", firestoreDocs[0].second.lessonId)
        assertEquals(1200, firestoreDocs[0].second.totalPracticedSeconds)

        val roomLesson = lessonDao.getLessonProgressById("lesson_01")
        assertEquals("Room should have the lesson", "Introduction", roomLesson?.lessonTitle)
        assertEquals(1200, roomLesson?.totalPracticedSeconds)
    }

    @Test
    fun `write preserves all lesson progress fields through sync`() = runTest {
        val progress = LessonProgress(
            lessonId = "vibrato_01",
            lessonTitle = "Vibrato Basics",
            difficulty = "Intermediate",
            completed = true,
            totalPracticedSeconds = 5400,
            lastPracticedTimestamp = 1716883200000L
        )

        repo.write(progress)
        advanceUntilIdle()

        val roomLesson = lessonDao.getLessonProgressById("vibrato_01")
        assertEquals("Vibrato Basics", roomLesson?.lessonTitle)
        assertEquals("Intermediate", roomLesson?.difficulty)
        assertTrue(roomLesson?.completed == true)
        assertEquals(5400, roomLesson?.totalPracticedSeconds)
        assertEquals(1716883200000L, roomLesson?.lastPracticedTimestamp)
    }

    @Test
    fun `write overwrites existing lesson progress`() = runTest {
        lessonDao.insertLessonProgress(
            LessonProgress(
                lessonId = "scales_01",
                lessonTitle = "Scales",
                difficulty = "Beginner",
                completed = false,
                totalPracticedSeconds = 300,
                lastPracticedTimestamp = 1717000000000L
            )
        )
        advanceUntilIdle()

        repo.write(
            LessonProgress(
                lessonId = "scales_01",
                lessonTitle = "Scales Updated",
                difficulty = "Intermediate",
                completed = true,
                totalPracticedSeconds = 600,
                lastPracticedTimestamp = 1717100000000L
            )
        )
        advanceUntilIdle()

        val roomLesson = lessonDao.getLessonProgressById("scales_01")
        assertEquals("Scales Updated", roomLesson?.lessonTitle)
        assertEquals("Intermediate", roomLesson?.difficulty)
        assertTrue(roomLesson?.completed == true)
        assertEquals(600, roomLesson?.totalPracticedSeconds)
    }

    // ── Observe Tests ────────────────────────────────────────────────────

    @Test
    fun `observe emits lessons from Room cache`() = runTest {
        lessonDao.insertLessonProgress(
            LessonProgress(
                lessonId = "etude_01",
                lessonTitle = "Etude No.1",
                difficulty = "Advanced",
                completed = false,
                totalPracticedSeconds = 0,
                lastPracticedTimestamp = 0L
            )
        )
        advanceUntilIdle()

        val emitted = repo.observe().first()
        assertEquals("Should emit cached lesson", 1, emitted.size)
        assertEquals("Etude No.1", emitted[0].lessonTitle)
    }

    @Test
    fun `observe syncs from Firestore snapshot to Room`() = runTest {
        fakeCollection.setDocument(
            "lesson_remote",
            LessonDoc(
                lessonId = "lesson_remote",
                lessonTitle = "Remote Lesson",
                difficulty = "Beginner",
                completed = true,
                totalPracticedSeconds = 900,
                lastPracticedTimestamp = 1717000000000L
            )
        )

        val emitted = repo.observe().first()

        assertEquals("Should sync from Firestore", 1, emitted.size)
        assertEquals("Remote Lesson", emitted[0].lessonTitle)
    }

    // ── Delete Tests ─────────────────────────────────────────────────────

    @Test
    fun `delete removes lesson from both Firestore and Room`() = runTest {
        val progress = LessonProgress(
            lessonId = "to_delete",
            lessonTitle = "Temporary",
            difficulty = "Beginner",
            completed = false,
            totalPracticedSeconds = 100,
            lastPracticedTimestamp = 1717000000000L
        )
        repo.write(progress)
        advanceUntilIdle()
        assertEquals(1, fakeCollection.getAllDocuments().size)

        repo.delete("to_delete")
        advanceUntilIdle()

        assertTrue("Firestore should be empty", fakeCollection.getAllDocuments().isEmpty())
        val roomLesson = lessonDao.getLessonProgressById("to_delete")
        assertEquals("Room lesson should be gone", null, roomLesson)
    }
}
