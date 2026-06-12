package com.violinmaster.app.data.firebase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [FirestoreSyncRepository] base class behavior.
 *
 * Uses a concrete test subclass with [FakeFirestoreCollection] and
 * in-memory cache storage to verify the generic dual-write pattern:
 * Firestore-first write-through → Room cache → observe re-emission.
 *
 * REQ-CSYNC-001: Generic sync repository dual-write pattern.
 * REQ-CSYNC-002: observe() re-emits Room Flow after Firestore changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreSyncRepositoryTest {

    // ── Test Types ───────────────────────────────────────────────────────

    data class TestEntity(val id: String, val data: String)
    data class TestDoc(val id: String, val data: String)

    /**
     * Concrete implementation of FirestoreSyncRepository for testing.
     *
     * Exposes cache directly for test setup/assertion without needing
     * to call protected [insertCache].
     */
    private class TestSyncRepository(
        collection: IFirestoreCollection<TestDoc>
    ) : FirestoreSyncRepository<TestEntity, TestDoc>(collection) {

        // In-memory cache storage (public for test access)
        private val _cache = MutableStateFlow<List<TestEntity>>(emptyList())
        val cacheContents: List<TestEntity> get() = _cache.value

        /** Seeds the cache directly for test setup (bypasses protected insertCache). */
        fun seedCache(entity: TestEntity) {
            val current = _cache.value.toMutableList()
            val idx = current.indexOfFirst { it.id == entity.id }
            if (idx >= 0) {
                current[idx] = entity
            } else {
                current.add(entity)
            }
            _cache.value = current
        }

        override fun TestEntity.toFirestoreDoc(): TestDoc = TestDoc(id = id, data = data)
        override fun TestDoc.toEntity(): TestEntity = TestEntity(id = id, data = data)
        override fun TestDoc.docId(): String = id

        override suspend fun insertCache(entity: TestEntity) {
            val current = _cache.value.toMutableList()
            val idx = current.indexOfFirst { it.id == entity.id }
            if (idx >= 0) {
                current[idx] = entity
            } else {
                current.add(entity)
            }
            _cache.value = current
        }

        override suspend fun deleteCache(docId: String) {
            _cache.value = _cache.value.filter { it.id != docId }
        }

        override fun observeCache(): Flow<List<TestEntity>> = _cache
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private fun createTestRepo(): Pair<TestSyncRepository, FakeFirestoreCollection<TestDoc>> {
        val fakeCollection = FakeFirestoreCollection<TestDoc>()
        val repo = TestSyncRepository(fakeCollection)
        return repo to fakeCollection
    }

    // ── Write Tests ──────────────────────────────────────────────────────

    @Test
    fun `write stores entity in both Firestore and cache`() = runTest {
        val (repo, collection) = createTestRepo()
        val entity = TestEntity(id = "1", data = "hello")

        repo.write(entity)
        advanceUntilIdle()

        // Firestore should have the document
        val firestoreDocs = collection.getAllDocuments()
        assertEquals("Firestore should have one document", 1, firestoreDocs.size)
        assertEquals("1", firestoreDocs[0].first)
        assertEquals("hello", firestoreDocs[0].second.data)

        // Cache should have the entity
        assertEquals("Cache should have one entity", 1, repo.cacheContents.size)
        assertEquals("hello", repo.cacheContents[0].data)
    }

    @Test
    fun `write overwrites existing document in both stores`() = runTest {
        val (repo, collection) = createTestRepo()

        repo.write(TestEntity(id = "1", data = "original"))
        advanceUntilIdle()

        repo.write(TestEntity(id = "1", data = "updated"))
        advanceUntilIdle()

        assertEquals("Firestore should still have one document", 1, collection.getAllDocuments().size)
        assertEquals("updated", collection.getAllDocuments()[0].second.data)
        assertEquals("Cache should have updated data", "updated", repo.cacheContents[0].data)
    }

    @Test
    fun `write multiple entities stores them independently`() = runTest {
        val (repo, collection) = createTestRepo()

        repo.write(TestEntity(id = "a", data = "alpha"))
        repo.write(TestEntity(id = "b", data = "beta"))
        repo.write(TestEntity(id = "c", data = "gamma"))
        advanceUntilIdle()

        assertEquals("Firestore should have 3 documents", 3, collection.getAllDocuments().size)
        assertEquals("Cache should have 3 entities", 3, repo.cacheContents.size)
    }

    // ── Delete Tests ─────────────────────────────────────────────────────

    @Test
    fun `delete removes entity from both Firestore and cache`() = runTest {
        val (repo, collection) = createTestRepo()
        repo.write(TestEntity(id = "to_delete", data = "gone"))
        advanceUntilIdle()
        assertEquals(1, collection.getAllDocuments().size)

        repo.delete("to_delete")
        advanceUntilIdle()

        assertTrue("Firestore should be empty after delete", collection.getAllDocuments().isEmpty())
        assertTrue("Cache should be empty after delete", repo.cacheContents.isEmpty())
    }

    @Test
    fun `delete only removes the specified entity`() = runTest {
        val (repo, collection) = createTestRepo()
        repo.write(TestEntity(id = "keep", data = "stay"))
        repo.write(TestEntity(id = "remove", data = "go away"))
        advanceUntilIdle()

        repo.delete("remove")
        advanceUntilIdle()

        assertEquals("Firestore should have 1 remaining", 1, collection.getAllDocuments().size)
        assertEquals("keep", collection.getAllDocuments()[0].first)
        assertEquals("Cache should have 1 remaining", 1, repo.cacheContents.size)
        assertEquals("keep", repo.cacheContents[0].id)
    }

    // ── Observe Tests ────────────────────────────────────────────────────

    @Test
    fun `observe emits current cache state on collection`() = runTest {
        val (repo, _) = createTestRepo()
        repo.seedCache(TestEntity(id = "x", data = "cached"))

        val emitted = repo.observe().first()

        assertEquals("Should emit the cached entity", 1, emitted.size)
        assertEquals("x", emitted[0].id)
        assertEquals("cached", emitted[0].data)
    }

    @Test
    fun `observe emits empty list when cache is empty`() = runTest {
        val (repo, _) = createTestRepo()

        val emitted = repo.observe().first()

        assertTrue("Should emit empty list for empty cache", emitted.isEmpty())
    }

    @Test
    fun `observe reflects Firestore snapshot changes in cache`() = runTest {
        val (repo, collection) = createTestRepo()

        // Seed Firestore collection with a document BEFORE observing
        collection.setDocument("s1", TestDoc(id = "s1", data = "from firestore"))

        // Trigger observe() — the snapshot listener fires synchronously in the fake,
        // which launches insertCache on Dispatchers.IO.
        val flow = repo.observe()
        // Collect first emission — this will be from the Room cache
        flow.first()

        // Allow Dispatchers.IO coroutines (insertCache from Firestore snapshot) to complete
        advanceUntilIdle()

        // After syncing, the cache should contain the Firestore document
        val cacheAfter = repo.cacheContents
        assertTrue("Cache should contain synced entity after observer processes snapshot",
            cacheAfter.any { it.id == "s1" })
        assertEquals("from firestore",
            cacheAfter.find { it.id == "s1" }?.data)
    }

    @Test
    fun `observe listener errors do not break Flow`() = runTest {
        val (repo, collection) = createTestRepo()
        repo.seedCache(TestEntity(id = "survive", data = "still here"))

        // Simulate a Firestore listener error
        collection.simulateError(RuntimeException("Permission denied"))

        val emitted = repo.observe().first()

        assertEquals("Flow should still emit cached data despite Firestore error", 1, emitted.size)
        assertEquals("still here", emitted[0].data)
    }
}
