package uk.ac.gre.behaviouralauth.capture

import kotlin.math.sqrt

/**
 * Common base type for all behavioural input events captured by the app.
 *
 * Each event exposes a single timestamp so windowing logic can order and
 * evaluate events consistently, even though different event types carry
 * different raw details.
 */
sealed class BehaviouralEvent {
    abstract val timestampMs: Long
}

/**
 * Captures a single text-length transition observed from user typing.
 *
 * The app intentionally models typing through text-length changes rather than
 * low-level key down/up callbacks because Android soft keyboards are not
 * reliable sources of those events.
 */
data class KeystrokeEvent(
    override val timestampMs: Long,
    val previousLength: Int,
    val newLength: Int,
    val wasDeletion: Boolean
) : BehaviouralEvent()

/**
 * Captures one swipe-like drag gesture from pointer down to pointer up.
 */
data class SwipeEvent(
    val startTimestampMs: Long,
    val endTimestampMs: Long,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
) : BehaviouralEvent() {
    override val timestampMs: Long
        get() = endTimestampMs

    val durationMs: Long
        get() = endTimestampMs - startTimestampMs

    val distancePx: Float
        get() = sqrt(
            (endX - startX) * (endX - startX) +
                (endY - startY) * (endY - startY)
        )

    val velocityPxPerSec: Float
        get() = if (durationMs > 0) distancePx * 1000f / durationMs else 0f
}

