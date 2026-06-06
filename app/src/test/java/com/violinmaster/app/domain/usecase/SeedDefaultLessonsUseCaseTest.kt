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

class SeedDefaultLessonsUseCaseTest {

  private lateinit var repo: FakeSeedLessonsRepo
  private lateinit var useCase: SeedDefaultLessonsUseCase

  @Before
  fun setup() {
    repo = FakeSeedLessonsRepo()
    useCase = SeedDefaultLessonsUseCase(repo)
  }

  @Test
  fun `inserts all 9 default lessons`() = runTest {
    useCase()

    assertEquals(9, repo.insertedLessons.size)

    val ids = repo.insertedLessons.map { it.lessonId }.toSet()
    assertEquals(setOf("beg_1", "beg_2", "beg_3", "int_1", "int_2", "int_3", "adv_1", "adv_2", "adv_3"), ids)

    val difficulties = repo.insertedLessons.map { it.difficulty }.toSet()
    assertEquals(setOf("Beginner", "Intermediate", "Advanced"), difficulties)

    repo.insertedLessons.forEach {
              assertTrue(it.lessonTitle.isNotEmpty())
      assertEquals(0, it.totalPracticedSeconds)
    }
  }

  @Test
  fun `all lessons start uncompleted`() = runTest {
    useCase()

    repo.insertedLessons.forEach {
      assertEquals("lesson $it.lessonId should start uncompleted", false, it.completed)
    }
  }

  @Test
  fun `seeding twice inserts 18 lessons`() = runTest {
    useCase()
    useCase()

    assertEquals(18, repo.insertedLessons.size)
  }

  class FakeSeedLessonsRepo : IPracticeRepository {
    val insertedLessons = mutableListOf<LessonProgress>()
    override val allSessions: Flow<List<PracticeSession>> = flowOf(emptyList())
    override val allLevelProgress: Flow<List<LessonProgress>> = flowOf(emptyList())
    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> = flowOf(emptyList())
    override suspend fun insertSession(session: PracticeSession) {}
    override suspend fun deleteSession(id: Int) {}
    override suspend fun clearSessions() {}
    override suspend fun insertLessonProgress(progress: LessonProgress) { insertedLessons.add(progress) }
    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {}
    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? = null
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
