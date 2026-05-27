package com.violinmaster.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.violinmaster.app.data.local.CachedMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDao {

    // --- Practice Sessions Queries ---
    @Query("SELECT * FROM practice_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<PracticeSession>>

    @Query("SELECT * FROM practice_sessions WHERE dateString = :dateString")
    fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSession)

    @Query("DELETE FROM practice_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Query("DELETE FROM practice_sessions")
    suspend fun clearAllSessions()

    // --- Lesson Progress Queries ---
    @Query("SELECT * FROM lesson_progress")
    fun getAllLessonProgress(): Flow<List<LessonProgress>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getLessonProgressById(lessonId: String): LessonProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonProgress(progress: LessonProgress)

    @Query("UPDATE lesson_progress SET completed = :completed WHERE lessonId = :lessonId")
    suspend fun updateLessonCompletion(lessonId: String, completed: Boolean)

    // --- User Accounts Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserAccount?

    @Query("SELECT * FROM user_accounts ORDER BY username ASC")
    fun getAllUsers(): Flow<List<UserAccount>>

    // --- Assignments Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment)

    @Query("SELECT * FROM student_assignments ORDER BY timestamp DESC")
    fun getAllAssignments(): Flow<List<Assignment>>

    @Query("SELECT * FROM student_assignments WHERE studentUsername = :studentUsername OR studentUsername = 'ALL' ORDER BY timestamp DESC")
    fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>>

    @Query("SELECT * FROM student_assignments WHERE teacherUsername = :teacherUsername ORDER BY timestamp DESC")
    fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>>

    @Query("UPDATE student_assignments SET completed = :completed WHERE id = :id")
    suspend fun updateAssignmentCompletion(id: Int, completed: Boolean)

    @Query("DELETE FROM student_assignments WHERE id = :id")
    suspend fun deleteAssignmentById(id: Int)

    // --- Cached Messages Queries (ChatRepository cache layer) ---

    /**
     * Returns cached messages for a given assignment, ordered by timestamp
     * ascending (oldest first). REQ-CHAT-004: Room cache for offline access.
     */
    @Query("SELECT * FROM cached_messages WHERE assignmentId = :assignmentId ORDER BY timestamp ASC")
    fun getCachedMessagesByAssignment(assignmentId: String): Flow<List<CachedMessage>>

    /**
     * Inserts or replaces cached messages. OnConflictStrategy.REPLACE ensures
     * Firestore snapshot updates overwrite stale cached entries.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedMessages(messages: List<CachedMessage>)

    /**
     * Removes all cached messages for a given assignment.
     * Called when assignment is deleted or chat is cleared.
     */
    @Query("DELETE FROM cached_messages WHERE assignmentId = :assignmentId")
    suspend fun clearCachedMessagesForAssignment(assignmentId: String)
}
