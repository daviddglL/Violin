package com.violinmaster.app.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for practice data access.
 *
 * Defines the contract for all practice-related CRUD operations:
 * sessions, lessons, users, assignments, daily tasks, and leaderboard.
 *
 * REQ-ARCH-002: Repository interfaces enable dependency inversion,
 * allowing ViewModels to depend on abstractions instead of concrete classes.
 */
interface IPracticeRepository {
    val allSessions: Flow<List<PracticeSession>>
    val allLevelProgress: Flow<List<LessonProgress>>

    fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>>

    suspend fun insertSession(session: PracticeSession)
    suspend fun deleteSession(id: Int)
    suspend fun clearSessions()

    suspend fun insertLessonProgress(progress: LessonProgress)
    suspend fun updateLessonCompletion(lessonId: String, completed: Boolean)
    suspend fun getLessonProgressById(lessonId: String): LessonProgress?

    val allUsers: Flow<List<UserAccount>>

    suspend fun insertUser(user: UserAccount)
    suspend fun getUserByUsername(username: String): UserAccount?

    val allAssignments: Flow<List<Assignment>>

    fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>>
    fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>>

    suspend fun insertAssignment(assignment: Assignment)
    suspend fun updateAssignmentCompletion(id: Int, completed: Boolean)
    suspend fun deleteAssignmentById(id: Int)
}
