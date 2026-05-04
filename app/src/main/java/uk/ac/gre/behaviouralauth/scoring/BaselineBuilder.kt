package uk.ac.gre.behaviouralauth.scoring

import kotlin.math.sqrt
import uk.ac.gre.behaviouralauth.capture.FeatureVector

private const val MIN_BASELINE_STD = 0.001
private const val ACCESSIBILITY_STD_MULTIPLIER = 1.5

//Builds a baseline behavioural profile from high-confidence feature vectors.
class BaselineBuilder {
    private val vectors = mutableListOf<FeatureVector>()
    private var isAccessibilityMode = false

    //Enables wider thresholds for users with accessibility needs.
    fun setAccessibilityMode(enabled: Boolean) {
        isAccessibilityMode = enabled
    }

    //Adds only reliable feature vectors to the baseline dataset.
    fun addVector(vector: FeatureVector) {
        if (vector.confidence >= 0.5) {
            vectors.add(vector)
        }
    }

    fun vectorCount(): Int = vectors.size

    //Checks whether enough vectors have been collected to build a profile.
    fun canBuildProfile(): Boolean = vectors.size >= 3

    //Builds the baseline profile using feature means and standard deviations.
    fun build(): BaselineProfile {
        require(canBuildProfile()) {
            "At least 3 high-confidence feature vectors are required to build a baseline."
        }

        //Calculates the average value for each Tier 1 feature.
        val means = Tier1FeatureKeys.ALL.associateWith { key ->
            vectors.map { vector -> extractFeature(vector, key) }.meanOrZero()
        }

        //Calculates feature variation and widens it if accessibility mode is enabled.
        val stds = Tier1FeatureKeys.ALL.associateWith { key ->
            val values = vectors.map { vector -> extractFeature(vector, key) }
            val baseStd = values.populationStdOrZero().coerceAtLeast(MIN_BASELINE_STD)
            if (isAccessibilityMode) {
                baseStd * ACCESSIBILITY_STD_MULTIPLIER
            } else {
                baseStd
            }
        }

        return BaselineProfile(
            featureMeans = means,
            featureStds = stds,
            sessionCount = vectors.size,
            isAccessibilityProfile = isAccessibilityMode,
            createdTimestampMs = System.currentTimeMillis()
        )
    }

    //Clears the collected vectors so a new baseline can be created.
    fun reset() {
        vectors.clear()
    }

    //Maps each feature key to its corresponding value in the feature vector.
    private fun extractFeature(vector: FeatureVector, key: String): Double {
        return when (key) {
            "meanInterKeyInterval" -> vector.meanInterKeyInterval
            "stdInterKeyInterval" -> vector.stdInterKeyInterval
            "deleteRatio" -> vector.deleteRatio
            "typingSpeed" -> vector.typingSpeed
            "meanSwipeVelocity" -> vector.meanSwipeVelocity
            "stdSwipeVelocity" -> vector.stdSwipeVelocity
            "meanSwipeDuration" -> vector.meanSwipeDuration
            "meanSwipeDistance" -> vector.meanSwipeDistance
            else -> 0.0
        }
    }
}

//Returns the mean value or 0.0 if the list is empty.
private fun List<Double>.meanOrZero(): Double {
    if (isEmpty()) return 0.0
    return sum() / size.toDouble()
}

//Returns the population standard deviation or 0.0 if there are too few values.
private fun List<Double>.populationStdOrZero(): Double {
    if (size < 2) return 0.0
    val mean = meanOrZero()
    val variance = sumOf { value ->
        val difference = value - mean
        difference * difference
    } / size.toDouble()

    return sqrt(variance)
}