package uk.ac.gre.behaviouralauth.capture

//Holds the calculated behaviour features for one time window.
data class FeatureVector(
    val windowStartMs: Long,
    val windowEndMs: Long,

    //Basic activity counts used to judge how much behaviour was captured.
    val keystrokeCount: Int,
    val gestureCount: Int,
    val confidence: Double,

    //Typing features based on timing and correction behaviour.
    val meanInterKeyInterval: Double,
    val stdInterKeyInterval: Double,
    val deleteRatio: Double,
    val typingSpeed: Double,

    //Swipe features based on movement speed, time, and distance.
    val meanSwipeVelocity: Double,
    val stdSwipeVelocity: Double,
    val meanSwipeDuration: Double,
    val meanSwipeDistance: Double,
    val medianInterKeyInterval: Double,
    val stdSwipeDuration: Double,
    val stdSwipeDistance: Double
) {
    companion object {

        //Creates a blank feature vector when there is not enough activity in the window.
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