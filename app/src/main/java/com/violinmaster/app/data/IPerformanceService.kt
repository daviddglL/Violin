package com.violinmaster.app.data

/**
 * Interface for performance monitoring, enabling test doubles and abstracting
 * Firebase Performance Monitoring from the rest of the app.
 *
 * REQ-PERF-001: Custom trace creation for critical operations.
 * REQ-PERF-002: Metric collection (duration, success/failure counts).
 */
interface IPerformanceService {
    /**
     * Starts a custom trace with the given name.
     * Call [stopTrace] when the operation completes.
     *
     * @param name Unique trace name (e.g., "video_pipeline", "gemini_api_call").
     * @return A trace handle for stopping and adding metrics.
     */
    fun startTrace(name: String): TraceHandle

    /**
     * Increments a counter metric on a trace.
     *
     * @param traceName The trace to add the metric to.
     * @param metricName Name of the counter (e.g., "success_count", "error_count").
     * @param incrementBy Amount to increment (default 1).
     */
    fun incrementMetric(traceName: String, metricName: String, incrementBy: Long = 1)
}

/**
 * Handle returned by [IPerformanceService.startTrace].
 * Call [stop] when the traced operation completes.
 */
interface TraceHandle {
    /** Stops the trace and records the duration. */
    fun stop()

    /** Records an attribute key-value pair on the trace. */
    fun putAttribute(key: String, value: String)

    /** Increments a counter metric on this trace. */
    fun incrementMetric(metricName: String, incrementBy: Long)
}
