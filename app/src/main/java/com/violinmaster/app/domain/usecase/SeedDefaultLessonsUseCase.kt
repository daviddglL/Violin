package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.LessonProgress

/**
 * Seeds the database with the 9 default violin lessons.
 *
 * Only inserts lessons that do not already exist (REPLACE strategy on conflict).
 * No Android imports — delegating to [IPracticeRepository].
 */
class SeedDefaultLessonsUseCase constructor(
    private val repository: IPracticeRepository
) {
    /**
     * Inserts all 9 default lessons (Beginner, Intermediate, Advanced).
     */
    suspend operator fun invoke() {
        val defaults = listOf(
            LessonProgress("beg_1", "Posture & Open Strings Bowing", "Beginner", false, 0),
            LessonProgress("beg_2", "First Finger Patterns (D Major)", "Beginner", false, 0),
            LessonProgress("beg_3", "First Position Rhythms & Songs", "Beginner", false, 0),
            LessonProgress("int_1", "Relaxing Left Hand & Vibrato", "Intermediate", false, 0),
            LessonProgress("int_2", "Shifting to Third Position (III)", "Intermediate", false, 0),
            LessonProgress("int_3", "Double Stop Balance & Harmony", "Intermediate", false, 0),
            LessonProgress("adv_1", "Bowing Styles: Martelé, Spiccato", "Advanced", false, 0),
            LessonProgress("adv_2", "Paganini Practice Theme (A minor)", "Advanced", false, 0),
            LessonProgress("adv_3", "High Position Shifts (5th & 7th)", "Advanced", false, 0)
        )
        for (lesson in defaults) {
            repository.insertLessonProgress(lesson)
        }
    }
}
