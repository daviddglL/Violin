package com.violinmaster.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.di.AuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PublishAssignmentUseCaseTest {

  private lateinit var context: Context
  private lateinit var repo: FakePublishAssignmentRepo
  private lateinit var authManager: AuthManager
  private lateinit var useCase: PublishAssignmentUseCase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    repo = FakePublishAssignmentRepo()
    authManager = AuthManager(context)
    useCase = PublishAssignmentUseCase(repo, authManager)
  }

  @After
  fun tearDown() {
    authManager.clearSession()
  }

  @Test
  fun `creates assignment with real video url from upload pipeline`() = runTest {
    val user = UserAccount(
      username = "teacher1", role = "TEACHER",
      hashedPassword = "hash", salt = "salt", teacherCode = "TEACH-123"
    )
    authManager.restoreCurrentUser(user)

    val realVideoUrl = "https://firebasestorage.googleapis.com/v0/b/violin-app/o/videos%2FTEACH-123%2Fdefault%2Ftest.mp4?alt=media"
    val result = useCase(
      title = "Scale Practice",
      description = "Practice D major",
      targetStudent = "ALL",
      videoTitle = "Tutorial Video",
      durationSeconds = 120,
      videoUrl = realVideoUrl
    )

    assertNotNull(result)
    assertEquals("Scale Practice", result.title)
    assertEquals("Practice D major", result.description)
    assertEquals("TEACH-123", result.teacherUsername)
    assertEquals("ALL", result.studentUsername)
    assertEquals("Tutorial Video", result.videoTitle)
    assertEquals(120, result.videoDurationSeconds)
    assertEquals(realVideoUrl, result.videoResourceUrl)
    assertEquals(1, repo.insertedAssignments.size)
  }

  @Test
  fun `creates assignment without video when video url and title are empty`() = runTest {
    val user = UserAccount(
      username = "teacher1", role = "TEACHER",
      hashedPassword = "hash", salt = "salt", teacherCode = "TEACH-123"
    )
    authManager.restoreCurrentUser(user)

    val result = useCase("No Video", "Just a note", "bob", "", 0, videoUrl = "")

    assertEquals("", result.videoTitle)
    assertEquals(0, result.videoDurationSeconds)
    assertEquals("", result.videoResourceUrl)
  }

  @Test
  fun `uses real video url when provided regardless of videoTitle`() = runTest {
    val user = UserAccount(
      username = "teacher1", role = "TEACHER",
      hashedPassword = "hash", salt = "salt", teacherCode = "TEACH-123"
    )
    authManager.restoreCurrentUser(user)

    val realVideoUrl = "https://storage.example.com/video/123.mp4"
    // Empty videoTitle should still store the real URL
    val result = useCase(
      title = "Quick Task",
      description = "Watch this",
      targetStudent = "ALL",
      videoTitle = "",
      durationSeconds = 60,
      videoUrl = realVideoUrl
    )

    assertEquals("", result.videoTitle)
    assertEquals(60, result.videoDurationSeconds)
    assertEquals(realVideoUrl, result.videoResourceUrl)
  }

  @Test(expected = IllegalStateException::class)
  fun `no authenticated user throws exception`() = runTest {
    useCase("Test", "Desc", "ALL", "", 0)
  }

  class FakePublishAssignmentRepo : IPracticeRepository {
    val insertedAssignments = mutableListOf<Assignment>()
    override val allSessions: Flow<List<PracticeSession>> = flowOf(emptyList())
    override val allLevelProgress: Flow<List<LessonProgress>> = flowOf(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = flowOf(emptyList())
    override suspend fun insertSession(session: PracticeSession) {}
    override suspend fun deleteSession(id: Int) {}
    override suspend fun clearSessions() {}
    override suspend fun insertLessonProgress(progress: LessonProgress) {}
    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = null
    override val allUsers: Flow<List<UserAccount>> = flowOf(emptyList())
    override suspend fun insertUser(user: UserAccount) {}
    override suspend fun getUserByUsername(username: String): UserAccount? = null
    override val allAssignments: Flow<List<Assignment>> = flowOf(emptyList())
    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
    override suspend fun insertAssignment(assignment: Assignment) { insertedAssignments.add(assignment) }
    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {}
    override suspend fun deleteAssignmentById(id: Int) {}
  }
}
