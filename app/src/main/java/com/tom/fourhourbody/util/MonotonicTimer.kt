package com.tom.fourhourbody.util

import android.os.SystemClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/**
 * Timing is read from [SystemClock.elapsedRealtime] on every emission rather than accumulated
 * from a Handler loop, so the working-set stopwatch and the rest countdown stay accurate with
 * the screen off or the device locked — a wall-clock loop drifts, and deep sleep stops it dead.
 */
object MonotonicTimer {

    const val DEFAULT_PERIOD_MS = 50L

    fun now(): Long = SystemClock.elapsedRealtime()

    /** Milliseconds elapsed since [startRealtimeMs], emitted every [periodMs]. */
    fun elapsedFlow(
        startRealtimeMs: Long = now(),
        periodMs: Long = DEFAULT_PERIOD_MS
    ): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            emit(now() - startRealtimeMs)
            delay(periodMs)
        }
    }

    /**
     * Milliseconds remaining of [durationMs], clamped at zero and emitting a final zero so a
     * collector can complete on it.
     */
    fun countdownFlow(
        durationMs: Long,
        startRealtimeMs: Long = now(),
        periodMs: Long = DEFAULT_PERIOD_MS
    ): Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            val remaining = durationMs - (now() - startRealtimeMs)
            if (remaining <= 0L) {
                emit(0L)
                return@flow
            }
            emit(remaining)
            delay(minOf(periodMs, remaining))
        }
    }
}
