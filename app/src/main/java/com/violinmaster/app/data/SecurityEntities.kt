package com.violinmaster.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val username: String,
    val role: String, // "TEACHER", "STUDENT", "FREELANCER"
    val hashedPassword: String,
    val salt: String,
    val teacherCode: String = "", // For teachers, this is their OWN invite code; for students, this is who they linked to
    val points: Int = 0,
    val skillLevel: String = "Beginner" // "Beginner", "Intermediate", "Advanced"
)

@Entity(tableName = "student_assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val teacherUsername: String,
    val studentUsername: String, // either a specific student's username or "ALL"
    val videoTitle: String = "", // Optional short tutorial video attached
    val videoDurationSeconds: Int = 0, // max 180 seconds (3 mins)
    val videoResourceUrl: String = "", // signed simulated streaming link
    val timestamp: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)
