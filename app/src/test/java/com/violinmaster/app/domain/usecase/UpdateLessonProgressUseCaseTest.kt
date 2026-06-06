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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateLessonProgressUseCaseTest {

  private lateinit var repo: FakeUpdateProgressRepo
  private lateinit var useCase: UpdateLessonProgressUseCase

  @Before
  fun setup() {
    repo = FakeUpdateProgressRepo()
    useCase = UpdateLessonProgressUseCase(repo)
  }

  @Test
  fun `adds seconds to existing lesson`() = runTest {
    repo.lessons["beg_1"] = LessonProgress("beg_1", "Posture", "Beginner", false, 100)

    val newlyCompleted = useCase("beg_1", 50)

    assertFalse(newlyCompleted)
    assertEquals(1, repo.insertedLessons.size)
    assertEquals(150, repo.insertedLessons[0].totalPracticedSeconds)
    assertFalse(repo.insertedLessons[0].completed)
  }

  @Test
  fun `reaches threshold marks lesson completed`() = runTest {
    repo.lessons["beg_1"] = LessonProgress("beg_1", "Posture", "Beginner", false, 250)

    val newlyCompleted = useCase("beg_1", 60)

    assertTrue(newlyCompleted)
    assertEquals(310, repo.insertedLessons[0].totalPracticedSeconds)
    assertTrue(repo.insertedLessons[0].completed)
  }

  @Test
  fun `exactly at threshold completes lesson`() = runTest {
    repo.lessons["beg_1"] = LessonProgress("beg_1", "Posture", "Beginner", false, 250)

    val newlyCompleted = useCase("beg_1", 50)

    assertTrue(newlyCompleted)
    assertEquals(300, repo.insertedLessons[0].totalPracticedSeconds)
    assertTrue(repo.insertedLessons[0].completed)
  }

  @Test
  fun `already completed lesson returns false`() = runTest {
    repo.lessons["beg_1"] = LessonProgress("beg_1", "Posture", "Beginner", true, 350)

    val newlyCompleted = useCase("beg_1", 60)

    assertFalse(newlyCompleted)
    assertEquals(410, repo.insertedLessons[0].totalPracticedSeconds)
    assertTrue(repo.insertedLessons[0].completed)
  }

  @Test
  fun `nonexistent lesson returns false`() = runTest {
    val newlyCompleted = useCase("nonexistent", 100)

    assertFalse(newlyCompleted)
    assertTrue(repo.insertedLessons.isEmpty())
  }

  @Test
  fun `below threshold returns false`() = runTest {
    repo.lessons["beg_1"] = LessonProgress("beg_1", "Posture", "Beginner", false, 0)

    val newlyCompleted = useCase("beg_1", 299)

    assertFalse(newlyCompleted)
    assertEquals(299, repo.insertedLessons[0].totalPracticedSeconds)
    assertFalse(repo.insertedLessons[0].completed)
  }

  class FakeUpdateProgressRepo : IPracticeRepository {
    val lessons = mutableMapOf<String, LessonProgress>()
    val insertedLessons = mutableListOf<LessonProgress>()
    override val allSessions: Flow<List<PracticeSession>> = flowOf(emptyList())
    override val allLevelProgress: Flow<List<LessonProgress>> = flowOf(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = flowOf(emptyList())
    override suspend fun insertSession(session: PracticeSession) {}
    override suspend fun deleteSession(id: Int) {}
    override suspend fun clearSessions() {}
    override suspend fun insertLessonProgress(progress: LessonProgress) { insertedLessons.add(progress) }
    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = lessons[lessonId]
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
