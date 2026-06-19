package com.violinmaster.app.domain.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple circuit breaker for external API calls.
 *
 * Prevents cascading failures when an external service (e.g., Gemini API)
 * is unreachable or returning errors. After [failureThreshold] consecutive
 * failures, the circuit opens and all subsequent calls fail fast without
 * touching the network for [resetTimeoutMs] milliseconds.
 *
 * States:
 * - CLOSED: normal operation, requests pass through
 * - OPEN: failures exceeded threshold, all requests fail fast
 * - HALF_OPEN: after timeout, allows one probe request to test recovery
 *
 * Thread-safe via [Mutex].
 *
 * @param failureThreshold Number of consecutive failures before opening the circuit.
 * @param resetTimeoutMs Time in milliseconds before transitioning from OPEN to HALF_OPEN.
 */
class CircuitBreaker(
    private val failureThreshold: Int = 3,
    private val resetTimeoutMs: Long = 30_000L
) {
    private val mutex = Mutex()

    private var failureCount: Int = 0
    private var lastFailureTimeMs: Long = 0L
    private var state: State = State.CLOSED

    private enum class State { CLOSED, OPEN, HALF_OPEN }

    /**
     * Returns true if the circuit is closed (requests are allowed).
     * Call this BEFORE making the external API call.
     *
     * If the circuit was OPEN but the timeout has elapsed, transitions
     * to HALF_OPEN and allows exactly one probe request.
     */
    suspend fun allowRequest(): Boolean = mutex.withLock {
        when (state) {
            State.CLOSED -> true
            State.OPEN -> {
                val elapsed = System.currentTimeMillis() - lastFailureTimeMs
                if (elapsed >= resetTimeoutMs) {
                    state = State.HALF_OPEN
                    true // Allow one probe
                } else {
                    false
                }
            }
            State.HALF_OPEN -> false // Only one probe at a time
        }
    }

    /**
     * Records a successful request. Resets the circuit to CLOSED.
     */
    suspend fun recordSuccess() = mutex.withLock {
        failureCount = 0
        state = State.CLOSED
    }

    /**
     * Records a failed request. If failures reach the threshold, opens the circuit.
     */
    suspend fun recordFailure() = mutex.withLock {
        failureCount++
        lastFailureTimeMs = System.currentTimeMillis()
        if (failureCount >= failureThreshold) {
            state = State.OPEN
        }
    }

    /** Current state for observability (e.g., logging, metrics). */
    suspend fun currentState(): String = mutex.withLock { state.name }

    /** Number of consecutive failures. */
    suspend fun currentFailureCount(): Int = mutex.withLock { failureCount }
}
