package uk.ac.gre.behaviouralauth.scoring

import kotlin.math.sqrt
import uk.ac.gre.behaviouralauth.capture.FeatureVector

private const val MIN_BASELINE_STD = 0.001
private const val ACCESSIBILITY_STD_MULTIPLIER = 1.5

/**
 * Builds a baseline from high-confidence enrollment windows.
 */
class BaselineBuilder {
    private val vectors = mutableListOf<FeatureVector>()
    private var isAccessibilityMode = false

    fun setAccessibilityMode(enabled: Boolean) {
        isAccessibilityMode = enabled
    }

    fun addVector(vector: FeatureVector) {
        if (vector.confidence >= 0.5) {
            vectors.add(vector)
        }
    }

    fun vectorCount(): Int = vectors.size

    fun canBuildProfile(): Boolean = vectors.size >= 3

    fun build(): BaselineProfile {
        require(canBuildProfile()) {
            "At least 3 high-confidence feature vectors are required to build a baseline."
        }

        val means = Tier1FeatureKeys.ALL.associateWith { key ->
            vectors.map { vector -> extractFeature(vector, key) }.meanOrZero()
        }

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

    fun reset() {
        vectors.clear()
    }

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

private fun List<Double>.meanOrZero(): Double {
    if (isEmpty()) return 0.0
    return sum() / size.toDouble()
}

private fun List<Double>.populationStdOrZero(): Double {
    if (size < 2) return 0.0
    val mean = meanOrZero()
    val variance = sumOf { value ->
        val difference = value - mean
        difference * difference
    } / size.toDouble()

    return sqrt(variance)
}

