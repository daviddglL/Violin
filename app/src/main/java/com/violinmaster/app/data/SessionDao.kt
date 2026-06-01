package com.violinmaster.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PracticeSession)

    @Query("SELECT * FROM practice_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<PracticeSession>>

    @Query("SELECT * FROM practice_sessions WHERE dateString BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getSessionsForDateRange(startDate: String, endDate: String): Flow<List<PracticeSession>>

    @Query("SELECT * FROM practice_sessions WHERE dateString = :dateString")
    fun getSessionsByDate(dateString: String): Flow<List<PracticeSession>>

    @Delete
    suspend fun deleteSession(session: PracticeSession)

    @Query("DELETE FROM practice_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Query("DELETE FROM practice_sessions")
    suspend fun clearAllSessions()

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM practice_sessions WHERE dateString BETWEEN :startDate AND :endDate")
    fun getTotalPracticeTimeForDateRange(startDate: String, endDate: String): Flow<Int>
}
