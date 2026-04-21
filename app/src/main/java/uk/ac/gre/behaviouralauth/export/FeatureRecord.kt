package uk.ac.gre.behaviouralauth.export

import uk.ac.gre.behaviouralauth.capture.FeatureVector

data class FeatureRecord(
    val userId: String,
    val sessionId: String,
    val sessionType: String,
    val windowStartMs: Long,
    val windowEndMs: Long,
    val keystrokeCount: Int,
    val gestureCount: Int,
    val confidence: Double,
    val meanInterKeyInterval: Double,
    val stdInterKeyInterval: Double,
    val medianInterKeyInterval: Double,
    val deleteRatio: Double,
    val typingSpeed: Double,
    val meanSwipeVelocity: Double,
    val stdSwipeVelocity: Double,
    val meanSwipeDuration: Double,
    val meanSwipeDistance: Double,
    val stdSwipeDuration: Double,
    val stdSwipeDistance: Double
) {
    companion object {
        fun fromFeatureVector(
            vector: FeatureVector,
            userId: String,
            sessionId: String,
            sessionType: String
        ): FeatureRecord {
            return FeatureRecord(
                userId = userId,
                sessionId = sessionId,
                sessionType = sessionType,
                windowStartMs = vector.windowStartMs,
                windowEndMs = vector.windowEndMs,
                keystrokeCount = vector.keystrokeCount,
                gestureCount = vector.gestureCount,
                confidence = vector.confidence,
                meanInterKeyInterval = vector.meanInterKeyInterval,
                stdInterKeyInterval = vector.stdInterKeyInterval,
                medianInterKeyInterval = vector.medianInterKeyInterval,
                deleteRatio = vector.deleteRatio,
                typingSpeed = vector.typingSpeed,
                meanSwipeVelocity = vector.meanSwipeVelocity,
                stdSwipeVelocity = vector.stdSwipeVelocity,
                meanSwipeDuration = vector.meanSwipeDuration,
                meanSwipeDistance = vector.meanSwipeDistance,
                stdSwipeDuration = vector.stdSwipeDuration,
                stdSwipeDistance = vector.stdSwipeDistance
            )
        }
    }
}
