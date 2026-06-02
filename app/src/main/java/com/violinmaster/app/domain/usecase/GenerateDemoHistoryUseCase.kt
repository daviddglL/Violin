package com.violinmaster.app.domain.usecase

import com.violinmaster.app.data.IPracticeRepository
import com.violinmaster.app.data.PracticeSession
import java.time.LocalDate
import kotlin.random.Random

/**
 * Generates demo practice history for testing and preview.
 *
 * Clears existing sessions, creates random sessions for the past 7 days,
 * and marks two preset lessons as completed.
 *
 * No Android imports — delegating to [IPracticeRepository].
 */
class GenerateDemoHistoryUseCase constructor(
    private val repository: IPracticeRepository
) {
    /**
     * Populates the database with realistic demo data.
     */
    suspend operator fun invoke() {
        repository.clearSessions()

        val today = LocalDate.now()
        val categories = listOf(
            "Smart Tuner Tuning",
            "Advanced Bowing: Détaché & Martelé",
            "Open Strings Tuning & Bowing",
            "Metronome Scale Practice"
        )

        for (dayOffset in -6..0) {
            val dayDate = today.plusDays(dayOffset.toLong())
            val dayString = dayDate.toString()

            val logsForDayCount = Random.nextInt(1, 3)
            for (i in 0 until logsForDayCount) {
                val practiceMin = Random.nextInt(10, 40)
                val randCategory = categories.shuffled().first()
                repository.insertSession(
                    PracticeSession(
                        dateString = dayString,
                        durationSeconds = practiceMin * 60,
                        category = randCategory,
                        timestamp = dayDate.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 + (i * 3600 * 1000)
                    )
                )
            }
        }

        val beg1 = repository.getLessonProgressById("beg_1")
        val adv1 = repository.getLessonProgressById("adv_1")

        if (beg1 != null) {
            repository.insertLessonProgress(
                beg1.copy(
                    totalPracticedSeconds = 900,
                    completed = true,
                    lastPracticedTimestamp = System.currentTimeMillis()
                )
            )
        }
        if (adv1 != null) {
            repository.insertLessonProgress(
                adv1.copy(
                    totalPracticedSeconds = 480,
                    completed = true,
                    lastPracticedTimestamp = System.currentTimeMillis() - 86400000
                )
            )
        }
    }
}
