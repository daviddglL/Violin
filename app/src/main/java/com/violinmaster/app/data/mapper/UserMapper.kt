package com.violinmaster.app.data.mapper

import com.violinmaster.app.data.UserAccount
import com.violinmaster.app.domain.model.User

/**
 * Mapper between Room [UserAccount] entity and domain [User] model.
 *
 * REQ-ARCH-001: Data layer entities map to pure domain models.
 */
object UserMapper {

    /** Maps a Room entity to a pure domain model (strips auth fields). */
    fun toDomain(entity: UserAccount): User = User(
        username = entity.username,
        role = entity.role,
        skillLevel = entity.skillLevel,
        points = entity.points,
        teacherCode = entity.teacherCode,
        birthYear = entity.birthYear
    )

    /**
     * Maps a domain model to a Room entity.
     * Auth fields (hashedPassword, salt) are set to empty strings since domain
     * models don't carry authentication data.
     */
    fun toEntity(domain: User): UserAccount = UserAccount(
        username = domain.username,
        role = domain.role,
        hashedPassword = "",
        salt = "",
        teacherCode = domain.teacherCode,
        points = domain.points,
        skillLevel = domain.skillLevel,
        birthYear = domain.birthYear
    )
}
