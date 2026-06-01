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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DAO tests for SessionDao covering all CRUD operations.
 *
 * Uses a minimal in-memory database with only [PracticeSession] entity.
 * REQ-ARCH-004-S1: SessionDao manages practice_sessions table.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionDaoTest {

    @Database(entities = [PracticeSession::class], version = 1)
    abstract class TestSessionDatabase : RoomDatabase() {
        abstract fun sessionDao(): SessionDao
    }

    private lateinit var database: TestSessionDatabase
    private lateinit var dao: SessionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TestSessionDatabase::class.java)
            .build()
        dao = database.sessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Insert + Query ─────────────────────────────────────────────────

    @Test
    fun `insertSession then getAllSessions returns the session`() = runTest {
        val session = PracticeSession(
            dateString = "2026-05-27",
            durationSeconds = 1800,
            category = "Smart Tuner",
            timestamp = 1716840000000
        )
        dao.insertSession(session)
        advanceUntilIdle()

        val sessions = dao.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals("2026-05-27", sessions[0].dateString)
        assertEquals(1800, sessions[0].durationSeconds)
        assertEquals("Smart Tuner", sessions[0].category)
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Test
    fun `deleteSession removes the session`() = runTest {
        val session = PracticeSession(
            dateString = "2026-05-27",
            durationSeconds = 600,
            category = "Metronome",
            timestamp = 1716840000000
        )
        dao.insertSession(session)
        advanceUntilIdle()

        val inserted = dao.getAllSessions().first().first()
        dao.deleteSession(inserted)
        advanceUntilIdle()

        val afterDelete = dao.getAllSessions().first()
        assertTrue(afterDelete.isEmpty())
    }

    // ── Date Range Query ────────────────────────────────────────────────

    @Test
    fun `getSessionsForDateRange returns only matching sessions`() = runTest {
        dao.insertSession(
            PracticeSession(
                dateString = "2026-05-27",
                durationSeconds = 300,
                category = "Morning",
                timestamp = 1L
            )
        )
        dao.insertSession(
            PracticeSession(
                dateString = "2026-05-28",
                durationSeconds = 600,
                category = "Evening",
                timestamp = 2L
            )
        )
        dao.insertSession(
            PracticeSession(
                dateString = "2026-05-29",
                durationSeconds = 900,
                category = "Night",
                timestamp = 3L
            )
        )
        advanceUntilIdle()

        val range = dao.getSessionsForDateRange("2026-05-27", "2026-05-28").first()
        assertEquals(2, range.size)
        val dates = range.map { it.dateString }
        assertTrue(dates.contains("2026-05-27"))
        assertTrue(dates.contains("2026-05-28"))
    }

    // ── Total Practice Time ─────────────────────────────────────────────

    @Test
    fun `getTotalPracticeTimeForDateRange sums durations correctly`() = runTest {
        dao.insertSession(
            PracticeSession(
                dateString = "2026-05-27",
                durationSeconds = 1800,
                category = "A",
                timestamp = 1L
            )
        )
        dao.insertSession(
            PracticeSession(
                dateString = "2026-05-27",
                durationSeconds = 1200,
                category = "B",
                timestamp = 2L
            )
        )
        advanceUntilIdle()

        val total = dao.getTotalPracticeTimeForDateRange("2026-05-27", "2026-05-27").first()
        assertEquals(3000, total)

        val empty = dao.getTotalPracticeTimeForDateRange("2026-05-28", "2026-05-28").first()
        assertEquals(0, empty)
    }

    // ── Data Integrity ──────────────────────────────────────────────────

    @Test
    fun `session auto-generates unique IDs`() = runTest {
        dao.insertSession(
            PracticeSession(dateString = "2026-05-27", durationSeconds = 100, category = "A")
        )
        dao.insertSession(
            PracticeSession(dateString = "2026-05-27", durationSeconds = 200, category = "B")
        )
        advanceUntilIdle()

        val sessions = dao.getAllSessions().first()
        assertEquals(2, sessions.size)
        assertTrue(sessions[0].id != sessions[1].id)
    }

    @Test
    fun `getAllSessions returns empty for empty table`() = runTest {
        val sessions = dao.getAllSessions().first()
        assertTrue(sessions.isEmpty())
    }
}
