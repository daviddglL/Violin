package com.violinmaster.app.domain.model

/**
 * Pre-defined security questions for PIN recovery (REQ-PINREC-001).
 *
 * Each enum constant carries a localization key used to display the question
 * in the user's chosen language.
 */
enum class RecoveryQuestion(val questionKey: String) {
    FIRST_PET("recovery_q_first_pet"),
    BIRTH_CITY("recovery_q_birth_city"),
    FAVORITE_TEACHER("recovery_q_favorite_teacher"),
    CHILDHOOD_NICKNAME("recovery_q_childhood_nickname"),
    FIRST_INSTRUMENT("recovery_q_first_instrument")
}
