package com.violinmaster.app.data.firebase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [IFirestoreCollection] and its [FakeFirestoreCollection] implementation.
 *
 * Verifies the Firestore collection abstraction works correctly for:
 * - set/delete document operations
 * - snapshot listener (observe) that fires on changes
 * - fake serves as test double for unit tests of FirestoreSyncRepository
 *
 * REQ-CSYNC-001: Firestore collection abstraction for test substitution.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeFirestoreCollectionTest {

    // ── Simple test doc type ─────────────────────────────────────────────
    data class TestDoc(val name: String, val value: Int)

    // ── Set Document Tests ───────────────────────────────────────────────

    @Test
    fun `setDocument stores document in fake collection`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        val doc = TestDoc(name = "doc1", value = 42)

        fake.setDocument("doc1", doc)

        val stored = fake.getAllDocuments()
        assertEquals("Should contain exactly one document", 1, stored.size)
        assertEquals("doc1", stored.first().first)
        assertEquals(42, stored.first().second.value)
    }

    @Test
    fun `setDocument overwrites existing document with same id`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        fake.setDocument("doc1", TestDoc(name = "original", value = 1))
        fake.setDocument("doc1", TestDoc(name = "updated", value = 2))

        val stored = fake.getAllDocuments()
        assertEquals("Should still have one document after overwrite", 1, stored.size)
        assertEquals(2, stored.first().second.value)
        assertEquals("updated", stored.first().second.name)
    }

    @Test
    fun `setDocument with multiple ids stores them independently`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        fake.setDocument("a", TestDoc(name = "first", value = 1))
        fake.setDocument("b", TestDoc(name = "second", value = 2))
        fake.setDocument("c", TestDoc(name = "third", value = 3))

        val stored = fake.getAllDocuments()
        assertEquals("Should have 3 distinct documents", 3, stored.size)
    }

    // ── Delete Document Tests ────────────────────────────────────────────

    @Test
    fun `deleteDocument removes document from fake collection`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        fake.setDocument("doc1", TestDoc(name = "test", value = 99))
        assertEquals(1, fake.getAllDocuments().size)

        fake.deleteDocument("doc1")

        assertTrue("Collection should be empty after delete", fake.getAllDocuments().isEmpty())
    }

    @Test
    fun `deleteDocument on non-existent id is no-op`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        fake.setDocument("a", TestDoc(name = "keep", value = 1))

        fake.deleteDocument("nonexistent")

        assertEquals("Existing document should still be present", 1, fake.getAllDocuments().size)
    }

    @Test
    fun `deleteDocument only removes the specified document`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        fake.setDocument("a", TestDoc(name = "alpha", value = 1))
        fake.setDocument("b", TestDoc(name = "beta", value = 2))
        fake.setDocument("c", TestDoc(name = "gamma", value = 3))

        fake.deleteDocument("b")

        val stored = fake.getAllDocuments()
        assertEquals("Should have 2 documents remaining", 2, stored.size)
        val ids = stored.map { it.first }
        assertTrue("doc a should still exist", ids.contains("a"))
        assertTrue("doc c should still exist", ids.contains("c"))
    }

    // ── Snapshot Listener Tests ──────────────────────────────────────────

    @Test
    fun `snapshot listener fires on setDocument with updated snapshot`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        val capturedSnapshots = mutableListOf<List<TestDoc>>()

        val cancel = fake.addSnapshotListener(
            onSnapshot = { docs -> capturedSnapshots.add(docs.toList()) },
            onError = { /* no errors expected */ }
        )

        // Initial snapshot is empty collection
        // Set a document should trigger the listener
        fake.setDocument("doc1", TestDoc(name = "hello", value = 10))

        assertTrue("Snapshot listener should have fired at least once", capturedSnapshots.isNotEmpty())
        val lastSnapshot = capturedSnapshots.last()
        assertEquals("Last snapshot should contain the set document", 1, lastSnapshot.size)
        assertEquals("hello", lastSnapshot[0].name)
        assertEquals(10, lastSnapshot[0].value)

        cancel()
    }

    @Test
    fun `snapshot listener fires on deleteDocument with updated snapshot`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        fake.setDocument("keep", TestDoc(name = "keeper", value = 1))
        fake.setDocument("gone", TestDoc(name = "removable", value = 2))

        val capturedSnapshots = mutableListOf<List<TestDoc>>()
        val cancel = fake.addSnapshotListener(
            onSnapshot = { docs -> capturedSnapshots.add(docs.toList()) },
            onError = { /* no errors expected */ }
        )

        fake.deleteDocument("gone")

        val lastSnapshot = capturedSnapshots.last()
        assertEquals("Should have 1 document remaining after delete", 1, lastSnapshot.size)
        assertEquals("keeper", lastSnapshot[0].name)

        cancel()
    }

    @Test
    fun `snapshot listener cancel stops receiving updates`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        var callCount = 0
        val cancel = fake.addSnapshotListener(
            onSnapshot = { callCount++ },
            onError = { /* no errors expected */ }
        )

        callCount = 0 // reset after initial snapshot

        fake.setDocument("d1", TestDoc(name = "before cancel", value = 1))
        val countBeforeCancel = callCount

        cancel()

        fake.setDocument("d2", TestDoc(name = "after cancel", value = 2))
        assertEquals(
            "After cancel, no more updates should be received",
            countBeforeCancel,
            callCount
        )
    }

    // ── getAllDocuments Tests ────────────────────────────────────────────

    @Test
    fun `getAllDocuments returns empty list for new collection`() {
        val fake = FakeFirestoreCollection<TestDoc>()
        assertTrue("New collection should be empty", fake.getAllDocuments().isEmpty())
    }

    @Test
    fun `getAllDocuments preserves insertion order`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        fake.setDocument("z", TestDoc(name = "z", value = 1))
        fake.setDocument("a", TestDoc(name = "a", value = 2))
        fake.setDocument("m", TestDoc(name = "m", value = 3))

        val ids = fake.getAllDocuments().map { it.first }
        assertEquals("Documents should preserve insertion order", listOf("z", "a", "m"), ids)
    }

    // ── Error Listener Tests ─────────────────────────────────────────────

    @Test
    fun `simulateError triggers onError callback`() = runTest {
        val fake = FakeFirestoreCollection<TestDoc>()
        var capturedError: Exception? = null

        val cancel = fake.addSnapshotListener(
            onSnapshot = { },
            onError = { capturedError = it }
        )

        val testError = RuntimeException("Simulated Firestore error")
        fake.simulateError(testError)

        assertNotNull("onError should have been called", capturedError)
        assertEquals("Simulated Firestore error", capturedError!!.message)

        cancel()
    }
}
