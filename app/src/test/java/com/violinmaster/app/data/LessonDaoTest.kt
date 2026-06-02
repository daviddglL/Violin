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
 * DAO tests for LessonDao covering all CRUD operations.
 *
 * Uses a minimal in-memory database with only [LessonProgress] entity.
 * REQ-ARCH-004-S1: LessonDao manages lesson_progress table.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LessonDaoTest {

    @Database(entities = [LessonProgress::class], version = 1)
    abstract class TestLessonDatabase : RoomDatabase() {
        abstract fun lessonDao(): LessonDao
    }

    private lateinit var database: TestLessonDatabase
    private lateinit var dao: LessonDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TestLessonDatabase::class.java)
            .build()
        dao = database.lessonDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Insert + Query All ──────────────────────────────────────────────

    @Test
    fun `insertLesson then getAllLessons returns it`() = runTest {
        val progress = LessonProgress(
            lessonId = "mod_1",
            lessonTitle = "Intro to Bowing",
            difficulty = "Beginner",
            completed = false,
            totalPracticedSeconds = 1200,
            lastPracticedTimestamp = 1716840000000
        )
        dao.insertLesson(progress)
        advanceUntilIdle()

        val all = dao.getAllLessons().first()
        assertEquals(1, all.size)
        assertEquals("mod_1", all[0].lessonId)
        assertEquals("Intro to Bowing", all[0].lessonTitle)
        assertFalse(all[0].completed)
    }

    // ── Get By ID ───────────────────────────────────────────────────────

    @Test
    fun `getLessonById returns the matching lesson`() = runTest {
        dao.insertLesson(
            LessonProgress(
                lessonId = "mod_2",
                lessonTitle = "Vibrato",
                difficulty = "Intermediate",
                completed = false
            )
        )
        advanceUntilIdle()

        val result = dao.getLessonById("mod_2")
        assertNotNull(result)
        assertEquals("Vibrato", result!!.lessonTitle)
        assertEquals("Intermediate", result.difficulty)
    }

    @Test
    fun `getLessonById returns null for non-existent lesson`() = runTest {
        val result = dao.getLessonById("nonexistent")
        assertNull(result)
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Test
    fun `updateLesson modifies an existing lesson`() = runTest {
        val original = LessonProgress(
            lessonId = "mod_3",
            lessonTitle = "Scales",
            difficulty = "Beginner",
            completed = false,
            totalPracticedSeconds = 600
        )
        dao.insertLesson(original)
        advanceUntilIdle()

        val updated = original.copy(
            lessonTitle = "Scales Master",
            totalPracticedSeconds = 1200
        )
        dao.updateLesson(updated)
        advanceUntilIdle()

        val result = dao.getLessonById("mod_3")
        assertNotNull(result)
        assertEquals("Scales Master", result!!.lessonTitle)
        assertEquals(1200, result.totalPracticedSeconds)
    }

    // ── Mark Completed ──────────────────────────────────────────────────

    @Test
    fun `markLessonCompleted sets completed to true`() = runTest {
        dao.insertLesson(
            LessonProgress(
                lessonId = "mod_4",
                lessonTitle = "Arpeggios",
                difficulty = "Advanced",
                completed = false
            )
        )
        advanceUntilIdle()

        dao.markLessonCompleted("mod_4", true)
        advanceUntilIdle()

        val updated = dao.getLessonById("mod_4")
        assertNotNull(updated)
        assertTrue(updated!!.completed)
    }

    @Test
    fun `markLessonCompleted sets completed to false`() = runTest {
        dao.insertLesson(
            LessonProgress(
                lessonId = "mod_5",
                lessonTitle = "Etudes",
                difficulty = "Intermediate",
                completed = true
            )
        )
        advanceUntilIdle()

        dao.markLessonCompleted("mod_5", false)
        advanceUntilIdle()

        val updated = dao.getLessonById("mod_5")
        assertNotNull(updated)
        assertFalse(updated!!.completed)
    }

    // ── Get Completed ───────────────────────────────────────────────────

    @Test
    fun `getAllCompletedLessons returns only completed lessons`() = runTest {
        dao.insertLesson(
            LessonProgress(lessonId = "c1", lessonTitle = "Done 1", difficulty = "Beginner", completed = true)
        )
        dao.insertLesson(
            LessonProgress(lessonId = "c2", lessonTitle = "Done 2", difficulty = "Beginner", completed = true)
        )
        dao.insertLesson(
            LessonProgress(lessonId = "p1", lessonTitle = "Pending", difficulty = "Advanced", completed = false)
        )
        advanceUntilIdle()

        val completed = dao.getAllCompletedLessons().first()
        assertEquals(2, completed.size)
        assertTrue(completed.all { it.completed })
        assertTrue(completed.any { it.lessonId == "c1" })
        assertTrue(completed.any { it.lessonId == "c2" })
    }

    @Test
    fun `getAllCompletedLessons returns empty when none completed`() = runTest {
        dao.insertLesson(
            LessonProgress(lessonId = "p2", lessonTitle = "Pending", difficulty = "Beginner", completed = false)
        )
        advanceUntilIdle()

        val completed = dao.getAllCompletedLessons().first()
        assertTrue(completed.isEmpty())
    }
}
