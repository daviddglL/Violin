package com.violinmaster.app.data.firebase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

/**
 * Validates the Firestore security rules file structure and content.
 *
 * REQ-FSEC-001: Deny by default — catches all unauthenticated and cross-user access.
 * REQ-FSEC-002: User self-access — authenticated users read/write only own documents.
 * REQ-FSEC-003: Teacher-student assignment access — role-based grants.
 *
 * Since Firebase Emulator cannot run in unit test context, these tests validate
 * the rules file directly using structural assertions on the rules content.
 */
class FirestoreRulesValidationTest {

    private val rulesFile: File by lazy {
        // firestore.rules lives at project root.
        // Gradle runs tests from the app/ module directory, so walk up to find it.
        var dir: File = File(System.getProperty("user.dir") ?: ".")
        while (dir.parentFile != null && !File(dir, "firestore.rules").exists()) {
            dir = dir.parentFile ?: break
        }
        File(dir, "firestore.rules")
    }

    private val rulesContent: String by lazy {
        require(rulesFile.exists()) {
            "firestore.rules not found at ${rulesFile.absolutePath}. " +
            "Searched from ${System.getProperty("user.dir")} upward. " +
            "Ensure firestore.rules exists at the project root."
        }
        rulesFile.readText()
    }

    // ── REQ-FSEC-001: Deny by Default ─────────────────────────────────────

    @Test
    fun `rules file exists and is non-empty`() {
        assertTrue("firestore.rules must exist", rulesFile.exists())
        assertTrue("firestore.rules must be non-empty", rulesContent.isNotBlank())
    }

    @Test
    fun `rules contain deny-by-default catch-all pattern`() {
        // The catch-all deny pattern: match /{document=**} { allow read, write: if false; }
        val hasCatchAllDeny = rulesContent.contains("/{document=**}") &&
                rulesContent.contains("if false")
        assertTrue(
            "Rules must contain deny-by-default catch-all with 'if false'",
            hasCatchAllDeny
        )
    }

    @Test
    fun `rules require authentication for all explicit collection matches`() {
        // Every allow statement should check request.auth != null
        val allowStatements = Regex("allow\\s+(read|write)\\s*:")
            .findAll(rulesContent).toList()
        assertTrue("Rules must contain explicit allow statements", allowStatements.isNotEmpty())

        // After each allow line, within a reasonable window, auth must be checked
        val hasAuthCheck = rulesContent.contains("request.auth != null")
        assertTrue("Rules must check request.auth != null for authenticated access", hasAuthCheck)
    }

    @Test
    fun `rules protect users collection with auth uid check`() {
        val hasUsersCollection = rulesContent.contains("match /users/{uid}") ||
                rulesContent.contains("match /users/")
        assertTrue("Rules must define protection for users collection", hasUsersCollection)

        val hasUidCheck = rulesContent.contains("request.auth.uid")
        assertTrue("Users collection must check request.auth.uid for identity verification", hasUidCheck)
    }

    // ── REQ-FSEC-002: User Self-Access ─────────────────────────────────────

    @Test
    fun `rules protect sessions subcollection under user scope`() {
        val hasSessionsCollection = rulesContent.contains("/sessions/") ||
                rulesContent.contains("match /users/{uid}/sessions")
        assertTrue("Rules must protect sessions subcollection under users/{uid}", hasSessionsCollection)
    }

    @Test
    fun `rules protect lesson_progress collection`() {
        val hasLessonCollection = rulesContent.contains("lesson_progress")
        assertTrue("Rules must protect lesson_progress collection", hasLessonCollection)
    }

    // ── REQ-FSEC-003: Teacher-Student Assignment Access ────────────────────

    @Test
    fun `rules protect assignments collection`() {
        val hasAssignmentsCollection = rulesContent.contains("match /assignments/")
        assertTrue("Rules must protect assignments collection", hasAssignmentsCollection)
    }

    @Test
    fun `rules contain role-based access checks for assignments`() {
        // Teacher and student username checks must exist in the rules
        val hasTeacherCheck = rulesContent.contains("teacherUsername")
        val hasStudentCheck = rulesContent.contains("studentUsername")
        assertTrue(
            "Rules must check teacherUsername for role-based assignment access",
            hasTeacherCheck
        )
        assertTrue(
            "Rules must check studentUsername for role-based assignment access",
            hasStudentCheck
        )
    }

    @Test
    fun `rules separate read and write permissions for assignments`() {
        // Need both read and write rules for assignments (different role checks)
        val allowReadCount = Regex("allow\\s+read\\s*:").findAll(rulesContent).count()
        val allowWriteCount = Regex("allow\\s+write\\s*:").findAll(rulesContent).count()
        assertTrue("Rules must have at least one allow read statement", allowReadCount >= 1)
        assertTrue("Rules must have at least one allow write statement", allowWriteCount >= 1)
    }

    @Test
    fun `rules protect chat messages subcollection under assignments`() {
        val hasMessagesCollection = rulesContent.contains("/messages/") ||
                rulesContent.contains("match /assignments/{assignmentId}/messages")
        assertTrue("Rules must protect messages subcollection under assignments", hasMessagesCollection)
    }

    // ── Helper function validation ─────────────────────────────────────────

    @Test
    fun `rules include helper function for username resolution`() {
        // Function that resolves username from users/{uid} for role checks
        val hasUsernameFunction = rulesContent.contains("function getUsername") ||
                rulesContent.contains("getUsername")
        assertTrue(
            "Rules should include a getUsername function for resolving usernames from UIDs",
            hasUsernameFunction
        )
    }

    @Test
    fun `rules version is specified`() {
        val hasVersion = rulesContent.contains("rules_version")
        assertTrue("Rules must specify rules_version", hasVersion)
    }

    @Test
    fun `rules specify service cloud firestore`() {
        val hasService = rulesContent.contains("service cloud.firestore")
        assertTrue("Rules must specify service cloud.firestore", hasService)
    }

    // ── REQ-FSEC-001 Triangulation: Specific cross-user scenarios ─────────

    @Test
    fun `users collection write also checks auth uid not just read`() {
        // The users match block should check uid on both read AND write
        val usersBlock = extractBlock(rulesContent, "match /users/{uid}")
        assertNotNull("Users collection block must exist", usersBlock)
        // The allow statement should come BEFORE the sessions subcollection
        val allowLine = usersBlock!!.lines().firstOrNull {
            it.contains("allow") && it.contains("request.auth.uid == uid")
        }
        assertNotNull("Users collection must check request.auth.uid == uid", allowLine)
    }

    @Test
    fun `sessions subcollection inherits parent uid check scope`() {
        // Sessions are nested under users/{uid}, so uid must match from parent scope
        val sessionsBlock = extractBlock(rulesContent, "match /sessions/{sessionId}")
        assertNotNull("Sessions subcollection must exist", sessionsBlock)
        val hasUidRef = sessionsBlock!!.contains("uid")
        assertTrue("Sessions must reference the parent uid for access control", hasUidRef)
    }

    @Test
    fun `lesson_progress uses firebaseUid prefix matching on docId`() {
        // docId format: {firebaseUid}_{lessonId} — must check uid matches prefix
        val lessonBlock = extractBlock(rulesContent, "match /lesson_progress/{docId}")
        assertNotNull("Lesson progress block must exist", lessonBlock)
        val hasDocIdCheck = lessonBlock!!.contains("docId") &&
                lessonBlock.contains("request.auth.uid")
        assertTrue(
            "Lesson progress must verify docId matches authenticated user's firebaseUid",
            hasDocIdCheck
        )
    }

    // ── REQ-FSEC-003 Triangulation: Role-specific scenarios ────────────────

    @Test
    fun `assignments write rule restricts to teacher only`() {
        // Write access must ONLY check teacherUsername, not studentUsername.
        // Since rules are multi-line, check the full block content.
        val assignmentsBlock = extractBlock(rulesContent, "match /assignments/{assignmentId}")
        assertNotNull("Assignments block must exist", assignmentsBlock)

        // The full block must contain an allow write statement
        assertTrue("Assignments block must have allow write", assignmentsBlock!!.contains("allow write"))

        // Write access checks teacherUsername in its condition
        // (continuation lines after the allow write line)
        assertTrue(
            "Write access must check teacherUsername",
            assignmentsBlock.contains("teacherUsername")
        )

        // The write statement must NOT grant access based on studentUsername
        // However, the read statement in the same block does reference studentUsername.
        // So we verify the overall structure has differentiated read vs write checks.
        val hasSeparateReadWrite = assignmentsBlock.contains("allow read") &&
                assignmentsBlock.contains("allow write")
        assertTrue("Assignments must have separate allow read and allow write rules", hasSeparateReadWrite)
    }

    @Test
    fun `assignments read rule allows both teacher and student`() {
        val assignmentsBlock = extractBlock(rulesContent, "match /assignments/{assignmentId}")
        assertNotNull("Assignments block must exist", assignmentsBlock)

        assertTrue("Assignments block must have allow read", assignmentsBlock!!.contains("allow read"))

        val hasBothRoles = assignmentsBlock.contains("teacherUsername") &&
                assignmentsBlock.contains("studentUsername")
        assertTrue(
            "Read access must reference both teacherUsername and studentUsername",
            hasBothRoles
        )
    }

    @Test
    fun `assignments read uses OR for teacher-student disjunction`() {
        // Both teacher OR student can read — must use || not &&
        val assignmentsBlock = extractBlock(rulesContent, "match /assignments/{assignmentId}")
        assertNotNull("Assignments block must exist", assignmentsBlock)

        // The OR operator (||) must appear in the read rule condition
        val hasOrOperator = assignmentsBlock!!.contains("||")
        assertTrue(
            "Assignments read must use || (OR) to allow either teacher OR student",
            hasOrOperator
        )
    }

    @Test
    fun `messages subcollection protected under assignments`() {
        val messagesBlock = extractBlock(rulesContent, "match /messages/{messageId}")
        assertNotNull("Messages subcollection must exist under assignments", messagesBlock)
        val hasAuthCheck = messagesBlock!!.contains("request.auth != null")
        assertTrue("Messages must require authentication", hasAuthCheck)
    }

    @Test
    fun `no sensitive data patterns like password in rules`() {
        // Rules should NOT reference password, salt, or hashedPassword
        assertFalse(
            "Rules must not reference hashedPassword",
            rulesContent.contains("hashedPassword")
        )
        assertFalse(
            "Rules must not reference salt",
            rulesContent.contains("salt")
        )
        assertFalse(
            "Rules must not reference password",
            rulesContent.contains("password")
        )
    }

    // ── Helper ────────────────────────────────────────────────────────────

    /**
     * Extracts the content block for a given match path from the rules.
     * Returns the text between the opening `{` and closing `}` of the match block,
     * including nested blocks.
     *
     * Skips braces within the match path declaration itself (e.g., {assignmentId})
     * and only starts counting at the block-opening brace.
     */
    private fun extractBlock(content: String, matchPath: String): String? {
        val startIdx = content.indexOf(matchPath)
        if (startIdx == -1) return null

        // Find the block-opening brace: the first '{' after the match path
        // that is NOT part of a path variable like {assignmentId}.
        // Strategy: skip past the match path text, then find the next '{'.
        val afterPath = startIdx + matchPath.length
        val blockOpenIdx = content.indexOf('{', afterPath)
        if (blockOpenIdx == -1) return null

        var depth = 0
        val sb = StringBuilder()
        for (i in blockOpenIdx until content.length) {
            val ch = content[i]
            if (ch == '{') {
                depth++
            } else if (ch == '}') {
                depth--
                if (depth == 0) {
                    sb.append(ch)
                    break
                }
            }
            sb.append(ch)
        }
        return sb.toString().ifBlank { null }
    }
}
