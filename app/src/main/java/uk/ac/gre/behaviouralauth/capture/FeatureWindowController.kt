package uk.ac.gre.behaviouralauth.capture

class FeatureWindowController(
    private val onWindowClosed: (FeatureVector) -> Unit
) {
    //Holds the current group of behavioural events before they become features.
    private val window = FeatureWindow()

    //Keeps the latest window summary so the UI can still show recent activity.
    private var latestSnapshot = WindowSnapshot(
        keystrokeCount = 0,
        swipeCount = 0,
        totalEventCount = 0,
        confidence = 0.0,
        firstEventTimestamp = null
    )

    fun submitKeystroke(event: KeystrokeEvent) {
        window.addKeystroke(event)
        latestSnapshot = snapshotFromWindow()
        closeWindowIfNeeded(currentMs = event.timestampMs)
    }

    fun submitSwipe(event: SwipeEvent) {
        window.addSwipe(event)
        latestSnapshot = snapshotFromWindow()
        closeWindowIfNeeded(currentMs = event.timestampMs)
    }

    fun checkForTimeBasedClose(currentMs: Long) {
        //Nothing needs to be checked if no behaviour has been captured yet.
        if (window.isEmpty) {
            return
        }

        val lastEventMs = window.lastEventTimestamp() ?: return

        //Closes the window if it has enough data, has lasted too long, or the user is inactive.
        if (
            window.shouldCloseDueToVolumeOrTime(currentMs) ||
            window.shouldCloseDueToInactivity(lastEventMs, currentMs)
        ) {
            closeWindow(windowEndMs = currentMs)
        }
    }

    fun snapshot(): WindowSnapshot {
        //Returns a live snapshot when the window is active, otherwise the last saved one.
        return if (window.isEmpty) latestSnapshot else snapshotFromWindow()
    }

    private fun closeWindowIfNeeded(currentMs: Long) {
        //Used after each new event to decide whether the current window is complete.
        if (window.shouldCloseDueToVolumeOrTime(currentMs)) {
            closeWindow(windowEndMs = currentMs)
        }
    }

    private fun closeWindow(windowEndMs: Long) {
        val features = window.computeFeatures(windowEndMs)

        //Saves a summary before resetting, so the latest window can still be displayed.
        latestSnapshot = WindowSnapshot(
            keystrokeCount = features.keystrokeCount,
            swipeCount = features.gestureCount,
            totalEventCount = features.keystrokeCount + features.gestureCount,
            confidence = features.confidence,
            firstEventTimestamp = features.windowStartMs
        )

        onWindowClosed(features)
        window.reset()
    }

    private fun snapshotFromWindow(): WindowSnapshot {
        //Creates a lightweight summary of the current window without closing it.
        return WindowSnapshot(
            keystrokeCount = window.keystrokeCount,
            swipeCount = window.swipeCount,
            totalEventCount = window.totalEventCount,
            confidence = FeatureWindow.confidenceFromCount(window.totalEventCount),
            firstEventTimestamp = window.firstEventTimestamp
        )
    }
}

//Simple view of the current feature window, mainly useful for status display.
data class WindowSnapshot(
    val keystrokeCount: Int,
    val swipeCount: Int,
    val totalEventCount: Int,
    val confidence: Double,
    val firstEventTimestamp: Long?
)