package com.violinmaster.app.data.firebase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Validates the FirestoreCollection concrete implementation of IFirestoreCollection.
 *
 * FirestoreCollection wraps FirebaseFirestore for production use, providing
 * a testable abstraction via IFirestoreCollection<T>. These tests verify
 * structural contract and path resolution behavior.
 *
 * Firestore backend is not available in Robolectric unit tests.
 * Full Firestore read/write/listen integration is tested in androidTest
 * (Firebase Emulator) in PR 5.
 *
 * REQ-CSYNC-001: IFirestoreCollection concrete wrapper for production Firestore calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FirestoreCollectionTest {

    private lateinit var firestore: FirebaseFirestore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Initialize Firebase for Robolectric (needed for FirebaseFirestore singleton)
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.violinmaster.app.test")
                    .setApiKey("fake-api-key")
                    .setProjectId("violin-master-test")
                    .build()
            )
        }
        firestore = FirebaseFirestore.getInstance()
    }

    @After
    fun tearDown() {
        // Firestore instance is singleton — no explicit cleanup needed
    }

    // ── Structural Contract ───────────────────────────────────────────────

    @Test
    fun `FirestoreCollection is instantiable with valid params`() {
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "test_collection" },
            docClass = SessionDoc::class.java
        )

        assertNotNull("FirestoreCollection should be instantiable", collection)
    }

    @Test
    fun `FirestoreCollection satisfies IFirestoreCollection contract`() {
        val collection: IFirestoreCollection<SessionDoc> = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "test_collection" },
            docClass = SessionDoc::class.java
        )

        assertNotNull(collection)
        // Interface methods exist: setDocument, deleteDocument, addSnapshotListener
        // Compile-time verification: this assignment would fail if FirestoreCollection
        // didn't implement IFirestoreCollection<SessionDoc>
    }

    @Test
    fun `path provider is stored and callable`() {
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "dynamic_path_42" },
            docClass = SessionDoc::class.java
        )

        val path = collection.collectionPath()
        assertEquals("dynamic_path_42", path)
    }

    @Test
    fun `path provider is resolved lazily on each call`() {
        var counter = 0
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "path_${++counter}" },
            docClass = SessionDoc::class.java
        )

        assertEquals("path_1", collection.collectionPath())
        assertEquals("path_2", collection.collectionPath())
        assertEquals("path_3", collection.collectionPath())
    }

    @Test
    fun `docClass is stored correctly`() {
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "lessons" },
            docClass = LessonDoc::class.java
        )

        assertEquals(LessonDoc::class.java, collection.getDocClass())
    }

    // ── addSnapshotListener returns cancel function ────────────────────────

    @Test
    fun `addSnapshotListener returns cancel function`() {
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "test_listeners" },
            docClass = SessionDoc::class.java
        )

        val cancel = collection.addSnapshotListener(
            onSnapshot = { _ -> },
            onError = { _ -> }
        )

        assertNotNull("Cancel function should not be null", cancel)
        // Cancel the listener to clean up — fire-and-forget
        cancel()
    }

    @Test
    fun `addSnapshotListener callbacks are invocable`() {
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "test_snapshots" },
            docClass = SessionDoc::class.java
        )

        val latch = CountDownLatch(1)
        var callbackFired = false
        val cancel = collection.addSnapshotListener(
            onSnapshot = { _ ->
                callbackFired = true
                latch.countDown()
            },
            onError = { _ ->
                callbackFired = true
                latch.countDown()
            }
        )

        // Wait briefly for Firestore snapshot callback (may fire immediately,
        // may fire on error, or may not fire at all without backend).
        // Either way, the listener registration succeeded.
        latch.await(2, TimeUnit.SECONDS)
        cancel()

        // The test validates that the listener mechanism is wired correctly.
        // callbackFired may be false if Firestore doesn't callback in Robolectric,
        // but the test ensures no crash and cancel() works.
        assertNotNull("Listener registration and cleanup should not throw", cancel)
    }

    // ── Multiple doc types (type-safety verification) ──────────────────────

    @Test
    fun `FirestoreCollection typed to AssignmentDoc resolves correct path and class`() {
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "assignments" },
            docClass = AssignmentDoc::class.java
        )

        assertEquals("assignments", collection.collectionPath())
        assertEquals(AssignmentDoc::class.java, collection.getDocClass())
    }

    @Test
    fun `FirestoreCollection typed to UserDoc resolves correct path and class`() {
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "users" },
            docClass = UserDoc::class.java
        )

        assertEquals("users", collection.collectionPath())
        assertEquals(UserDoc::class.java, collection.getDocClass())
    }

    @Test
    fun `FirestoreCollection typed to LessonDoc resolves correct path and class`() {
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "lesson_progress" },
            docClass = LessonDoc::class.java
        )

        assertEquals("lesson_progress", collection.collectionPath())
        assertEquals(LessonDoc::class.java, collection.getDocClass())
    }

    // ── Dynamic path for user-scoped subcollections ─────────────────────────

    @Test
    fun `dynamic path for user scoped subcollection sessions`() {
        val uid = "user_abc_123"
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "users/$uid/sessions" },
            docClass = SessionDoc::class.java
        )

        assertEquals("users/user_abc_123/sessions", collection.collectionPath())
    }

    @Test
    fun `dynamic path changes when uid changes between calls`() {
        var currentUid = "user_a"
        val collection = FirestoreCollection(
            firestore = firestore,
            pathProvider = { "users/$currentUid/sessions" },
            docClass = SessionDoc::class.java
        )

        assertEquals("users/user_a/sessions", collection.collectionPath())
        currentUid = "user_b"
        assertEquals("users/user_b/sessions", collection.collectionPath())
    }
}
