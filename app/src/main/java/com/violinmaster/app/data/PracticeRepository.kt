package com.violinmaster.app.data

import kotlinx.coroutines.flow.Flow

class PracticeRepository(private val practiceDao: PracticeDao) : IPracticeRepository {

    override val allSessions: Flow<List<PracticeSession>> = practiceDao.getAllSessions()
    override val allLevelProgress: Flow<List<LessonProgress>> = practiceDao.getAllLessonProgress()

    override fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>> {
        return practiceDao.getSessionsByDate(dateString)
    }

    override suspend fun insertSession(session: PracticeSession) {
        practiceDao.insertSession(session)
    }

    override suspend fun deleteSession(id: Int) {
        practiceDao.deleteSessionById(id)
    }

    override suspend fun clearSessions() {
        practiceDao.clearAllSessions()
    }

    override suspend fun insertLessonProgress(progress: LessonProgress) {
        practiceDao.insertLessonProgress(progress)
    }

    override suspend fun updateLessonCompletion(lessonId: String, completed: Boolean) {
        practiceDao.updateLessonCompletion(lessonId, completed)
    }

    override suspend fun getLessonProgressById(lessonId: String): LessonProgress? {
        return practiceDao.getLessonProgressById(lessonId)
    }

    // --- User Management ---
    override val allUsers: Flow<List<UserAccount>> = practiceDao.getAllUsers()

    override suspend fun insertUser(user: UserAccount) {
        practiceDao.insertUser(user)
    }

    override suspend fun getUserByUsername(username: String): UserAccount? {
        return practiceDao.getUserByUsername(username)
    }

    // --- Assignments Management ---
    override val allAssignments: Flow<List<Assignment>> = practiceDao.getAllAssignments()

    override fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>> {
        return practiceDao.getAssignmentsForStudent(studentUsername)
    }

    override fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>> {
        return practiceDao.getAssignmentsByTeacher(teacherUsername)
    }

    override suspend fun insertAssignment(assignment: Assignment) {
        practiceDao.insertAssignment(assignment)
    }

    override suspend fun updateAssignmentCompletion(id: Int, completed: Boolean) {
        practiceDao.updateAssignmentCompletion(id, completed)
    }

    override suspend fun deleteAssignmentById(id: Int) {
        practiceDao.deleteAssignmentById(id)
    }
}
