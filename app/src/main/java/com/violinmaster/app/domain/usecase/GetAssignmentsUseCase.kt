package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.IPracticeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Retrieves assignments filtered by user role and identity.
 *
 * No Android imports — delegating to [IPracticeRepository].
 */
class GetAssignmentsUseCase constructor(
  private val repository: IPracticeRepository
) {
  /**
   * Returns a Flow of assignments scoped to the user.
   *
   * - TEACHER: returns all assignments for their teacher code.
   * - STUDENT: returns assignments targeted to them (including ALL).
   *
   * @param username The current user's username.
   * @param role The user's role (TEACHER, STUDENT).
   * @param teacherCode The teacher code (for both teacher and student scoping).
   */
  operator fun invoke(username: String, role: String, teacherCode: String): Flow<List<Assignment>> {
    return when (role) {
      "TEACHER" -> repository.getAssignmentsByTeacher(teacherCode)
      "STUDENT" -> repository.allAssignments.map { list ->
        list.filter {
          it.studentUsername.equals(username, ignoreCase = true) ||
            (it.studentUsername == "ALL" && it.teacherUsername == teacherCode)
        }
      }
      else -> MutableStateFlow(emptyList())
    }
  }
}
