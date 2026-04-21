package uk.ac.gre.behaviouralauth.capture

import kotlin.math.sqrt

private const val KEYSTROKE_CLOSE_THRESHOLD = 20
private const val SWIPE_CLOSE_THRESHOLD = 5
private const val MIN_EVENTS_FOR_TIME_CLOSE = 5
private const val TIME_CLOSE_THRESHOLD_MS = 30_000L
private const val INACTIVITY_CLOSE_THRESHOLD_MS = 10_000L
private const val FULL_CONFIDENCE_EVENT_COUNT = 20.0

/**
 * Collects behavioural events until a window is ready to be closed and scored.
 *
 * This class is deliberately self-contained so it can be tested without any UI
 * dependency and later reused by both enrollment and authentication flows.
 */
class FeatureWindow {
    private val keystrokes = mutableListOf<KeystrokeEvent>()
    private val swipes = mutableListOf<SwipeEvent>()
    private var firstEventMs: Long? = null

    val keystrokeCount: Int
        get() = keystrokes.size

    val swipeCount: Int
        get() = swipes.size

    val totalEventCount: Int
        get() = keystrokeCount + swipeCount

    val isEmpty: Boolean
        get() = totalEventCount == 0

    val firstEventTimestamp: Long?
        get() = firstEventMs

    fun addKeystroke(event: KeystrokeEvent) {
        keystrokes += event
        firstEventMs = minTimestamp(firstEventMs, event.timestampMs)
    }

    fun addSwipe(event: SwipeEvent) {
        swipes += event
        firstEventMs = minTimestamp(firstEventMs, event.startTimestampMs)
    }

    /**
     * Closes on:
     * - 20+ keystrokes
     * - 5+ swipes
     * - 30+ seconds elapsed from the first event when at least 5 events exist
     */
    fun shouldCloseDueToVolumeOrTime(currentMs: Long): Boolean {
        if (keystrokeCount >= KEYSTROKE_CLOSE_THRESHOLD) {
            return true
        }

        if (swipeCount >= SWIPE_CLOSE_THRESHOLD) {
            return true
        }

        val startMs = firstEventMs ?: return false
        return totalEventCount >= MIN_EVENTS_FOR_TIME_CLOSE &&
            currentMs - startMs >= TIME_CLOSE_THRESHOLD_MS
    }

    /**
     * Closes if the user has been inactive for 10+ seconds and the window has
     * at least 5 total events.
     */
    fun shouldCloseDueToInactivity(lastEventMs: Long, currentMs: Long): Boolean {
        return totalEventCount >= MIN_EVENTS_FOR_TIME_CLOSE &&
            currentMs - lastEventMs >= INACTIVITY_CLOSE_THRESHOLD_MS
    }

    fun lastEventTimestamp(): Long? {
        val lastKeystroke = keystrokes.lastOrNull()?.timestampMs
        val lastSwipe = swipes.lastOrNull()?.timestampMs

        return listOfNotNull(lastKeystroke, lastSwipe).maxOrNull()
    }

    /**
     * Computes all window features from the accumulated events.
     *
     * Returning a FeatureVector instead of primitive values keeps the window
     * computation output stable and easy to pass through later repositories.
     */
    fun computeFeatures(windowEndMs: Long): FeatureVector {
        val startMs = firstEventMs ?: return FeatureVector.empty(windowEndMs, windowEndMs)

        val interKeyIntervals = keystrokes
            .zipWithNext { previous, current ->
                (current.timestampMs - previous.timestampMs).coerceAtLeast(0L).toDouble()
            }

        val swipeVelocities = swipes.map { it.velocityPxPerSec.toDouble().finiteOrZero() }
        val swipeDurations = swipes.map { it.durationMs.coerceAtLeast(0L).toDouble() }
        val swipeDistances = swipes.map { it.distancePx.toDouble().finiteOrZero() }
        val windowDurationSeconds = ((windowEndMs - startMs).coerceAtLeast(0L)) / 1000.0

        val typingEvents = keystrokes.count { !it.wasDeletion }

        return FeatureVector(
            windowStartMs = startMs,
            windowEndMs = windowEndMs,
            keystrokeCount = keystrokeCount,
            gestureCount = swipeCount,
            confidence = confidenceFromCount(totalEventCount),
            meanInterKeyInterval = interKeyIntervals.meanOrZero(),
            stdInterKeyInterval = interKeyIntervals.populationStdOrZero(),
            deleteRatio = if (keystrokeCount > 0) {
                keystrokes.count { it.wasDeletion }.toDouble() / keystrokeCount.toDouble()
            } else {
                0.0
            },
            typingSpeed = if (keystrokeCount > 0 && windowDurationSeconds > 0.0) {
                typingEvents.toDouble() / windowDurationSeconds
            } else {
                0.0
            },
            meanSwipeVelocity = swipeVelocities.meanOrZero(),
            stdSwipeVelocity = swipeVelocities.populationStdOrZero(),
            meanSwipeDuration = swipeDurations.meanOrZero(),
            meanSwipeDistance = swipeDistances.meanOrZero(),
            medianInterKeyInterval = interKeyIntervals.medianOrZero(),
            stdSwipeDuration = swipeDurations.populationStdOrZero(),
            stdSwipeDistance = swipeDistances.populationStdOrZero()
        ).sanitized()
    }

    /**
     * Clears the window so a new behavioural window can start fresh.
     */
    fun reset() {
        keystrokes.clear()
        swipes.clear()
        firstEventMs = null
    }

    companion object {
        fun confidenceFromCount(totalEventCount: Int): Double {
            return (totalEventCount / FULL_CONFIDENCE_EVENT_COUNT)
                .coerceIn(0.0, 1.0)
        }
    }
}

private fun minTimestamp(current: Long?, candidate: Long): Long {
    return current?.coerceAtMost(candidate) ?: candidate
}

private fun List<Double>.meanOrZero(): Double {
    if (isEmpty()) return 0.0
    return (sum() / size).finiteOrZero()
}

private fun List<Double>.populationStdOrZero(): Double {
    if (size < 2) return 0.0
    val mean = meanOrZero()
    val variance = sumOf { value ->
        val difference = value - mean
        difference * difference
    } / size.toDouble()

    return sqrt(variance).finiteOrZero()
}

private fun List<Double>.medianOrZero(): Double {
    if (isEmpty()) return 0.0
    val sorted = sorted()
    val middleIndex = sorted.size / 2

    return if (sorted.size % 2 == 0) {
        ((sorted[middleIndex - 1] + sorted[middleIndex]) / 2.0).finiteOrZero()
    } else {
        sorted[middleIndex].finiteOrZero()
    }
}

private fun Double.finiteOrZero(): Double {
    return if (isFinite()) this else 0.0
}

private fun FeatureVector.sanitized(): FeatureVector {
    return copy(
        confidence = confidence.finiteOrZero(),
        meanInterKeyInterval = meanInterKeyInterval.finiteOrZero(),
        stdInterKeyInterval = stdInterKeyInterval.finiteOrZero(),
        deleteRatio = deleteRatio.finiteOrZero(),
        typingSpeed = typingSpeed.finiteOrZero(),
        meanSwipeVelocity = meanSwipeVelocity.finiteOrZero(),
        stdSwipeVelocity = stdSwipeVelocity.finiteOrZero(),
        meanSwipeDuration = meanSwipeDuration.finiteOrZero(),
        meanSwipeDistance = meanSwipeDistance.finiteOrZero(),
        medianInterKeyInterval = medianInterKeyInterval.finiteOrZero(),
        stdSwipeDuration = stdSwipeDuration.finiteOrZero(),
        stdSwipeDistance = stdSwipeDistance.finiteOrZero()
    )
}

