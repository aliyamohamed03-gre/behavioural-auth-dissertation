package uk.ac.gre.behaviouralauth.capture

/**
 * Represents all behavioural features extracted from one closed window.
 *
 * Field names match the later CSV export schema exactly, so they should not be
 * renamed casually.
 */
data class FeatureVector(
    val windowStartMs: Long,
    val windowEndMs: Long,
    val keystrokeCount: Int,
    val gestureCount: Int,
    val confidence: Double,
    val meanInterKeyInterval: Double,
    val stdInterKeyInterval: Double,
    val deleteRatio: Double,
    val typingSpeed: Double,
    val meanSwipeVelocity: Double,
    val stdSwipeVelocity: Double,
    val meanSwipeDuration: Double,
    val meanSwipeDistance: Double,
    val medianInterKeyInterval: Double,
    val stdSwipeDuration: Double,
    val stdSwipeDistance: Double
) {
    companion object {
        /**
         * Returns a zero-filled feature vector.
         *
         * This is useful when a window closes without enough data or when tests
         * need a known empty baseline object.
         */
        fun empty(startMs: Long, endMs: Long): FeatureVector {
            return FeatureVector(
                windowStartMs = startMs,
                windowEndMs = endMs,
                keystrokeCount = 0,
                gestureCount = 0,
                confidence = 0.0,
                meanInterKeyInterval = 0.0,
                stdInterKeyInterval = 0.0,
                deleteRatio = 0.0,
                typingSpeed = 0.0,
                meanSwipeVelocity = 0.0,
                stdSwipeVelocity = 0.0,
                meanSwipeDuration = 0.0,
                meanSwipeDistance = 0.0,
                medianInterKeyInterval = 0.0,
                stdSwipeDuration = 0.0,
                stdSwipeDistance = 0.0
            )
        }
    }
}

