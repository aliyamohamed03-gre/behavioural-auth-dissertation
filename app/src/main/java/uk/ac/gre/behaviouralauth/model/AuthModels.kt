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

//Represents the main authentication state shown to the user.
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

//Controls how strict the behavioural authentication checks should be.
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

        //Converts a saved integer back into the matching sensitivity setting.
        fun fromStorage(value: Int): SensitivitySetting {
            return entries.firstOrNull { it.storageValue == value } ?: NORMAL
        }

        //Converts the slider position into a sensitivity level.
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

//Labels the type of test session being recorded.
enum class SessionLabel(val displayName: String) {
    NORMAL("Normal"),
    ALTERED_SPEED("Altered Speed"),
    DIFFERENT_HAND("Different Hand"),
    SIMULATED_IMPERSONATION("Simulated Impersonation"),
    ACCESSIBILITY_SIMULATION("Accessibility Simulation")
}

//Stores a feature that strongly contributed to unusual behaviour.
data class AnomalyFactor(
    val featureName: String,
    val zScore: Double
)

//Holds the current screen data for the enrolment process.
data class EnrollmentUiState(
    val currentPassageIndex: Int = 0,
    val typedText: String = "",
    val keystrokeCount: Int = 0,
    val gestureCount: Int = 0,
    val isAccessibilityMode: Boolean = false
)

//Holds the current screen data for a testing session.
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

//Stores the PIN challenge state when extra verification is needed.
data class StepUpUiState(
    val pinInput: String = "",
    val attemptsRemaining: Int = 3
)

//Combines the main app state into one object for the UI to observe.
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

//Text passages used to collect typing behaviour during enrolment.
val EnrollmentPassages = listOf(
    "The quick brown fox jumps over the lazy dog.",
    "Smartphone usage has increased dramatically, creating new security challenges.",
    "Continuous authentication checks for changes in user behaviour after login. "
)

fun trustScoreColor(score: Int?): Color {
    //Chooses a colour that matches the trust score range.
    return when {
        score == null -> StatusGreyText
        score > 70 -> ScoreGreen
        score >= 40 -> ScoreAmber
        else -> ScoreRed
    }
}