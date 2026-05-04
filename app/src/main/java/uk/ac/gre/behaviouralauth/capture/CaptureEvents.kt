package uk.ac.gre.behaviouralauth.capture

import kotlin.math.sqrt

//Base type for all behavioural data captured by the app.
sealed class BehaviouralEvent {
    abstract val timestampMs: Long
}

//Stores a typing change, such as adding or deleting characters.
data class KeystrokeEvent(
    override val timestampMs: Long,
    val previousLength: Int,
    val newLength: Int,
    val wasDeletion: Boolean
) : BehaviouralEvent()

//Stores the main details of a swipe gesture.
data class SwipeEvent(
    val startTimestampMs: Long,
    val endTimestampMs: Long,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
) : BehaviouralEvent() {

    //Uses the end time as the event timestamp because the full swipe is known by then.
    override val timestampMs: Long
        get() = endTimestampMs

    val durationMs: Long
        get() = endTimestampMs - startTimestampMs

    //Calculates the straight-line distance between the start and end of the swipe.
    val distancePx: Float
        get() = sqrt(
            (endX - startX) * (endX - startX) +
                    (endY - startY) * (endY - startY)
        )

    //Converts swipe speed into pixels per second.
    val velocityPxPerSec: Float
        get() = if (durationMs > 0) distancePx * 1000f / durationMs else 0f
}