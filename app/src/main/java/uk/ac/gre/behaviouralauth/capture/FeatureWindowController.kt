package uk.ac.gre.behaviouralauth.capture

/**
 * Coordinates one active FeatureWindow and emits completed FeatureVectors.
 *
 * The controller is the piece that the ViewModel can later own, while the
 * FeatureWindow remains a focused statistics engine.
 */
class FeatureWindowController(
    private val onWindowClosed: (FeatureVector) -> Unit
) {
    private val window = FeatureWindow()

    /**
     * The snapshot is retained after a window closes so the UI can still show
     * the most recent window summary until the next window starts accumulating.
     */
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

    /**
     * Called by a periodic timer to close windows that have gone stale due to
     * elapsed time or inactivity.
     */
    fun checkForTimeBasedClose(currentMs: Long) {
        if (window.isEmpty) {
            return
        }

        val lastEventMs = window.lastEventTimestamp() ?: return

        if (
            window.shouldCloseDueToVolumeOrTime(currentMs) ||
            window.shouldCloseDueToInactivity(lastEventMs, currentMs)
        ) {
            closeWindow(windowEndMs = currentMs)
        }
    }

    fun snapshot(): WindowSnapshot {
        return if (window.isEmpty) latestSnapshot else snapshotFromWindow()
    }

    private fun closeWindowIfNeeded(currentMs: Long) {
        if (window.shouldCloseDueToVolumeOrTime(currentMs)) {
            closeWindow(windowEndMs = currentMs)
        }
    }

    private fun closeWindow(windowEndMs: Long) {
        val features = window.computeFeatures(windowEndMs)
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
        return WindowSnapshot(
            keystrokeCount = window.keystrokeCount,
            swipeCount = window.swipeCount,
            totalEventCount = window.totalEventCount,
            confidence = FeatureWindow.confidenceFromCount(window.totalEventCount),
            firstEventTimestamp = window.firstEventTimestamp
        )
    }
}

data class WindowSnapshot(
    val keystrokeCount: Int,
    val swipeCount: Int,
    val totalEventCount: Int,
    val confidence: Double,
    val firstEventTimestamp: Long?
)
