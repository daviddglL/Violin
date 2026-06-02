package com.violinmaster.app.data.mapper

import com.violinmaster.app.data.PracticeSession as PracticeSessionEntity
import com.violinmaster.app.domain.model.PracticeSession

/**
 * Mapper between Room [PracticeSessionEntity] and domain [PracticeSession] model.
 */
object PracticeSessionMapper {

    fun toDomain(entity: PracticeSessionEntity): PracticeSession = PracticeSession(
        id = entity.id,
        dateString = entity.dateString,
        durationSeconds = entity.durationSeconds,
        category = entity.category,
        timestamp = entity.timestamp
    )

    fun toEntity(domain: PracticeSession): PracticeSessionEntity = PracticeSessionEntity(
        id = domain.id,
        dateString = domain.dateString,
        durationSeconds = domain.durationSeconds,
        category = domain.category,
        timestamp = domain.timestamp
    )
}
