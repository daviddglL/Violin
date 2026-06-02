package com.violinmaster.app.data.mapper

import com.violinmaster.app.data.Assignment
import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.data.PracticeSession
import com.violinmaster.app.data.UserAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for mapper utilities that convert between Room entities and domain models.
 *
 * REQ-ARCH-001: Data layer entities map to pure domain models.
 */
class MapperTest {

    // ── UserMapper ────────────────────────────────────────────────────

    @Test
    fun `UserMapper toDomain maps UserAccount to domain User`() {
        val entity = UserAccount(
            username = "violin_student",
            role = "STUDENT",
            hashedPassword = "hash123",
            salt = "salt456",
            teacherCode = "TEACH-0001",
            points = 150,
            skillLevel = "Advanced",
            birthYear = 2010
        )
        val domain = UserMapper.toDomain(entity)

        assertEquals("violin_student", domain.username)
        assertEquals("STUDENT", domain.role)
        assertEquals("Advanced", domain.skillLevel)
        assertEquals(150, domain.points)
        assertEquals("TEACH-0001", domain.teacherCode)
        assertEquals(2010, domain.birthYear)
        assertTrue(domain.isMinor)
    }

    @Test
    fun `UserMapper toEntity maps domain User to UserAccount`() {
        val domain = com.violinmaster.app.domain.model.User(
            username = "teacher_jane",
            role = "TEACHER",
            skillLevel = "Advanced",
            points = 200,
            teacherCode = "TEACH-0001",
            birthYear = 1990
        )
        val entity = UserMapper.toEntity(domain)

        assertEquals("teacher_jane", entity.username)
        assertEquals("TEACHER", entity.role)
        assertEquals("Advanced", entity.skillLevel)
        assertEquals(200, entity.points)
        assertEquals("TEACH-0001", entity.teacherCode)
        assertEquals(1990, entity.birthYear)
        assertFalse(entity.isMinor)
        // hashedPassword and salt are empty for domain→entity mapping (domain models don't carry auth)
        assertEquals("", entity.hashedPassword)
        assertEquals("", entity.salt)
    }

    @Test
    fun `UserMapper round-trip preserves data`() {
        val entity = UserAccount(
            username = "roundtrip",
            role = "FREELANCER",
            hashedPassword = "h",
            salt = "s",
            teacherCode = "CODE",
            points = 50,
            skillLevel = "Intermediate",
            birthYear = 2000
        )
        val domain = UserMapper.toDomain(entity)
        val backToEntity = UserMapper.toEntity(domain)

        assertEquals(entity.username, backToEntity.username)
        assertEquals(entity.role, backToEntity.role)
        assertEquals(entity.skillLevel, backToEntity.skillLevel)
        assertEquals(entity.points, backToEntity.points)
        assertEquals(entity.teacherCode, backToEntity.teacherCode)
        assertEquals(entity.birthYear, backToEntity.birthYear)
    }

    // ── PracticeSessionMapper ──────────────────────────────────────────

    @Test
    fun `PracticeSessionMapper toDomain maps entity to domain`() {
        val entity = PracticeSession(
            id = 42,
            dateString = "2026-06-01",
            durationSeconds = 1800,
            category = "Smart Tuner",
            timestamp = 1717200000000L
        )
        val domain = PracticeSessionMapper.toDomain(entity)

        assertEquals(42, domain.id)
        assertEquals("2026-06-01", domain.dateString)
        assertEquals(1800, domain.durationSeconds)
        assertEquals("Smart Tuner", domain.category)
        assertEquals(1717200000000L, domain.timestamp)
    }

    @Test
    fun `PracticeSessionMapper toEntity maps domain to entity`() {
        val domain = com.violinmaster.app.domain.model.PracticeSession(
            id = 99,
            dateString = "2026-05-15",
            durationSeconds = 900,
            category = "Metronome",
            timestamp = 1717100000000L
        )
        val entity = PracticeSessionMapper.toEntity(domain)

        assertEquals(99, entity.id)
        assertEquals("2026-05-15", entity.dateString)
        assertEquals(900, entity.durationSeconds)
        assertEquals("Metronome", entity.category)
        assertEquals(1717100000000L, entity.timestamp)
    }

    // ── LessonProgressMapper ───────────────────────────────────────────

    @Test
    fun `LessonProgressMapper toDomain maps entity to domain Lesson`() {
        val entity = LessonProgress(
            lessonId = "mod_1",
            lessonTitle = "Intro to Bowing",
            difficulty = "Beginner",
            completed = true,
            totalPracticedSeconds = 3600,
            lastPracticedTimestamp = 1717200000000L
        )
        val domain = LessonProgressMapper.toDomain(entity)

        assertEquals("mod_1", domain.lessonId)
        assertEquals("Intro to Bowing", domain.title)
        assertEquals("Beginner", domain.difficulty)
        assertTrue(domain.completed)
        assertEquals(3600, domain.totalPracticedSeconds)
    }

    @Test
    fun `LessonProgressMapper toEntity maps domain Lesson to entity`() {
        val domain = com.violinmaster.app.domain.model.Lesson(
            lessonId = "mod_5",
            title = "Vibrato Techniques",
            difficulty = "Advanced",
            completed = false,
            totalPracticedSeconds = 1200
        )
        val entity = LessonProgressMapper.toEntity(domain)

        assertEquals("mod_5", entity.lessonId)
        assertEquals("Vibrato Techniques", entity.lessonTitle)
        assertEquals("Advanced", entity.difficulty)
        assertFalse(entity.completed)
        assertEquals(1200, entity.totalPracticedSeconds)
    }

    // ── AssignmentMapper ───────────────────────────────────────────────

    @Test
    fun `AssignmentMapper toDomain maps entity to domain Assignment`() {
        val entity = Assignment(
            id = 7,
            title = "Scale Practice",
            description = "Play D major scale with metronome.",
            teacherUsername = "teacher_jane",
            studentUsername = "student_bob",
            videoTitle = "Scale Demo",
            videoDurationSeconds = 120,
            videoResourceUrl = "https://example.com/video.mp4",
            timestamp = 1717200000000L,
            completed = true
        )
        val domain = AssignmentMapper.toDomain(entity)

        assertEquals(7, domain.id)
        assertEquals("Scale Practice", domain.title)
        assertEquals("Play D major scale with metronome.", domain.description)
        assertEquals("teacher_jane", domain.teacherUsername)
        assertEquals("student_bob", domain.studentUsername)
        assertTrue(domain.completed)
        assertEquals(1717200000000L, domain.timestamp)
    }

    @Test
    fun `AssignmentMapper toEntity maps domain Assignment to entity`() {
        val domain = com.violinmaster.app.domain.model.Assignment(
            id = 3,
            title = "Daily Warmup",
            description = "10 minutes of scales",
            teacherUsername = "teacher_jane",
            studentUsername = "ALL",
            completed = false,
            timestamp = 1717100000000L
        )
        val entity = AssignmentMapper.toEntity(domain)

        assertEquals(3, entity.id)
        assertEquals("Daily Warmup", entity.title)
        assertEquals("10 minutes of scales", entity.description)
        assertEquals("teacher_jane", entity.teacherUsername)
        assertEquals("ALL", entity.studentUsername)
        assertFalse(entity.completed)
        assertEquals(1717100000000L, entity.timestamp)
    }
}
