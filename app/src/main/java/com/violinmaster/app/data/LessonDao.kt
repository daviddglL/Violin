package com.violinmaster.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonProgress(progress: LessonProgress)

    @Update
    suspend fun updateLesson(lesson: LessonProgress)

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getLessonById(lessonId: String): LessonProgress?

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getLessonProgressById(lessonId: String): LessonProgress?

    @Query("SELECT * FROM lesson_progress ORDER BY difficulty ASC, lessonTitle ASC")
    fun getAllLessons(): Flow<List<LessonProgress>>

    @Query("SELECT * FROM lesson_progress ORDER BY difficulty ASC, lessonTitle ASC")
    fun getAllLessonProgress(): Flow<List<LessonProgress>>

    @Query("SELECT * FROM lesson_progress WHERE completed = 1 ORDER BY lessonTitle ASC")
    fun getAllCompletedLessons(): Flow<List<LessonProgress>>

    @Query("UPDATE lesson_progress SET completed = :completed WHERE lessonId = :lessonId")
    suspend fun markLessonCompleted(lessonId: String, completed: Boolean)

    @Query("UPDATE lesson_progress SET completed = :completed WHERE lessonId = :lessonId")
    suspend fun updateLessonCompletion(lessonId: String, completed: Boolean)

    @Query("DELETE FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun deleteLessonProgress(lessonId: String)
}
