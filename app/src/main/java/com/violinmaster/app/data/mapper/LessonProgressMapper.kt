package com.violinmaster.app.data.mapper

import com.violinmaster.app.data.LessonProgress
import com.violinmaster.app.domain.model.Lesson

/**
 * Mapper between Room [LessonProgress] entity and domain [Lesson] model.
 */
object LessonProgressMapper {

    fun toDomain(entity: LessonProgress): Lesson = Lesson(
        lessonId = entity.lessonId,
        title = entity.lessonTitle,
        difficulty = entity.difficulty,
        completed = entity.completed,
        totalPracticedSeconds = entity.totalPracticedSeconds
    )

    fun toEntity(domain: Lesson): LessonProgress = LessonProgress(
        lessonId = domain.lessonId,
        lessonTitle = domain.title,
        difficulty = domain.difficulty,
        completed = domain.completed,
        totalPracticedSeconds = domain.totalPracticedSeconds
    )
}
