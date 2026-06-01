package com.violinmaster.app.data.mapper

import com.violinmaster.app.data.Assignment as AssignmentEntity
import com.violinmaster.app.domain.model.Assignment

/**
 * Mapper between Room [AssignmentEntity] and domain [Assignment] model.
 */
object AssignmentMapper {

    fun toDomain(entity: AssignmentEntity): Assignment = Assignment(
        id = entity.id,
        title = entity.title,
        description = entity.description,
        teacherUsername = entity.teacherUsername,
        studentUsername = entity.studentUsername,
        completed = entity.completed,
        timestamp = entity.timestamp
    )

    fun toEntity(domain: Assignment): AssignmentEntity = AssignmentEntity(
        id = domain.id,
        title = domain.title,
        description = domain.description,
        teacherUsername = domain.teacherUsername,
        studentUsername = domain.studentUsername,
        completed = domain.completed,
        timestamp = domain.timestamp
    )
}
