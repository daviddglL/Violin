package com.violinmaster.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for domain model data classes.
 *
 * Verifies that all domain models can be instantiated without Room dependencies
 * and that computed properties (like isMinor) work correctly using pure java.time.
 *
 * REQ-ARCH-001: Domain models must be pure Kotlin, no Android imports.
 */
class DomainModelsTest {

    // ── User ─────────────────────────────────────────────────────────

    @Test
    fun `User instantiation without Room dependencies`() {
        val user = User(
            username = "violin_student",
            role = "STUDENT",
            skillLevel = "Beginner",
            points = 150,
            teacherCode = "TEACH-0001",
            birthYear = 2010
        )

        assertEquals("violin_student", user.username)
        assertEquals("STUDENT", user.role)
        assertEquals("Beginner", user.skillLevel)
        assertEquals(150, user.points)
        assertEquals("TEACH-0001", user.teacherCode)
        assertEquals(2010, user.birthYear)
        assertTrue(user.isMinor) // computed from birthYear
    }

    @Test
    fun `User isMinor computes correctly from birthYear`() {
        val currentYear = LocalDate.now().year
        val minorBirthYear = currentYear - 10 // 10 years old → minor
        val adultBirthYear = currentYear - 25 // 25 years old → adult
        val edgeBirthYear = currentYear - 18 // exactly 18 → not minor

        val minor = User(
            username = "kid",
            role = "STUDENT",
            birthYear = minorBirthYear
        )
        val adult = User(
            username = "adult",
            role = "TEACHER",
            birthYear = adultBirthYear
        )
        val edge = User(
            username = "edge18",
            role = "STUDENT",
            birthYear = edgeBirthYear
        )

        assertTrue("10-year-old should be minor", minor.isMinor)
        assertFalse("25-year-old should NOT be minor", adult.isMinor)
        assertFalse("18-year-old should NOT be minor", edge.isMinor)
    }

    @Test
    fun `User isMinor returns false for birthYear 0 legacy users`() {
        val legacy = User(
            username = "legacy_user",
            role = "STUDENT",
            birthYear = 0
        )
        assertFalse("Legacy users with birthYear 0 should not be minor", legacy.isMinor)
    }

    @Test
    fun `User default values are correct`() {
        val user = User(
            username = "minimal",
            role = "STUDENT"
        )
        assertEquals("Beginner", user.skillLevel)
        assertEquals(0, user.points)
        assertEquals("", user.teacherCode)
        assertEquals(0, user.birthYear)
        assertFalse(user.isMinor)
    }

    // ── PracticeSession ───────────────────────────────────────────────

    @Test
    fun `PracticeSession instantiation without Room dependencies`() {
        val session = PracticeSession(
            id = 1,
            dateString = "2026-06-01",
            durationSeconds = 1800,
            category = "Smart Tuner",
            timestamp = 1717200000000L
        )

        assertEquals(1, session.id)
        assertEquals("2026-06-01", session.dateString)
        assertEquals(1800, session.durationSeconds)
        assertEquals("Smart Tuner", session.category)
        assertEquals(1717200000000L, session.timestamp)
    }

    @Test
    fun `PracticeSession default values`() {
        val session = PracticeSession(
            dateString = "2026-06-01",
            durationSeconds = 0,
            category = ""
        )

        assertEquals(0, session.id)
        assertTrue(session.timestamp > 0) // auto-generated timestamp
    }

    // ── Lesson ────────────────────────────────────────────────────────

    @Test
    fun `Lesson instantiation without Room dependencies`() {
        val lesson = Lesson(
            lessonId = "mod_1",
            title = "Intro to Bowing",
            difficulty = "Beginner",
            completed = true,
            totalPracticedSeconds = 3600
        )

        assertEquals("mod_1", lesson.lessonId)
        assertEquals("Intro to Bowing", lesson.title)
        assertEquals("Beginner", lesson.difficulty)
        assertTrue(lesson.completed)
        assertEquals(3600, lesson.totalPracticedSeconds)
    }

    @Test
    fun `Lesson default values`() {
        val lesson = Lesson(
            lessonId = "mod_x",
            title = "Test",
            difficulty = "Intermediate"
        )

        assertFalse(lesson.completed)
        assertEquals(0, lesson.totalPracticedSeconds)
    }

    // ── Assignment ────────────────────────────────────────────────────

    @Test
    fun `Assignment instantiation without Room dependencies`() {
        val assignment = Assignment(
            id = 42,
            title = "Vibrato Drill",
            description = "Practice slow vibrato on G string.",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob",
            completed = false,
            timestamp = 1717200000000L
        )

        assertEquals(42, assignment.id)
        assertEquals("Vibrato Drill", assignment.title)
        assertEquals("Practice slow vibrato on G string.", assignment.description)
        assertEquals("teacher_jane", assignment.teacherUsername)
        assertEquals("student_bob", assignment.studentUsername)
        assertFalse(assignment.completed)
        assertEquals(1717200000000L, assignment.timestamp)
    }

    @Test
    fun `Assignment default values`() {
        val assignment = Assignment(
            title = "Test",
            description = "",
            teacherUsername = "t",
            studentUsername = "s"
        )

        assertEquals(0, assignment.id)
        assertFalse(assignment.completed)
        assertTrue(assignment.timestamp > 0)
    }

    // ── Message ───────────────────────────────────────────────────────

    @Test
    fun `Message instantiation without Room dependencies`() {
        val message = Message(
            id = "msg_001",
            assignmentId = "assign_42",
            senderUsername = "teacher_jane",
            senderRole = "TEACHER",
            text = "Great vibrato!",
            timestamp = 1717200000000L
        )

        assertEquals("msg_001", message.id)
        assertEquals("assign_42", message.assignmentId)
        assertEquals("teacher_jane", message.senderUsername)
        assertEquals("TEACHER", message.senderRole)
        assertEquals("Great vibrato!", message.text)
        assertEquals(1717200000000L, message.timestamp)
    }

    @Test
    fun `Message default values`() {
        val message = Message(
            assignmentId = "a1",
            senderUsername = "s",
            senderRole = "STUDENT",
            text = ""
        )

        assertEquals("", message.id)
        assertTrue(message.timestamp > 0)
    }

    // ── No Android imports verification ────────────────────────────────

    @Test
    fun `domain models have no Room annotations`() {
        // Verify classes are pure Kotlin data classes without @Entity, @PrimaryKey
        val userClass = User::class.java
        val sessionClass = PracticeSession::class.java
        val lessonClass = Lesson::class.java
        val assignmentClass = Assignment::class.java
        val messageClass = Message::class.java

        // No Room annotations on any domain model
        for (clazz in listOf(userClass, sessionClass, lessonClass, assignmentClass, messageClass)) {
            val annotations = clazz.annotations.map { it.annotationClass.qualifiedName ?: "" }
            val hasRoomAnnotation = annotations.any { it.contains("androidx.room") }
            assertFalse("${clazz.simpleName} should not have Room annotations", hasRoomAnnotation)
        }
    }
}
