package com.violinmaster.app.data

/**
 * Central registry of all analytics event names and parameter keys.
 *
 * REQ-ANALYTICS-004: Standardized event naming.
 * REQ-ANALYTICS-005: Consistent parameter keys across all events.
 */
object AnalyticsEvents {

    // ── Event names ─────────────────────────────────────────────────────
    const val EVENT_PRACTICE_STARTED = "practice_started"
    const val EVENT_PRACTICE_ENDED = "practice_ended"
    const val EVENT_LESSON_COMPLETED = "lesson_completed"
    const val EVENT_ASSIGNMENT_CREATED = "assignment_created"
    const val EVENT_ASSIGNMENT_COMPLETED = "assignment_completed"
    const val EVENT_TUNER_OPENED = "tuner_opened"
    const val EVENT_METRONOME_OPENED = "metronome_opened"
    const val EVENT_PIN_LOGIN = "pin_login"
    const val EVENT_PIN_RECOVERY_USED = "pin_recovery_used"

    // ── Parameter keys ──────────────────────────────────────────────────
    const val PARAM_ROLE = "user_role"
    const val PARAM_SKILL_LEVEL = "skill_level"
    const val PARAM_INSTRUMENT = "instrument"
    const val PARAM_DURATION_SECONDS = "duration_seconds"
    const val PARAM_CATEGORY = "practice_category"
    const val PARAM_LESSON_ID = "lesson_id"
    const val PARAM_LESSON_LEVEL = "lesson_level"
    const val PARAM_ASSIGNMENT_ID = "assignment_id"
    const val PARAM_SCREEN_NAME = "screen_name"

    // ── User property keys ──────────────────────────────────────────────
    const val USER_PROP_ROLE = "user_role"
    const val USER_PROP_SKILL_LEVEL = "skill_level"
    const val USER_PROP_INSTRUMENT = "instrument"
}
