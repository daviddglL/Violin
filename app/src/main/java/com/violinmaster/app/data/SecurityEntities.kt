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
    val skillLevel: String = "Beginner", // "Beginner", "Intermediate", "Advanced"
    val birthYear: Int = 0, // 0 = not set (legacy users). Required for new registrations.
    val firebaseUid: String? = null, // Firebase Auth UID; null for PIN-only users (REQ-AUTH-001, REQ-DB-006)
    val fcmToken: String = "", // FCM push token for notifications (REQ-FCM-004)
    val securityQuestion: String = "", // Key of the security question for PIN recovery (REQ-PINREC-001)
    val securityAnswerSalt: String = "", // Salt used to hash the security answer (REQ-PINREC-001)
    val securityAnswerHash: String = "" // Hashed security answer (REQ-PINREC-001)
) {
    /** Whether the user is a minor (under 18). Computed from birthYear. */
    val isMinor: Boolean
        get() {
            if (birthYear <= 1900) return false
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            return (currentYear - birthYear) < 18
        }
}

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
