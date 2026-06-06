package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GenerateDemoHistoryUseCaseTest {

  private lateinit var repo: FakeGenerateDemoRepo
  private lateinit var useCase: GenerateDemoHistoryUseCase

  @Before
  fun setup() {
    repo = FakeGenerateDemoRepo()
    useCase = GenerateDemoHistoryUseCase(repo)
  }

  @Test
  fun `clears existing sessions and inserts demo data`() = runTest {
    useCase()

    assertTrue(repo.cleared)
    assertTrue(repo.insertedSessions.isNotEmpty())
  }

  @Test
  fun `marks existing lessons as completed when found`() = runTest {
    useCase()

    assertTrue(repo.cleared)
    assertEquals(2, repo.insertedLessons.size)
    assertEquals("beg_1", repo.insertedLessons[0].lessonId)
    assertEquals(900, repo.insertedLessons[0].totalPracticedSeconds)
    assertTrue(repo.insertedLessons[0].completed)
    assertEquals("adv_1", repo.insertedLessons[1].lessonId)
    assertEquals(480, repo.insertedLessons[1].totalPracticedSeconds)
    assertTrue(repo.insertedLessons[1].completed)
  }

  @Test
  fun `skips lesson updates when progress not found`() = runTest {
    repo.returnNullLesson = true

    useCase()

    assertTrue(repo.cleared)
    assertTrue(repo.insertedSessions.isNotEmpty())
    assertTrue(repo.insertedLessons.isEmpty())
  }

  class FakeGenerateDemoRepo : IPracticeRepository {
    val insertedSessions = mutableListOf<PracticeSession>()
    val insertedLessons = mutableListOf<LessonProgress>()
    var cleared = false
    var returnNullLesson = false

    override val allSessions: Flow<List<PracticeSession>> = flowOf(emptyList())
    override val allLevelProgress: Flow<List<LessonProgress>> = flowOf(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = flowOf(emptyList())
    override suspend fun insertSession(session: PracticeSession) { insertedSessions.add(session) }
    override suspend fun deleteSession(id: Int) {}
    override suspend fun clearSessions() { cleared = true }
    override suspend fun insertLessonProgress(progress: LessonProgress) { insertedLessons.add(progress) }
    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? {
      if (returnNullLesson) return null
      return when (lessonId) {
        "beg_1" -> LessonProgress("beg_1", "Posture & Open Strings Bowing", "Beginner", false, 0)
        "adv_1" -> LessonProgress("adv_1", "Bowing Styles: Martelé, Spiccato", "Advanced", false, 0)
        else -> null
      }
    }
    override val allUsers: Flow<List<UserAccount>> = flowOf(emptyList())
    override suspend fun insertUser(user: UserAccount) {}
    override suspend fun getUserByUsername(username: String): UserAccount? = null
    override val allAssignments: Flow<List<Assignment>> = flowOf(emptyList())
    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> = flowOf(emptyList())
    override suspend fun insertAssignment(assignment: Assignment) {}
    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {}
    override suspend fun deleteAssignmentById(id: Int) {}
  }
}
