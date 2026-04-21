package uk.ac.gre.behaviouralauth.model

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import uk.ac.gre.behaviouralauth.ui.theme.ScoreAmber
import uk.ac.gre.behaviouralauth.ui.theme.ScoreGreen
import uk.ac.gre.behaviouralauth.ui.theme.ScoreRed
import uk.ac.gre.behaviouralauth.ui.theme.StatusGreyBackground
import uk.ac.gre.behaviouralauth.ui.theme.StatusGreyText
import uk.ac.gre.behaviouralauth.ui.theme.StatusLockedBackground
import uk.ac.gre.behaviouralauth.ui.theme.StatusLockedText
import uk.ac.gre.behaviouralauth.ui.theme.StatusTrustedBackground
import uk.ac.gre.behaviouralauth.ui.theme.StatusTrustedText
import uk.ac.gre.behaviouralauth.ui.theme.StatusUncertainBackground
import uk.ac.gre.behaviouralauth.ui.theme.StatusUncertainText

enum class AuthState(
    val label: String,
    val symbol: String,
    val bannerBackground: Color,
    val bannerText: Color
) {
    NOT_ENROLLED("Not Enrolled", "i", StatusGreyBackground, StatusGreyText),
    TRUSTED("Trusted", "✓", StatusTrustedBackground, StatusTrustedText),
    UNCERTAIN("Uncertain", "⚠", StatusUncertainBackground, StatusUncertainText),
    LOCKED("Locked", "🔒", StatusLockedBackground, StatusLockedText)
}

enum class SensitivitySetting(
    val title: String,
    val description: String,
    val suspiciousWindowsBeforeChange: Int,
    val suspiciousThreshold: Int,
    val zScoreMultiplier: Double,
    val storageValue: Int
) {
    LOW(
        title = "Low",
        description = "Wider tolerance, fewer lockouts. Best for accessibility.",
        suspiciousWindowsBeforeChange = 4,
        suspiciousThreshold = 35,
        zScoreMultiplier = 0.7,
        storageValue = 0
    ),
    REDUCED(
        title = "Reduced",
        description = "Moderately relaxed tolerance.",
        suspiciousWindowsBeforeChange = 3,
        suspiciousThreshold = 40,
        zScoreMultiplier = 0.85,
        storageValue = 1
    ),
    NORMAL(
        title = "Normal",
        description = "Balanced security and usability.",
        suspiciousWindowsBeforeChange = 2,
        suspiciousThreshold = 50,
        zScoreMultiplier = 1.0,
        storageValue = 2
    ),
    STRICT(
        title = "Strict",
        description = "Tighter tolerance, more frequent challenges.",
        suspiciousWindowsBeforeChange = 2,
        suspiciousThreshold = 60,
        zScoreMultiplier = 1.15,
        storageValue = 3
    ),
    HIGH(
        title = "High",
        description = "Maximum security. May cause frequent lockouts.",
        suspiciousWindowsBeforeChange = 1,
        suspiciousThreshold = 70,
        zScoreMultiplier = 1.3,
        storageValue = 4
    );

    companion object {
        val MEDIUM: SensitivitySetting
            get() = NORMAL

        fun fromStorage(value: Int): SensitivitySetting {
            return entries.firstOrNull { it.storageValue == value } ?: NORMAL
        }

        fun fromSlider(value: Float): SensitivitySetting {
            return when (value.roundToInt().coerceIn(0, 4)) {
                0 -> LOW
                1 -> REDUCED
                2 -> NORMAL
                3 -> STRICT
                4 -> HIGH
                else -> NORMAL
            }
        }
    }
}

enum class SessionLabel(val displayName: String) {
    NORMAL("Normal"),
    ALTERED_SPEED("Altered Speed"),
    DIFFERENT_HAND("Different Hand"),
    SIMULATED_IMPERSONATION("Simulated Impersonation"),
    ACCESSIBILITY_SIMULATION("Accessibility Simulation")
}

data class AnomalyFactor(
    val featureName: String,
    val zScore: Double
)

data class EnrollmentUiState(
    val currentPassageIndex: Int = 0,
    val typedText: String = "",
    val keystrokeCount: Int = 0,
    val gestureCount: Int = 0,
    val isAccessibilityMode: Boolean = false
)

data class TestUiState(
    val selectedLabel: SessionLabel = SessionLabel.NORMAL,
    val typedText: String = "",
    val keystrokeCount: Int = 0,
    val gestureCount: Int = 0,
    val trustScore: Int? = null,
    val meanZScore: Double? = null,
    val confidence: Double? = null,
    val state: AuthState? = null,
    val anomalyFactors: List<AnomalyFactor> = emptyList(),
    val windowLogs: List<String> = emptyList(),
    val lastLoggedWindow: Int = 0
)

data class StepUpUiState(
    val pinInput: String = "",
    val attemptsRemaining: Int = 3
)

data class AppUiState(
    val isReady: Boolean = false,
    val hasConsented: Boolean = false,
    val authState: AuthState = AuthState.NOT_ENROLLED,
    val sessionsCompleted: Int = 0,
    val hasProfile: Boolean = false,
    val sensitivity: SensitivitySetting = SensitivitySetting.NORMAL,
    val dataSizeText: String = "0 B",
    val enrollment: EnrollmentUiState = EnrollmentUiState(),
    val test: TestUiState = TestUiState(),
    val stepUp: StepUpUiState = StepUpUiState()
)

val EnrollmentPassages = listOf(
    "The quick brown fox jumps over the lazy dog. In the world of cybersecurity, authentication is the process of verifying identity. Smartphone usage has increased dramatically, creating new security challenges.",
    "Behavioural biometrics measures how someone interacts with a device over time. Typing rhythm and swipe patterns can support secure authentication without interrupting normal phone usage.",
    "Continuous authentication checks for changes in user behaviour after login. A reliable prototype should balance usability, accessibility, and strong protection against impersonation attempts."
)

fun trustScoreColor(score: Int?): Color {
    return when {
        score == null -> StatusGreyText
        score > 70 -> ScoreGreen
        score >= 40 -> ScoreAmber
        else -> ScoreRed
    }
}
