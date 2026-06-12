package com.violinmaster.app.data.firebase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.PracticeDatabase
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.SessionDao
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
 * Integration tests for [SessionSyncRepository] with in-memory Room and FakeFirestoreCollection.
 *
 * Verifies PracticeSession ↔ SessionDoc mapping and DAO integration
 * for the session subcollection sync.
 *
 * REQ-CSYNC-004: Session collection path under users/{uid}/sessions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SessionSyncRepositoryTest {

    private lateinit var database: PracticeDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var fakeCollection: FakeFirestoreCollection<SessionDoc>
    private lateinit var repo: SessionSyncRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PracticeDatabase::class.java).build()
        sessionDao = database.sessionDao()
        fakeCollection = FakeFirestoreCollection()
        repo = SessionSyncRepository(fakeCollection, sessionDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Write Tests ──────────────────────────────────────────────────────

    @Test
    fun `write stores session in both Firestore and Room`() = runTest {
        val session = PracticeSession(
            id = 0,
            dateString = "2026-06-12",
            durationSeconds = 1800,
            category = "Smart Tuner",
            timestamp = 1717000000000L
        )

        repo.write(session)
        advanceUntilIdle()

        val firestoreDocs = fakeCollection.getAllDocuments()
        assertEquals("Firestore should have 1 session", 1, firestoreDocs.size)
        assertEquals("0", firestoreDocs[0].first)
        assertEquals(1800, firestoreDocs[0].second.durationSeconds)

        val roomSessions = sessionDao.getAllSessions().first()
        assertEquals("Room should have 1 session", 1, roomSessions.size)
        assertEquals("2026-06-12", roomSessions[0].dateString)
    }

    @Test
    fun `write preserves all session fields through sync`() = runTest {
        val session = PracticeSession(
            id = 42,
            dateString = "2026-01-01",
            durationSeconds = 3600,
            category = "Metronome",
            timestamp = 1704067200000L
        )

        repo.write(session)
        advanceUntilIdle()

        val roomSession = sessionDao.getAllSessions().first().first()
        assertEquals(42, roomSession.id)
        assertEquals("2026-01-01", roomSession.dateString)
        assertEquals(3600, roomSession.durationSeconds)
        assertEquals("Metronome", roomSession.category)
        assertEquals(1704067200000L, roomSession.timestamp)
    }

    // ── Observe Tests ────────────────────────────────────────────────────

    @Test
    fun `observe emits sessions from Room cache`() = runTest {
        val session = PracticeSession(
            id = 1,
            dateString = "2026-03-15",
            durationSeconds = 900,
            category = "Advanced Bowing",
            timestamp = 1717000000000L
        )
        sessionDao.insertSession(session)
        advanceUntilIdle()

        val emitted = repo.observe().first()
        assertEquals("Should emit cached session", 1, emitted.size)
        assertEquals("Advanced Bowing", emitted[0].category)
    }

    @Test
    fun `observe emits updated cache after Firestore snapshot`() = runTest {
        // Pre-seed Firestore
        fakeCollection.setDocument(
            "1",
            SessionDoc(
                id = "1",
                dateString = "2026-05-01",
                durationSeconds = 600,
                category = "Scales",
                timestamp = 1717000000000L
            )
        )

        val emitted = repo.observe().first()

        assertEquals("Should sync from Firestore to Room", 1, emitted.size)
        assertEquals("Scales", emitted[0].category)
    }

    // ── Delete Tests ─────────────────────────────────────────────────────

    @Test
    fun `delete removes session from both Firestore and Room`() = runTest {
        val session = PracticeSession(
            id = 99,
            dateString = "2026-06-01",
            durationSeconds = 300,
            category = "Warmup",
            timestamp = 1716883200000L
        )
        repo.write(session)
        advanceUntilIdle()
        assertEquals(1, fakeCollection.getAllDocuments().size)

        repo.delete("99")
        advanceUntilIdle()

        assertTrue("Firestore should be empty", fakeCollection.getAllDocuments().isEmpty())
        val roomSessions = sessionDao.getAllSessions().first()
        assertTrue("Room should be empty", roomSessions.isEmpty())
    }
}
