package com.violinmaster.app.ui.component

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.AnalyticsHelper
import com.violinmaster.app.data.IAnalyticsService
import com.violinmaster.app.data.ICrashReportingService
import com.violinmaster.app.data.IPerformanceService
import com.violinmaster.app.data.TraceHandle
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import com.violinmaster.app.domain.usecase.CompleteAssignmentUseCase
import com.violinmaster.app.domain.usecase.DeleteAssignmentUseCase
import com.violinmaster.app.domain.usecase.GetAssignmentsUseCase
import com.violinmaster.app.domain.usecase.PublishAssignmentUseCase
import com.violinmaster.app.service.FaceBlurProcessor
import com.violinmaster.app.service.VideoCompressionService
import com.violinmaster.app.service.VideoRecordingService
import com.violinmaster.app.service.VideoUploadService
import com.violinmaster.app.ui.theme.AppLanguage
import com.violinmaster.app.ui.viewmodel.AssignmentViewModel
import com.violinmaster.app.ui.viewmodel.VideoUploadViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * TDD tests for VU-003(b): Record button replaces checkbox in AssignmentCreationForm.
 *
 * RED phase: AssignmentCreationForm does not yet accept videoViewModel param.
 * These tests will fail to compile until the production code is updated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AssignmentCreationFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var authManager: AuthManager
    private lateinit var fakeRepo: FakePracticeRepository
    private lateinit var assignmentVM: AssignmentViewModel
    private lateinit var fakeRecordingService: FakeVideoRecordingService
    private lateinit var videoViewModel: VideoUploadViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Initialize Firebase for fake services that extend Firebase-dependent classes
        FirebaseApp.initializeApp(context)
        authManager = AuthManager(context)
        fakeRepo = FakePracticeRepository()
        fakeRecordingService = FakeVideoRecordingService(context)

        val analyticsHelper = AnalyticsHelper(
            object : IAnalyticsService {
                override fun logEvent(name: String, params: Map<String, Any>) {}
                override fun setUserProperty(key: String, value: String) {}
                override fun setUserId(id: String) {}
                override fun setCurrentScreen(screenName: String, screenClass: String) {}
            },
            object : ICrashReportingService {
                override fun log(message: String) {}
                override fun recordException(throwable: Throwable) {}
                override fun setCustomKey(key: String, value: String) {}
            }
        )

        assignmentVM = AssignmentViewModel(
            repository = fakeRepo,
            authManager = authManager,
            analyticsHelper = analyticsHelper,
            getAssignmentsUseCase = GetAssignmentsUseCase(fakeRepo),
            completeAssignmentUseCase = CompleteAssignmentUseCase(fakeRepo, authManager),
            publishAssignmentUseCase = PublishAssignmentUseCase(fakeRepo, authManager),
            deleteAssignmentUseCase = DeleteAssignmentUseCase(fakeRepo)
        )

        videoViewModel = VideoUploadViewModel(
            recordingService = fakeRecordingService,
            faceBlurProcessor = FakeFaceBlurProcessor(),
            compressionService = FakeVideoCompressionService(),
            uploadService = FakeVideoUploadService(),
            authManager = authManager,
            analyticsHelper = analyticsHelper,
            performanceService = FakePerformanceService()
        )
    }

    @After
    fun tearDown() {
        fakeRecordingService.cleanup()
    }

    @Test
    fun `VU-003b - form renders without crash when videoViewModel is null`() {
        composeTestRule.setContent {
            AssignmentCreationForm(
                lang = AppLanguage.ENGLISH,
                linkedStudents = emptyList(),
                assignmentVM = assignmentVM,
                videoViewModel = null
            )
        }
        composeTestRule.waitForIdle()

        // The form with null videoViewModel shows the checkbox fallback section
        composeTestRule.onNodeWithTag("assignment_video_checkbox_section").fetchSemanticsNode()
    }

    @Test
    fun `VU-003b - record video button visible when videoViewModel is provided`() {
        composeTestRule.setContent {
            AssignmentCreationForm(
                lang = AppLanguage.ENGLISH,
                linkedStudents = emptyList(),
                assignmentVM = assignmentVM,
                videoViewModel = videoViewModel
            )
        }
        composeTestRule.waitForIdle()

        // The record button uses testTag, should be in the composition
        composeTestRule.onNodeWithTag("assignment_record_video_button").fetchSemanticsNode()
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    class FakePracticeRepository : IPracticeRepository {
        override val allSessions: Flow<List<PracticeSession>> = flowOf(emptyList())
        override val allLevelProgress: Flow<List<LessonProgress>> = flowOf(emptyList())
        override val allUsers: Flow<List<UserAccount>> = flowOf(emptyList())
        override val allAssignments: Flow<List<Assignment>> = flowOf(emptyList())

        override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = flowOf(emptyList())
        override suspend fun insertSession(session: PracticeSession) {}
        override suspend fun deleteSession(id: Int) {}
        override suspend fun clearSessions() {}
        override suspend fun insertLessonProgress(progress: LessonProgress) {}
        override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
        override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = null
        override suspend fun insertUser(user: UserAccount) {}
        override suspend fun getUserByUsername(username: String): UserAccount? = null
        override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
        override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
        override suspend fun insertAssignment(assignment: Assignment) {}
        override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {}
        override suspend fun deleteAssignmentById(id: Int) {}
    }

    class FakeFaceBlurProcessor : FaceBlurProcessor()

    class FakeVideoCompressionService : VideoCompressionService() {
        override suspend fun compressVideo(
            inputFile: File,
            outputFile: File,
            onProgress: (Float) -> Unit
        ): File {
            onProgress(0.0f)
            onProgress(0.5f)
            onProgress(1.0f)
            outputFile.parentFile?.mkdirs()
            inputFile.copyTo(outputFile, overwrite = true)
            return outputFile
        }
    }

    class FakeVideoUploadService : VideoUploadService(
        com.google.firebase.storage.FirebaseStorage.getInstance()
    ) {
        override suspend fun uploadVideo(
            videoFile: File,
            teacherUsername: String,
            assignmentId: String,
            onProgress: (Float) -> Unit
        ): String {
            onProgress(0.0f)
            onProgress(0.5f)
            onProgress(1.0f)
            return "https://firebasestorage.googleapis.com/v0/b/violin-app/o/videos%2F${teacherUsername}%2F${assignmentId}%2Ftest.mp4?alt=media"
        }
    }

    class FakeVideoRecordingService(context: Context) : VideoRecordingService(context) {
        var isRecordingFlag: Boolean = false
        private var currentFile: File? = null

        override fun startRecording(outputFile: File, onError: (String) -> Unit) {
            isRecordingFlag = true
            currentFile = outputFile
            outputFile.parentFile?.mkdirs()
            outputFile.createNewFile()
            outputFile.writeBytes(ByteArray(1024))
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

    private class FakePerformanceService : IPerformanceService {
        override fun startTrace(name: String): TraceHandle = FakeTraceHandle()
        override fun incrementMetric(traceName: String, metricName: String, incrementBy: Long) {}
    }

    private class FakeTraceHandle : TraceHandle {
        override fun stop() {}
        override fun putAttribute(key: String, value: String) {}
        override fun incrementMetric(metricName: String, incrementBy: Long) {}
    }
}
