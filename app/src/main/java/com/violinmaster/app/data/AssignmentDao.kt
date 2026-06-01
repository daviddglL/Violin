package com.violinmaster.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment)

    @Update
    suspend fun updateAssignment(assignment: Assignment)

    @Query("SELECT * FROM student_assignments WHERE studentUsername = :studentUsername OR studentUsername = 'ALL' ORDER BY timestamp DESC")
    fun getAssignmentsForStudent(studentUsername: String): Flow<List<Assignment>>

    @Query("SELECT * FROM student_assignments WHERE teacherUsername = :teacherUsername ORDER BY timestamp DESC")
    fun getAssignmentsByTeacher(teacherUsername: String): Flow<List<Assignment>>

    @Query("SELECT * FROM student_assignments ORDER BY timestamp DESC")
    fun getAllAssignments(): Flow<List<Assignment>>

    @Query("UPDATE student_assignments SET completed = :completed WHERE id = :id")
    suspend fun markAssignmentCompleted(id: Int, completed: Boolean)

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)
}
