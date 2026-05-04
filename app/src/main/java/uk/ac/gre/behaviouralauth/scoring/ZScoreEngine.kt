package uk.ac.gre.behaviouralauth.scoring

import kotlin.math.abs
import kotlin.math.roundToInt
import uk.ac.gre.behaviouralauth.capture.FeatureVector
import uk.ac.gre.behaviouralauth.model.AnomalyFactor

//Scores new behavioural data against an existing baseline profile using z-scores.
class ZScoreEngine(private val profile: BaselineProfile) {

    //Stores the final trust score, anomaly details, and suspicious-session result.
    data class ScoringResult(
        val trustScore: Int,
        val meanZScore: Double,
        val confidence: Double,
        val topAnomalies: List<AnomalyFactor>,
        val isSuspicious: Boolean
    )

    //Compares the observed feature vector with the baseline profile.
    fun score(
        vector: FeatureVector,
        zScoreMultiplier: Double = 1.0
    ): ScoringResult {
        val zScores = Tier1FeatureKeys.ALL.mapNotNull { key ->
            val observed = extractFeature(vector, key)
            val baselineMean = profile.featureMeans[key] ?: 0.0

            //Skips features where both observed and baseline values are missing.
            if (observed == 0.0 && baselineMean == 0.0) {
                null
            } else {
                val baselineStd = (profile.featureStds[key] ?: 0.001).coerceAtLeast(0.001)
                val zScore = (abs(observed - baselineMean) / baselineStd) * zScoreMultiplier
                AnomalyFactor(featureName = key, zScore = zScore)
            }
        }

        //Calculates the average z-score across all usable features.
        val meanZScore = if (zScores.isNotEmpty()) {
            zScores.sumOf { it.zScore } / zScores.size.toDouble()
        } else {
            0.0
        }

        //Converts the mean anomaly level into a trust score from 0 to 100.
        val trustScore = (100.0 - (meanZScore * 20.0))
            .coerceIn(0.0, 100.0)
            .roundToInt()

        return ScoringResult(
            trustScore = trustScore,
            meanZScore = meanZScore,
            confidence = vector.confidence,
            topAnomalies = zScores.sortedByDescending { it.zScore }.take(3),
            isSuspicious = trustScore < 50
        )
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