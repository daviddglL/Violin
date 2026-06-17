package com.violinmaster.app.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.data.IAnalyticsService
import com.violinmaster.app.data.ICrashReportingService
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.service.VideoCompressionService
import com.violinmaster.app.service.VideoRecordingService
import com.violinmaster.app.service.FaceBlurProcessor
import com.violinmaster.app.service.VideoUploadService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * TDD tests for VideoUploadViewModel state machine.
 *
 * REQ-VID-001 through REQ-VID-008: Video recording, compression, upload pipeline.
 *
 * Cannot test CameraX in Robolectric, so the ViewModel's state transitions
 * are tested using fake services. The ViewModel orchestrates the pipeline:
 * IDLE → RECORDING → COMPRESSING → UPLOADING → DONE (or ERROR at any step).
 *
 * RED phase: VideoUploadViewModel.kt and service classes do not exist yet.
 * These tests will fail to compile until the production code is written.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoUploadViewModelTest {

    private lateinit var fakeFaceBlurProcessor: FakeFaceBlurProcessor
    private lateinit var fakeRecordingService: FakeVideoRecordingService
    private lateinit var fakeCompressionService: FakeVideoCompressionService
    private lateinit var fakeUploadService: FakeVideoUploadService
    private lateinit var authManager: AuthManager
    private lateinit var analyticsHelper: AnalyticsHelper
    private lateinit var viewModel: VideoUploadViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Initialize Firebase for Robolectric (needed for FirebaseStorage singleton)
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
        fakeFaceBlurProcessor = FakeFaceBlurProcessor()
        fakeRecordingService = FakeVideoRecordingService(context)
        fakeCompressionService = FakeVideoCompressionService()
        fakeUploadService = FakeVideoUploadService()
        authManager = AuthManager(context)
        analyticsHelper = AnalyticsHelper(
            analyticsService = FakeAnalyticsService(),
            crashReportingService = FakeCrashReportingService()
        )
        viewModel = VideoUploadViewModel(
            recordingService = fakeRecordingService,
            faceBlurProcessor = fakeFaceBlurProcessor,
            compressionService = fakeCompressionService,
            uploadService = fakeUploadService,
            authManager = authManager,
            analyticsHelper = analyticsHelper
        )
    }

    @After
    fun tearDown() {
        // Clean up any temp files from fakes
        fakeRecordingService.cleanup()
    }

    // ─────────────────────────────────────────────────────────────────
    // T-014 Test 1: Initial state is IDLE
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `initial state is IDLE`() = runTest {
        val state = viewModel.uploadState.value
        assertTrue(
            "Initial state should be Idle, but was $state",
            state is VideoUploadViewModel.UploadState.Idle
        )
        assertFalse("Should not be recording initially", viewModel.isRecording())
        assertNull("Error should be null initially", viewModel.error.value)
    }

    // ─────────────────────────────────────────────────────────────────
    // T-014 Test 2: startRecording transitions to RECORDING
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `startRecording transitions to RECORDING`() = runTest {
        // Act
        viewModel.startRecording()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uploadState.value
        assertTrue(
            "State should be Recording after startRecording, but was $state",
            state is VideoUploadViewModel.UploadState.Recording
        )
        assertTrue("Should be recording", viewModel.isRecording())
        assertNull("Should have no error", viewModel.error.value)
    }

    // ─────────────────────────────────────────────────────────────────
    // T-014 Test 3: stopRecording → full pipeline (COMPRESSING → UPLOADING → DONE)
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `stopRecording pipeline transitions through COMPRESSING to UPLOADING to DONE`() = runTest {
        // Arrange: start recording first
        viewModel.startRecording()
        advanceUntilIdle()
        assertTrue(viewModel.isRecording())

        // Act: stop recording → triggers pipeline
        viewModel.stopRecording()
        advanceUntilIdle()

        // Assert: final state is DONE
        val state = viewModel.uploadState.value
        assertTrue(
            "Final state should be Done after pipeline completes, but was $state",
            state is VideoUploadViewModel.UploadState.Done
        )
        val doneState = state as VideoUploadViewModel.UploadState.Done
        assertNotNull("Video URL should not be null", doneState.videoUrl)
        assertTrue("Video URL should be non-empty", doneState.videoUrl.isNotEmpty())
        assertFalse("Should not be recording after stop", viewModel.isRecording())
        assertNull("Should have no error", viewModel.error.value)

        // Verify the pipeline recorded the correct transitions
        val transitions = viewModel.transitionHistory
        assertTrue(
            "Should have COMPRESSING transition",
            transitions.any { it is VideoUploadViewModel.UploadState.Compressing }
        )
        assertTrue(
            "Should have UPLOADING transition",
            transitions.any { it is VideoUploadViewModel.UploadState.Uploading }
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // T-014 Test 4: cancelRecording returns to IDLE and discards file
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `cancelRecording returns to IDLE and discards recording`() = runTest {
        // Arrange: start recording
        viewModel.startRecording()
        advanceUntilIdle()
        assertTrue(viewModel.isRecording())

        // Act: cancel
        viewModel.cancelRecording()
        advanceUntilIdle()

        // Assert: back to IDLE
        val state = viewModel.uploadState.value
        assertTrue(
            "State should be Idle after cancelRecording, but was $state",
            state is VideoUploadViewModel.UploadState.Idle
        )
        assertFalse("Should not be recording after cancel", viewModel.isRecording())
        assertNull("Should have no error after cancel", viewModel.error.value)
    }

    // ─────────────────────────────────────────────────────────────────
    // T-014 Test 5: error state on compression failure
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `error state set on compression failure`() = runTest {
        // Arrange: start recording, then make compression fail
        viewModel.startRecording()
        advanceUntilIdle()
        fakeCompressionService.shouldFail = true

        // Act: stop → pipeline hits compression error
        viewModel.stopRecording()
        advanceUntilIdle()

        // Assert: error state
        val state = viewModel.uploadState.value
        assertTrue(
            "State should be Error after compression failure, but was $state",
            state is VideoUploadViewModel.UploadState.Error
        )
        val errorState = state as VideoUploadViewModel.UploadState.Error
        assertTrue(
            "Error message should mention compression",
            errorState.message.contains("compress", ignoreCase = true) ||
                errorState.message.contains("compression", ignoreCase = true)
        )
        assertNull("ViewModel error flow should be null (error is in state)", viewModel.error.value)
    }

    // ─────────────────────────────────────────────────────────────────
    // T-014 Test 6: error state on upload failure
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `error state set on upload failure`() = runTest {
        // Arrange: start recording, compression succeeds, but upload fails
        viewModel.startRecording()
        advanceUntilIdle()
        fakeUploadService.shouldFail = true

        // Act: stop → pipeline hits upload error
        viewModel.stopRecording()
        advanceUntilIdle()

        // Assert: error state
        val state = viewModel.uploadState.value
        assertTrue(
            "State should be Error after upload failure, but was $state",
            state is VideoUploadViewModel.UploadState.Error
        )
        val errorState = state as VideoUploadViewModel.UploadState.Error
        assertTrue(
            "Error message should mention upload",
            errorState.message.contains("upload", ignoreCase = true)
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // T-014 Test 7: progress updates during compression and upload
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `progress updates emitted during compression and upload`() = runTest {
        // Arrange: start recording
        viewModel.startRecording()
        advanceUntilIdle()

        // Act: stop → pipeline runs
        viewModel.stopRecording()
        advanceUntilIdle()

        // Assert: both compression and upload progress were emitted
        // Fake services simulate 100% completion; check that progress
        // states were observed in the transition history
        val transitions = viewModel.transitionHistory
        val compressingStates = transitions.filterIsInstance<VideoUploadViewModel.UploadState.Compressing>()
        val uploadingStates = transitions.filterIsInstance<VideoUploadViewModel.UploadState.Uploading>()

        assertTrue(
            "Should have at least one Compressing state with progress > 0",
            compressingStates.any { it.progress > 0f }
        )
        assertTrue(
            "Should have at least one Uploading state with progress > 0",
            uploadingStates.any { it.progress > 0f }
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // T-014 Test 8: resetState returns to IDLE after Done
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `resetState returns to IDLE after Done`() = runTest {
        // Arrange: complete the full pipeline
        viewModel.startRecording()
        advanceUntilIdle()
        viewModel.stopRecording()
        advanceUntilIdle()

        val doneState = viewModel.uploadState.value
        assertTrue("Should be Done state", doneState is VideoUploadViewModel.UploadState.Done)

        // Act: reset
        viewModel.resetState()
        advanceUntilIdle()

        // Assert: back to IDLE
        val state = viewModel.uploadState.value
        assertTrue(
            "State should be Idle after resetState, but was $state",
            state is VideoUploadViewModel.UploadState.Idle
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // T-014 Test 9: resetState returns to IDLE after Error
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `resetState returns to IDLE after Error`() = runTest {
        // Arrange: trigger an error
        viewModel.startRecording()
        advanceUntilIdle()
        fakeCompressionService.shouldFail = true
        viewModel.stopRecording()
        advanceUntilIdle()

        val errorState = viewModel.uploadState.value
        assertTrue("Should be Error state", errorState is VideoUploadViewModel.UploadState.Error)

        // Act: reset
        viewModel.resetState()
        advanceUntilIdle()

        // Assert: back to IDLE
        val state = viewModel.uploadState.value
        assertTrue(
            "State should be Idle after resetState from Error, but was $state",
            state is VideoUploadViewModel.UploadState.Idle
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Fake Implementations — test doubles for services
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Fake FaceBlurProcessor — no-op for tests.
     */
    class FakeFaceBlurProcessor : FaceBlurProcessor()

    /**
     * Fake VideoRecordingService backed by a temp file.
     * Does NOT use CameraX — just creates an empty file to simulate recording.
     */
    class FakeVideoRecordingService(context: android.content.Context) : VideoRecordingService(context) {
        var isRecordingFlag: Boolean = false
        private var currentFile: File? = null

        override fun hasCameraPermission(): Boolean = true

        override fun startRecording(outputFile: File, onError: (String) -> Unit) {
            isRecordingFlag = true
            currentFile = outputFile
            outputFile.parentFile?.mkdirs()
            outputFile.createNewFile()
            outputFile.writeBytes(ByteArray(1024)) // 1KB dummy content
        }

        override fun stopRecording(): File {
            isRecordingFlag = false
            return currentFile ?: throw IllegalStateException("No recording in progress")
        }

        override fun isRecording(): Boolean = isRecordingFlag

        fun cleanup() {
            currentFile?.delete()
            currentFile = null
        }
    }

    /**
     * Fake VideoCompressionService — simulates compression with progress.
     */
    class FakeVideoCompressionService : VideoCompressionService() {
        var shouldFail: Boolean = false

        override suspend fun compressVideo(
            inputFile: File,
            outputFile: File,
            onProgress: (Float) -> Unit
        ): File {
            if (shouldFail) {
                throw RuntimeException("Fake compression failure: codec error")
            }
            // Simulate progress from 0 to 1.0
            onProgress(0.0f)
            onProgress(0.5f)
            onProgress(1.0f)
            // Simulate compression by copying the file
            outputFile.parentFile?.mkdirs()
            inputFile.copyTo(outputFile, overwrite = true)
            return outputFile
        }
    }

    /**
     * Fake VideoUploadService — simulates Firebase Storage upload with progress.
     */
    class FakeVideoUploadService : VideoUploadService(
        com.google.firebase.storage.FirebaseStorage.getInstance()
    ) {
        var shouldFail: Boolean = false

        override suspend fun uploadVideo(
            videoFile: File,
            teacherUsername: String,
            assignmentId: String,
            onProgress: (Float) -> Unit
        ): String {
            if (shouldFail) {
                throw RuntimeException("Fake upload failure: network error")
            }
            // Simulate progress from 0 to 1.0
            onProgress(0.0f)
            onProgress(0.5f)
            onProgress(1.0f)
            return "https://firebasestorage.googleapis.com/v0/b/violin-app/o/videos%2F${teacherUsername}%2F${assignmentId}%2Ftest.mp4?alt=media"
        }
    }

    // ── Test doubles for AnalyticsHelper dependencies ──────────────────

    private class FakeAnalyticsService : IAnalyticsService {
        override fun logEvent(name: String, params: Map<String, Any>) {}
        override fun setUserProperty(key: String, value: String) {}
        override fun setUserId(id: String) {}
        override fun setCurrentScreen(screenName: String, screenClass: String) {}
    }

    private class FakeCrashReportingService : ICrashReportingService {
        override fun log(message: String) {}
        override fun recordException(throwable: Throwable) {}
        override fun setCustomKey(key: String, value: String) {}
    }
}
