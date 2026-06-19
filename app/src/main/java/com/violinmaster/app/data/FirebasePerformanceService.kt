package com.violinmaster.app.data

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Performance Monitoring implementation of [IPerformanceService].
 *
 * Wraps [FirebasePerformance] for custom trace creation and metric collection.
 * The Firebase Perf Gradle plugin also auto-instruments:
 * - App start time
 * - Screen rendering (Activity/Fragment lifecycle)
 * - OkHttp network requests (bytecode instrumentation)
 *
 * REQ-PERF-001: Custom trace creation via [FirebasePerformance.startTrace].
 * REQ-PERF-002: Metric collection via [Trace.incrementMetric].
 */
@Singleton
class FirebasePerformanceService @Inject constructor() : IPerformanceService {

    override fun startTrace(name: String): TraceHandle {
        val trace = FirebasePerformance.getInstance().newTrace(name)
        trace.start()
        return FirebaseTraceHandle(trace)
    }

    override fun incrementMetric(traceName: String, metricName: String, incrementBy: Long) {
        val trace = FirebasePerformance.getInstance().newTrace(traceName)
        trace.incrementMetric(metricName, incrementBy)
    }
}

/**
 * [TraceHandle] implementation wrapping a Firebase [Trace].
 */
private class FirebaseTraceHandle(
    private val trace: Trace
) : TraceHandle {
    override fun stop() {
        trace.stop()
    }

    override fun putAttribute(key: String, value: String) {
        trace.putAttribute(key, value)
    }

    override fun incrementMetric(metricName: String, incrementBy: Long) {
        trace.incrementMetric(metricName, incrementBy)
    }
}
