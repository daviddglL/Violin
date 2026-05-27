package com.violinmaster.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_sessions")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // format: "YYYY-MM-DD"
    val durationSeconds: Int,
    val category: String, // e.g., "Smart Tuner", "Metronome", "Advanced Bowing Techniques"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "lesson_progress")
data class LessonProgress(
    @PrimaryKey val lessonId: String,
    val lessonTitle: String,
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val completed: Boolean = false,
    val totalPracticedSeconds: Int = 0,
    val lastPracticedTimestamp: Long = 0
)
