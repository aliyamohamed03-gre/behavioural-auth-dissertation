package uk.ac.gre.behaviouralauth.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.ac.gre.behaviouralauth.capture.FeatureVector

class ScoringTest {
    @Test
    fun testPerfectMatch() {
        val result = ZScoreEngine(profile(mean = 10.0, std = 2.0)).score(vector(value = 10.0))

        assertEquals(100, result.trustScore)
        assertFalse(result.isSuspicious)
    }

    @Test
    fun testModerateDev() {
        val result = ZScoreEngine(profile(mean = 10.0, std = 2.0)).score(vector(value = 12.0))

        assertEquals(80, result.trustScore)
    }

    @Test
    fun testHighDev() {
        val result = ZScoreEngine(profile(mean = 10.0, std = 2.0)).score(vector(value = 18.0))

        assertEquals(20, result.trustScore)
    }

    @Test
    fun testExtremeDev() {
        val result = ZScoreEngine(profile(mean = 10.0, std = 2.0)).score(vector(value = 24.0))

        assertEquals(0, result.trustScore)
        assertTrue(result.isSuspicious)
    }

    @Test
    fun testAccessibilityWiderTolerance() {
        val standardProfile = buildProfile(accessibilityMode = false)
        val accessibilityProfile = buildProfile(accessibilityMode = true)
        val slightlyDeviantVector = vector(value = 16.0)

        val standardScore = ZScoreEngine(standardProfile).score(slightlyDeviantVector).trustScore
        val accessibilityScore = ZScoreEngine(accessibilityProfile).score(slightlyDeviantVector).trustScore

        assertTrue(accessibilityScore > standardScore)
    }

    @Test
    fun testTopAnomaliesOrder() {
        val matchingValues = Tier1FeatureKeys.ALL.associateWith { 10.0 }.toMutableMap()
        matchingValues["typingSpeed"] = 30.0

        val result = ZScoreEngine(profile(mean = 10.0, std = 2.0)).score(
            vector(values = matchingValues)
        )

        assertEquals("typingSpeed", result.topAnomalies.first().featureName)
    }

    @Test
    fun testSkipsZeroFeatures() {
        val typingKeys = setOf(
            "meanInterKeyInterval",
            "stdInterKeyInterval",
            "deleteRatio",
            "typingSpeed"
        )
        val means = Tier1FeatureKeys.ALL.associateWith { key ->
            if (key in typingKeys) 10.0 else 0.0
        }
        val stds = Tier1FeatureKeys.ALL.associateWith { 2.0 }
        val observed = Tier1FeatureKeys.ALL.associateWith { key ->
            if (key in typingKeys) 12.0 else 0.0
        }

        val result = ZScoreEngine(
            BaselineProfile(
                featureMeans = means,
                featureStds = stds,
                sessionCount = 3,
                isAccessibilityProfile = false,
                createdTimestampMs = 0L
            )
        ).score(vector(values = observed))

        assertEquals(1.0, result.meanZScore, 0.0001)
        assertEquals(80, result.trustScore)
        assertFalse(result.topAnomalies.any { it.featureName.contains("Swipe") })
    }

    private fun buildProfile(accessibilityMode: Boolean): BaselineProfile {
        val builder = BaselineBuilder()
        builder.setAccessibilityMode(accessibilityMode)
        builder.addVector(vector(value = 10.0))
        builder.addVector(vector(value = 12.0))
        builder.addVector(vector(value = 14.0))
        return builder.build()
    }

    private fun profile(mean: Double, std: Double): BaselineProfile {
        return BaselineProfile(
            featureMeans = Tier1FeatureKeys.ALL.associateWith { mean },
            featureStds = Tier1FeatureKeys.ALL.associateWith { std },
            sessionCount = 3,
            isAccessibilityProfile = false,
            createdTimestampMs = 0L
        )
    }

    private fun vector(value: Double): FeatureVector {
        return vector(values = Tier1FeatureKeys.ALL.associateWith { value })
    }

    private fun vector(values: Map<String, Double>): FeatureVector {
        return FeatureVector(
            windowStartMs = 0L,
            windowEndMs = 10_000L,
            keystrokeCount = 20,
            gestureCount = 5,
            confidence = 1.0,
            meanInterKeyInterval = values.getValue("meanInterKeyInterval"),
            stdInterKeyInterval = values.getValue("stdInterKeyInterval"),
            deleteRatio = values.getValue("deleteRatio"),
            typingSpeed = values.getValue("typingSpeed"),
            meanSwipeVelocity = values.getValue("meanSwipeVelocity"),
            stdSwipeVelocity = values.getValue("stdSwipeVelocity"),
            meanSwipeDuration = values.getValue("meanSwipeDuration"),
            meanSwipeDistance = values.getValue("meanSwipeDistance"),
            medianInterKeyInterval = 0.0,
            stdSwipeDuration = 0.0,
            stdSwipeDistance = 0.0
        )
    }
}
