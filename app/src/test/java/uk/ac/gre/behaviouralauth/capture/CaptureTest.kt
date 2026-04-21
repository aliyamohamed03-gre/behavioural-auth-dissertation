package uk.ac.gre.behaviouralauth.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureTest {
    @Test
    fun testInterKeyIntervalCalculation() {
        val window = FeatureWindow()
        addKeystrokes(window, listOf(0L, 100L, 250L, 400L, 550L))

        val features = window.computeFeatures(windowEndMs = 550L)

        assertEquals(137.5, features.meanInterKeyInterval, 0.01)
    }

    @Test
    fun testStdInterKeyIntervalCalculation() {
        val window = FeatureWindow()
        addKeystrokes(window, listOf(0L, 100L, 250L, 400L, 550L))

        val features = window.computeFeatures(windowEndMs = 550L)

        assertEquals(21.65, features.stdInterKeyInterval, 0.05)
    }

    @Test
    fun testMedianInterKeyInterval() {
        val window = FeatureWindow()
        addKeystrokes(window, listOf(0L, 100L, 250L, 400L, 550L))

        val features = window.computeFeatures(windowEndMs = 550L)

        assertEquals(150.0, features.medianInterKeyInterval, 0.0)
    }

    @Test
    fun testDeleteRatio() {
        val window = FeatureWindow()

        repeat(7) { index ->
            window.addKeystroke(
                keystroke(timestampMs = index * 100L, wasDeletion = false)
            )
        }

        repeat(3) { index ->
            window.addKeystroke(
                keystroke(timestampMs = (700L + index * 100L), wasDeletion = true)
            )
        }

        val features = window.computeFeatures(windowEndMs = 1000L)

        assertEquals(0.3, features.deleteRatio, 0.0001)
    }

    @Test
    fun testTypingSpeed() {
        val window = FeatureWindow()

        repeat(10) { index ->
            window.addKeystroke(
                keystroke(timestampMs = index * 500L, wasDeletion = false)
            )
        }

        val features = window.computeFeatures(windowEndMs = 5000L)

        assertEquals(2.0, features.typingSpeed, 0.0001)
    }

    @Test
    fun testWindowClosesAt20Keystrokes() {
        val closedWindows = mutableListOf<FeatureVector>()
        val controller = FeatureWindowController(onWindowClosed = { closedWindows += it })

        repeat(20) { index ->
            controller.submitKeystroke(
                keystroke(timestampMs = index * 100L, wasDeletion = false)
            )
        }

        assertEquals(1, closedWindows.size)
    }

    @Test
    fun testWindowClosesAt5Swipes() {
        val closedWindows = mutableListOf<FeatureVector>()
        val controller = FeatureWindowController(onWindowClosed = { closedWindows += it })

        repeat(5) { index ->
            controller.submitSwipe(
                swipe(
                    startTimestampMs = index * 1000L,
                    endTimestampMs = index * 1000L + 100L
                )
            )
        }

        assertEquals(1, closedWindows.size)
    }

    @Test
    fun testWindowClosesOn30SecondsWithEnoughEvents() {
        val closedWindows = mutableListOf<FeatureVector>()
        val controller = FeatureWindowController(onWindowClosed = { closedWindows += it })

        addKeystrokes(controller, listOf(0L, 250L, 500L, 750L, 1000L))
        controller.checkForTimeBasedClose(currentMs = 31_000L)

        assertEquals(1, closedWindows.size)
    }

    @Test
    fun testWindowDoesNotCloseOn30SecondsWithFewerThan5Events() {
        val closedWindows = mutableListOf<FeatureVector>()
        val controller = FeatureWindowController(onWindowClosed = { closedWindows += it })

        addKeystrokes(controller, listOf(0L, 250L, 500L))
        controller.checkForTimeBasedClose(currentMs = 31_000L)

        assertEquals(0, closedWindows.size)
    }

    @Test
    fun testWindowClosesOnInactivity() {
        val closedWindows = mutableListOf<FeatureVector>()
        val controller = FeatureWindowController(onWindowClosed = { closedWindows += it })

        addKeystrokes(controller, listOf(0L, 250L, 500L, 750L, 1000L))
        controller.checkForTimeBasedClose(currentMs = 11_500L)

        assertEquals(1, closedWindows.size)
    }

    @Test
    fun testConfidenceWeighting() {
        val controller = FeatureWindowController(onWindowClosed = {})

        repeat(10) { index ->
            controller.submitKeystroke(
                keystroke(timestampMs = index * 100L, wasDeletion = false)
            )
        }
        assertEquals(0.5, controller.snapshot().confidence, 0.0001)

        repeat(10) { index ->
            controller.submitKeystroke(
                keystroke(timestampMs = (1000L + index * 100L), wasDeletion = false)
            )
        }
        assertEquals(1.0, controller.snapshot().confidence, 0.0001)

        repeat(5) { index ->
            controller.submitKeystroke(
                keystroke(timestampMs = (2000L + index * 100L), wasDeletion = false)
            )
        }
        assertEquals(0.25, controller.snapshot().confidence, 0.0001)
    }

    @Test
    fun testEmptyWindowFeatures() {
        val window = FeatureWindow()

        val features = window.computeFeatures(windowEndMs = 0L)

        assertEquals(0, features.keystrokeCount)
        assertEquals(0, features.gestureCount)
        assertEquals(0.0, features.confidence, 0.0)
        assertEquals(0.0, features.meanInterKeyInterval, 0.0)
        assertEquals(0.0, features.stdInterKeyInterval, 0.0)
        assertEquals(0.0, features.deleteRatio, 0.0)
        assertEquals(0.0, features.typingSpeed, 0.0)
        assertEquals(0.0, features.meanSwipeVelocity, 0.0)
        assertEquals(0.0, features.stdSwipeVelocity, 0.0)
        assertEquals(0.0, features.meanSwipeDuration, 0.0)
        assertEquals(0.0, features.meanSwipeDistance, 0.0)
        assertEquals(0.0, features.medianInterKeyInterval, 0.0)
        assertEquals(0.0, features.stdSwipeDuration, 0.0)
        assertEquals(0.0, features.stdSwipeDistance, 0.0)
    }

    private fun addKeystrokes(window: FeatureWindow, timestamps: List<Long>) {
        timestamps.forEach { timestamp ->
            window.addKeystroke(keystroke(timestampMs = timestamp, wasDeletion = false))
        }
    }

    private fun addKeystrokes(controller: FeatureWindowController, timestamps: List<Long>) {
        timestamps.forEach { timestamp ->
            controller.submitKeystroke(keystroke(timestampMs = timestamp, wasDeletion = false))
        }
    }

    private fun keystroke(timestampMs: Long, wasDeletion: Boolean): KeystrokeEvent {
        return if (wasDeletion) {
            KeystrokeEvent(
                timestampMs = timestampMs,
                previousLength = 5,
                newLength = 4,
                wasDeletion = true
            )
        } else {
            KeystrokeEvent(
                timestampMs = timestampMs,
                previousLength = 4,
                newLength = 5,
                wasDeletion = false
            )
        }
    }

    private fun swipe(startTimestampMs: Long, endTimestampMs: Long): SwipeEvent {
        return SwipeEvent(
            startTimestampMs = startTimestampMs,
            endTimestampMs = endTimestampMs,
            startX = 0f,
            startY = 0f,
            endX = 100f,
            endY = 0f
        )
    }
}
