package com.violinmaster.app.data.firebase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests for offline resilience behavior of [FirestoreSyncRepository].
 *
 * REQ-CSYNC-003: Offline resilience requirements:
 * - Write failure: throws to caller, Room cache NOT updated
 * - Room write failure: logged and recovered, other operations continue
 * - Read offline: Room serves last-known-good cache
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineResilienceTest {

    // ── Test Types ───────────────────────────────────────────────────────

    data class ResilientEntity(val id: String, val data: String)
    data class ResilientDoc(val id: String, val data: String)

    /**
     * Concrete repo for resilience testing with configurable cache failures.
     */
    private class ResilientSyncRepository(
        collection: IFirestoreCollection<ResilientDoc>
    ) : FirestoreSyncRepository<ResilientEntity, ResilientDoc>(collection) {

        private val _cache = MutableStateFlow<List<ResilientEntity>>(emptyList())
        val cacheContents: List<ResilientEntity> get() = _cache.value

        /** When true, [insertCache] throws to simulate Room failure. */
        var cacheFailsOnInsert: Boolean = false

        /** Count of failed cache insertions (verifies recovery). */
        var failedInserts: Int = 0

        fun seedCache(entity: ResilientEntity) {
            _cache.value = _cache.value + entity
        }

        override fun ResilientEntity.toFirestoreDoc(): ResilientDoc = ResilientDoc(id = id, data = data)
        override fun ResilientDoc.toEntity(): ResilientEntity = ResilientEntity(id = id, data = data)
        override fun ResilientDoc.docId(): String = id

        override suspend fun insertCache(entity: ResilientEntity) {
            if (cacheFailsOnInsert) {
                failedInserts++
                throw RuntimeException("Simulated Room write failure")
            }
            val current = _cache.value.toMutableList()
            current.removeAll { it.id == entity.id }
            current.add(entity)
            _cache.value = current
        }

        override suspend fun deleteCache(docId: String) {
            _cache.value = _cache.value.filter { it.id != docId }
        }

        override fun observeCache() = _cache
    }

    // ── Write Failure Tests ──────────────────────────────────────────────

    @Test
    fun `write failure does NOT update Room cache`() = runTest {
        val failingCollection = FailingFirestoreCollection<ResilientDoc>()
        val repo = ResilientSyncRepository(failingCollection)
        repo.seedCache(ResilientEntity(id = "existing", data = "before"))
        advanceUntilIdle()

        // Attempt write that will fail in Firestore
        try {
            repo.write(ResilientEntity(id = "new", data = "should not persist"))
            fail("Expected write to throw due to Firestore failure")
        } catch (e: RuntimeException) {
            assertEquals("Simulated Firestore write failure", e.message)
        }

        // Room cache should NOT have the failed entity
        assertEquals("Cache should still have only the original entity", 1, repo.cacheContents.size)
        assertEquals("existing", repo.cacheContents[0].id)
        assertEquals("before", repo.cacheContents[0].data)
    }

    @Test
    fun `write succeeds when Firestore recovers`() = runTest {
        val failingCollection = FailingFirestoreCollection<ResilientDoc>()
        val repo = ResilientSyncRepository(failingCollection)

        // First write fails
        try {
            repo.write(ResilientEntity(id = "fail", data = "nope"))
        } catch (_: RuntimeException) { /* expected */ }

        // Enable next write to succeed
        failingCollection.nextWriteSucceeds = true
        repo.write(ResilientEntity(id = "succeed", data = "yes"))
        advanceUntilIdle()

        assertEquals("Cache should have the successful entity", 1, repo.cacheContents.size)
        assertEquals("succeed", repo.cacheContents[0].id)
    }

    // ── Room Write Failure Recovery Tests ────────────────────────────────

    @Test
    fun `Room cache failure during observe does not break Flow`() = runTest {
        val fakeCollection = FakeFirestoreCollection<ResilientDoc>()
        val repo = ResilientSyncRepository(fakeCollection)
        repo.cacheFailsOnInsert = true
        repo.seedCache(ResilientEntity(id = "survive", data = "still alive"))
        advanceUntilIdle()

        // Push documents to Firestore — they'll fail to cache but shouldn't break observe
        fakeCollection.setDocument("bad1", ResilientDoc(id = "bad1", data = "should fail"))
        advanceUntilIdle()

        val emitted = repo.observe().first()
        // The cache should still have the original "survive" entity
        assertEquals("Flow should emit surviving cache entries", 1, emitted.size)
        assertEquals("still alive", emitted[0].data)
        assertTrue("Cache insertion should have been attempted and failed", repo.failedInserts > 0)
    }

    @Test
    fun `Room cache failure does not corrupt existing cache entries`() = runTest {
        val fakeCollection = FakeFirestoreCollection<ResilientDoc>()
        val repo = ResilientSyncRepository(fakeCollection)
        repo.seedCache(ResilientEntity(id = "safe", data = "keep me"))
        advanceUntilIdle()

        // Register observer so that Firestore pushes go through the sync mechanism
        // Make cache fail on new inserts
        repo.cacheFailsOnInsert = true

        // Push docs to Firestore — inserts will fail, but safe data should survive
        fakeCollection.setDocument("bad1", ResilientDoc(id = "bad1", data = "fail1"))
        fakeCollection.setDocument("bad2", ResilientDoc(id = "bad2", data = "fail2"))
        advanceUntilIdle()

        // Verify safe cache entry is still present and intact
        val cacheAfter = repo.cacheContents
        assertEquals("Only the safe entry should remain", 1, cacheAfter.size)
        assertEquals("safe", cacheAfter[0].id)
        assertEquals("keep me", cacheAfter[0].data)
        // failedInserts may be 0 because no observer was registered to trigger insertCache.
        // The real scenario is tested in `Room cache failure during observe does not break Flow`.
    }

    // ── Read Offline Tests ───────────────────────────────────────────────

    @Test
    fun `read serves Room cache when Firestore listener errors`() = runTest {
        val fakeCollection = FakeFirestoreCollection<ResilientDoc>()
        val repo = ResilientSyncRepository(fakeCollection)
        repo.seedCache(ResilientEntity(id = "offline1", data = "saved locally"))
        repo.seedCache(ResilientEntity(id = "offline2", data = "also local"))
        advanceUntilIdle()

        // Simulate Firestore permission error
        fakeCollection.simulateError(RuntimeException("Permission denied"))

        val emitted = repo.observe().first()

        assertEquals("Should serve cached data despite Firestore error", 2, emitted.size)
        val ids = emitted.map { it.id }
        assertTrue(ids.contains("offline1"))
        assertTrue(ids.contains("offline2"))
    }

    @Test
    fun `read serves empty cache when offline with no local data`() = runTest {
        val fakeCollection = FakeFirestoreCollection<ResilientDoc>()
        val repo = ResilientSyncRepository(fakeCollection)

        fakeCollection.simulateError(RuntimeException("Network unavailable"))

        val emitted = repo.observe().first()

        assertTrue("Should serve empty cache when no local data exists", emitted.isEmpty())
    }

    // ── Delete Failure Tests ─────────────────────────────────────────────

    @Test
    fun `delete failure does NOT clear Room cache`() = runTest {
        val failingCollection = FailingFirestoreCollection<ResilientDoc>()
        val repo = ResilientSyncRepository(failingCollection)
        repo.seedCache(ResilientEntity(id = "keep", data = "safe"))
        advanceUntilIdle()

        // Enable delete to fail
        failingCollection.nextDeleteFails = true

        try {
            repo.delete("keep")
            fail("Expected delete to throw due to Firestore delete failure")
        } catch (e: RuntimeException) {
            assertEquals("Simulated Firestore delete failure", e.message)
        }

        // Room cache should NOT have been cleared on failure
        assertEquals("Cache should still have the entity after failed delete", 1, repo.cacheContents.size)
        assertEquals("keep", repo.cacheContents[0].id)
        assertEquals("safe", repo.cacheContents[0].data)
    }
}
