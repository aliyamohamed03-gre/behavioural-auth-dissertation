package uk.ac.gre.behaviouralauth.scoring

/**
 * Stores the behavioural baseline created during enrollment.
 *
 * The means describe the user's typical behaviour. The standard deviations
 * describe how much natural variation is expected for each feature.
 */
data class BaselineProfile(
    val featureMeans: Map<String, Double>,
    val featureStds: Map<String, Double>,
    val sessionCount: Int,
    val isAccessibilityProfile: Boolean,
    val createdTimestampMs: Long
)

object Tier1FeatureKeys {
    val ALL = listOf(
        "meanInterKeyInterval",
        "stdInterKeyInterval",
        "deleteRatio",
        "typingSpeed",
        "meanSwipeVelocity",
        "stdSwipeVelocity",
        "meanSwipeDuration",
        "meanSwipeDistance"
    )
}

