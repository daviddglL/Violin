package com.violinmaster.app.data.firebase

/**
 * Firestore-compatible user document for cloud ↔ local reconciliation.
 *
 * Mirrors [com.violinmaster.app.data.UserAccount]. Credentials (hashedPassword, salt)
 * are included to enable cross-device login. Values are SHA-256 hashed with per-user
 * random salt — the original PIN is NEVER stored or transmitted.
 *
 * Firestore security rules (REQ-FSEC-002) restrict access to the owner's own document,
 * so credentials are never readable by other users.
 */
data class UserDoc(
    val username: String = "",
    val role: String = "FREELANCER",
    val teacherCode: String = "",
    val points: Int = 0,
    val skillLevel: String = "Beginner",
    val birthYear: Int = 0,
    val firebaseUid: String = "",
    val fcmToken: String = "",
    val hashedPassword: String = "",
    val salt: String = ""
) {
    companion object {
        /**
         * Creates a [UserDoc] from a [com.violinmaster.app.data.UserAccount],
         * including hashedPassword and salt for cross-device login support.
         */
        fun fromEntity(entity: com.violinmaster.app.data.UserAccount): UserDoc = UserDoc(
            username = entity.username,
            role = entity.role,
            teacherCode = entity.teacherCode,
            points = entity.points,
            skillLevel = entity.skillLevel,
            birthYear = entity.birthYear,
            firebaseUid = entity.firebaseUid ?: "",
            fcmToken = entity.fcmToken,
            hashedPassword = entity.hashedPassword,
            salt = entity.salt
        )
    }

    /**
     * Converts this Firestore document back to a Room [com.violinmaster.app.data.UserAccount].
     * hashedPassword and salt are restored from Firestore for cross-device login.
     */
    fun toEntity(): com.violinmaster.app.data.UserAccount =
        com.violinmaster.app.data.UserAccount(
            username = username,
            role = role,
            hashedPassword = hashedPassword,
            salt = salt,
            teacherCode = teacherCode,
            points = points,
            skillLevel = skillLevel,
            birthYear = birthYear,
            firebaseUid = firebaseUid.ifBlank { null },
            fcmToken = fcmToken
        )
}
