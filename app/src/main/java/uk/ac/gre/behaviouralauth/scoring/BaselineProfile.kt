package uk.ac.gre.behaviouralauth.scoring

//Stores the user's baseline behavioural profile for authentication scoring
data class BaselineProfile(
    val featureMeans: Map<String, Double>,
    val featureStds: Map<String, Double>,
    val sessionCount: Int,
    val isAccessibilityProfile: Boolean,
    val createdTimestampMs: Long
)
//Defines the Tier 1 behavioural features used to build and score profiles
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

