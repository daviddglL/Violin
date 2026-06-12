package com.violinmaster.app.data.firebase

/**
 * Firestore-compatible user document for cloud ↔ local reconciliation.
 *
 * Mirrors [com.violinmaster.app.data.UserAccount] without sensitive fields
 * (hashedPassword and salt are NEVER stored in Firestore — REQ-AUTH-001).
 */
data class UserDoc(
    val username: String = "",
    val role: String = "FREELANCER",
    val teacherCode: String = "",
    val points: Int = 0,
    val skillLevel: String = "Beginner",
    val birthYear: Int = 0,
    val firebaseUid: String = ""
) {
    companion object {
        /**
         * Creates a [UserDoc] from a [com.violinmaster.app.data.UserAccount],
         * excluding sensitive auth fields (hashedPassword, salt).
         */
        fun fromEntity(entity: com.violinmaster.app.data.UserAccount): UserDoc = UserDoc(
            username = entity.username,
            role = entity.role,
            teacherCode = entity.teacherCode,
            points = entity.points,
            skillLevel = entity.skillLevel,
            birthYear = entity.birthYear,
            firebaseUid = entity.firebaseUid ?: ""
        )
    }

    /**
     * Converts this Firestore document back to a Room [com.violinmaster.app.data.UserAccount].
     * hashedPassword and salt are empty since they are never stored in Firestore.
     */
    fun toEntity(): com.violinmaster.app.data.UserAccount =
        com.violinmaster.app.data.UserAccount(
            username = username,
            role = role,
            hashedPassword = "",
            salt = "",
            teacherCode = teacherCode,
            points = points,
            skillLevel = skillLevel,
            birthYear = birthYear,
            firebaseUid = firebaseUid.ifBlank { null }
        )
}
