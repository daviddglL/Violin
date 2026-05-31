package com.violinmaster.app.data

import kotlinx.coroutines.flow.Flow
import com.violinmaster.app.data.PracticeSession
class PracticeRepository(private val practiceDao: PracticeDao) {

    val allSessions: Flow<List<PracticeSession>> = practiceDao.getAllSessions()
    val allLevelProgress: Flow<List<LessonProgress>> = practiceDao.getAllLessonProgress()

    fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> {
        return practiceDao.getSessionsByDate(dateString)
    }

    suspend fun insertSession(session: PracticeSession) {
        practiceDao.insertSession(session)
    }

    suspend fun deleteSession(id: Int) {
        practiceDao.deleteSessionById(id)
    }

    suspend fun clearSessions() {
        practiceDao.clearAllSessions()
    }

    suspend fun insertLessonProgress(progress: LessonProgress) {
        practiceDao.insertLessonProgress(progress)
    }

    suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {
        practiceDao.updateLessonCompletion(lessonId, completed)
    }

    // Direct fetch helper
    suspend fun getLessonProgressById(lessonId: String): LessonProgress? {
        return practiceDao.getLessonProgressById(lessonId)
    }

    // --- User Management ---
    val allUsers: Flow<List<UserAccount>> = practiceDao.getAllUsers()

    suspend fun insertUser(user: UserAccount) {
        practiceDao.insertUser(user)
    }

    suspend fun getUserByUsername(username: String): UserAccount? {
        return practiceDao.getUserByUsername(username)
    }

    // --- Assignments Management ---
    val allAssignments: Flow<List<Assignment>> = practiceDao.getAllAssignments()

    fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> {
        return practiceDao.getAssignmentsForStudent(studentUsername)
    }

    fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> {
        return practiceDao.getAssignmentsByTeacher(teacherUsername)
    }

    suspend fun insertAssignment(assignment: Assignment) {
        practiceDao.insertAssignment(assignment)
    }

    suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {
        practiceDao.updateAssignmentCompletion(id, completed)
    }

    suspend fun deleteAssignmentById(id: Int) {
        practiceDao.deleteAssignmentById(id)
    }
}
