package com.violinmaster.app.data.firebase

import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Firestore document ↔ Room entity mapping.
 *
 * REQ-CSYNC-004: Verifies that all entity-to-document conversions preserve
 * field values correctly and that sensitive fields (hashedPassword, salt)
 * are never included in Firestore documents.
 */
class FirestoreDocMapperTest {

    // ── SessionDoc Mapper Tests ──────────────────────────────────────────

    @Test
    fun `SessionDoc fromEntity preserves all PracticeSession fields`() {
        val entity = PracticeSession(
            id = 42,
            dateString = "2026-06-12",
            durationSeconds = 3600,
            category = "Smart Tuner",
            timestamp = 1717000000000L
        )
        val doc = SessionDoc.fromEntity(entity)

        assertEquals("docId should match entity id", "42", doc.id)
        assertEquals("2026-06-12", doc.dateString)
        assertEquals(3600, doc.durationSeconds)
        assertEquals("Smart Tuner", doc.category)
        assertEquals(1717000000000L, doc.timestamp)
    }

    @Test
    fun `SessionDoc toEntity reconstructs PracticeSession correctly`() {
        val doc = SessionDoc(
            id = "99",
            dateString = "2026-01-15",
            durationSeconds = 1800,
            category = "Metronome",
            timestamp = 1717000000000L
        )
        val entity = doc.toEntity()

        assertEquals(99, entity.id)
        assertEquals("2026-01-15", entity.dateString)
        assertEquals(1800, entity.durationSeconds)
        assertEquals("Metronome", entity.category)
        assertEquals(1717000000000L, entity.timestamp)
    }

    @Test
    fun `SessionDoc round-trip fromEntity then toEntity is identity`() {
        val original = PracticeSession(
            id = 7,
            dateString = "2026-03-01",
            durationSeconds = 900,
            category = "Advanced Bowing Techniques",
            timestamp = 1716883200000L
        )
        val doc = SessionDoc.fromEntity(original)
        val reconstructed = doc.toEntity()

        assertEquals(original.id, reconstructed.id)
        assertEquals(original.dateString, reconstructed.dateString)
        assertEquals(original.durationSeconds, reconstructed.durationSeconds)
        assertEquals(original.category, reconstructed.category)
        assertEquals(original.timestamp, reconstructed.timestamp)
    }

    // ── LessonDoc Mapper Tests ───────────────────────────────────────────

    @Test
    fun `LessonDoc fromEntity preserves all LessonProgress fields`() {
        val entity = LessonProgress(
            lessonId = "lesson_12",
            lessonTitle = "Vibrato Basics",
            difficulty = "Intermediate",
            completed = true,
            totalPracticedSeconds = 5400,
            lastPracticedTimestamp = 1717000000000L
        )
        val doc = LessonDoc.fromEntity(entity)

        assertEquals("lesson_12", doc.lessonId)
        assertEquals("Vibrato Basics", doc.lessonTitle)
        assertEquals("Intermediate", doc.difficulty)
        assertTrue(doc.completed)
        assertEquals(5400, doc.totalPracticedSeconds)
        assertEquals(1717000000000L, doc.lastPracticedTimestamp)
    }

    @Test
    fun `LessonDoc toEntity reconstructs LessonProgress correctly`() {
        val doc = LessonDoc(
            lessonId = "lesson_5",
            lessonTitle = "Scales in Thirds",
            difficulty = "Advanced",
            completed = false,
            totalPracticedSeconds = 1200,
            lastPracticedTimestamp = 1716883200000L
        )
        val entity = doc.toEntity()

        assertEquals("lesson_5", entity.lessonId)
        assertEquals("Scales in Thirds", entity.lessonTitle)
        assertEquals("Advanced", entity.difficulty)
        assertFalse(entity.completed)
        assertEquals(1200, entity.totalPracticedSeconds)
        assertEquals(1716883200000L, entity.lastPracticedTimestamp)
    }

    @Test
    fun `LessonDoc round-trip fromEntity then toEntity is identity`() {
        val original = LessonProgress(
            lessonId = "intro_1",
            lessonTitle = "Introduction to the Violin",
            difficulty = "Beginner",
            completed = false,
            totalPracticedSeconds = 0,
            lastPracticedTimestamp = 0L
        )
        val doc = LessonDoc.fromEntity(original)
        val reconstructed = doc.toEntity()

        assertEquals(original.lessonId, reconstructed.lessonId)
        assertEquals(original.lessonTitle, reconstructed.lessonTitle)
        assertEquals(original.difficulty, reconstructed.difficulty)
        assertEquals(original.completed, reconstructed.completed)
        assertEquals(original.totalPracticedSeconds, reconstructed.totalPracticedSeconds)
        assertEquals(original.lastPracticedTimestamp, reconstructed.lastPracticedTimestamp)
    }

    // ── AssignmentDoc Mapper Tests ───────────────────────────────────────

    @Test
    fun `AssignmentDoc fromEntity preserves all Assignment fields`() {
        val entity = Assignment(
            id = 15,
            title = "Practice Scales",
            description = "Play all major scales slowly",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob",
            videoTitle = "Scale Tutorial",
            videoDurationSeconds = 120,
            videoResourceUrl = "https://storage.example.com/scale.mp4",
            timestamp = 1717000000000L,
            completed = false
        )
        val doc = AssignmentDoc.fromEntity(entity)

        assertEquals("15", doc.id)
        assertEquals("Practice Scales", doc.title)
        assertEquals("Play all major scales slowly", doc.description)
        assertEquals("teacher_jane", doc.teacherUsername)
        assertEquals("student_bob", doc.studentUsername)
        assertEquals("Scale Tutorial", doc.videoTitle)
        assertEquals(120, doc.videoDurationSeconds)
        assertEquals("https://storage.example.com/scale.mp4", doc.videoResourceUrl)
        assertEquals(1717000000000L, doc.timestamp)
        assertFalse(doc.completed)
    }

    @Test
    fun `AssignmentDoc toEntity reconstructs Assignment correctly`() {
        val doc = AssignmentDoc(
            id = "23",
            title = "Memorize Piece",
            description = "First movement",
            teacherUsername = "teacher_sam",
            studentUsername = "ALL",
            videoTitle = "",
            videoDurationSeconds = 0,
            videoResourceUrl = "",
            timestamp = 1716883200000L,
            completed = true
        )
        val entity = doc.toEntity()

        assertEquals(23, entity.id)
        assertEquals("Memorize Piece", entity.title)
        assertEquals("First movement", entity.description)
        assertEquals("teacher_sam", entity.teacherUsername)
        assertEquals("ALL", entity.studentUsername)
        assertEquals("", entity.videoTitle)
        assertEquals(0, entity.videoDurationSeconds)
        assertEquals("", entity.videoResourceUrl)
        assertEquals(1716883200000L, entity.timestamp)
        assertTrue(entity.completed)
    }

    @Test
    fun `AssignmentDoc round-trip fromEntity then toEntity is identity`() {
        val original = Assignment(
            id = 3,
            title = "Arpeggio Exercise",
            description = "C major arpeggios at 60 bpm",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob",
            videoTitle = "Arpeggio Demo",
            videoDurationSeconds = 45,
            videoResourceUrl = "https://storage.example.com/arpeggio.mp4",
            timestamp = 1717000000000L,
            completed = true
        )
        val doc = AssignmentDoc.fromEntity(original)
        val reconstructed = doc.toEntity()

        assertEquals(original.id, reconstructed.id)
        assertEquals(original.title, reconstructed.title)
        assertEquals(original.description, reconstructed.description)
        assertEquals(original.teacherUsername, reconstructed.teacherUsername)
        assertEquals(original.studentUsername, reconstructed.studentUsername)
        assertEquals(original.videoTitle, reconstructed.videoTitle)
        assertEquals(original.videoDurationSeconds, reconstructed.videoDurationSeconds)
        assertEquals(original.videoResourceUrl, reconstructed.videoResourceUrl)
        assertEquals(original.timestamp, reconstructed.timestamp)
        assertEquals(original.completed, reconstructed.completed)
    }

    // ── UserDoc Mapper Tests (existing, verify not broken) ───────────────

    @Test
    fun `UserDoc fromEntity excludes hashedPassword and salt`() {
        val entity = UserAccount(
            username = "test_user",
            role = "STUDENT",
            hashedPassword = "secret_hash",
            salt = "random_salt",
            teacherCode = "CODE123",
            points = 500,
            skillLevel = "Intermediate",
            birthYear = 2005,
            firebaseUid = "firebase_uid_abc"
        )
        val doc = UserDoc.fromEntity(entity)

        assertEquals("test_user", doc.username)
        assertEquals("STUDENT", doc.role)
        assertEquals("CODE123", doc.teacherCode)
        assertEquals(500, doc.points)
        assertEquals("Intermediate", doc.skillLevel)
        assertEquals(2005, doc.birthYear)
        assertEquals("firebase_uid_abc", doc.firebaseUid)
        // Sensitive fields are NOT in UserDoc at all — structural guarantee
    }

    @Test
    fun `UserDoc toEntity preserves all UserAccount fields with empty auth`() {
        val doc = UserDoc(
            username = "cloud_user",
            role = "TEACHER",
            teacherCode = "TEACH123",
            points = 1000,
            skillLevel = "Advanced",
            birthYear = 1985,
            firebaseUid = "uid_xyz",
        )
        val entity = doc.toEntity()

        assertEquals("cloud_user", entity.username)
        assertEquals("TEACHER", entity.role)
        assertEquals("", entity.hashedPassword) // Never stored in Firestore
        assertEquals("", entity.salt)           // Never stored in Firestore
        assertEquals("TEACH123", entity.teacherCode)
        assertEquals(1000, entity.points)
        assertEquals("Advanced", entity.skillLevel)
        assertEquals(1985, entity.birthYear)
        assertEquals("uid_xyz", entity.firebaseUid)
    }
}
