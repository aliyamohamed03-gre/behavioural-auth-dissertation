package uk.ac.gre.behaviouralauth.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import uk.ac.gre.behaviouralauth.capture.SwipeEvent
import uk.ac.gre.behaviouralauth.capture.captureSwipes
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.model.AuthState
import uk.ac.gre.behaviouralauth.model.SessionLabel
import uk.ac.gre.behaviouralauth.model.trustScoreColor
import uk.ac.gre.behaviouralauth.ui.components.AppButton
import uk.ac.gre.behaviouralauth.ui.components.ScreenFrame
import uk.ac.gre.behaviouralauth.ui.components.SectionCard
import uk.ac.gre.behaviouralauth.ui.components.SwipeCaptureArea

//Displays the live authentication testing screen for typing and swipe behaviour.
@Composable
fun TestScreen(
    uiState: AppUiState,
    onSelectLabel: (SessionLabel) -> Unit,
    onTextChanged: (String) -> Unit,
    onGestureCaptured: (SwipeEvent) -> Unit,
    onEndSession: () -> Unit
) {
    val testState = uiState.test
    val trustScoreText = testState.trustScore?.toString() ?: "--"
    val meanZText = testState.meanZScore?.let {
        String.format(Locale.US, "%.1f", it)
    } ?: "--"
    val confidenceText = testState.confidence?.let {
        String.format(Locale.US, "%.2f", it)
    } ?: "--"
    val stateText = testState.state?.let { "${it.symbol} ${it.label}" } ?: "--"
    val trustScoreAccessibilityText =
        "Current trust score: ${testState.trustScore ?: "not yet calculated"}. ${testState.state?.label ?: ""}"
    val showAnomalyPanel = testState.state == AuthState.UNCERTAIN || testState.state == AuthState.LOCKED

    ScreenFrame(
        title = "Authentication Test",
        contentDescription = "Authentication test screen"
    ) {
        Text(
            text = "Session label",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        //Lets the user label the session type for testing and analysis.
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SessionLabel.entries) { label ->
                FilterChip(
                    selected = label == testState.selectedLabel,
                    onClick = { onSelectLabel(label) },
                    label = {
                        Text(
                            text = label.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Session type: ${label.displayName}"
                    }
                )
            }
        }

        //Captures typing behaviour during the authentication test session.
        OutlinedTextField(
            value = testState.typedText,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .semantics {
                    contentDescription = "Type here during authentication testing to record your typing pattern"
                },
            label = { Text("Type here") },
            minLines = 6,
            textStyle = MaterialTheme.typography.bodyLarge
        )

        //Captures swipe behaviour during the authentication test session.
        SwipeCaptureArea(
            onGestureCaptured = {},
            modifier = Modifier.captureSwipes { event ->
                onGestureCaptured(event)
            },
            contentDescription = "Swipe area. Perform swipe gestures here to record your gesture pattern"
        )

        //Shows the latest trust score, anomaly level, state, and confidence.
        SectionCard(title = "Live Scoring", contentDescription = "Live scoring panel") {
            Text(
                text = "Trust Score: $trustScoreText/100",
                modifier = Modifier.semantics {
                    contentDescription = trustScoreAccessibilityText
                    liveRegion = LiveRegionMode.Polite
                },
                style = MaterialTheme.typography.headlineSmall,
                color = trustScoreColor(testState.trustScore),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "(mean z = $meanZText)",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "State: $stateText",
                modifier = Modifier.semantics {
                    contentDescription = "Authentication state: ${testState.state?.label ?: "not yet determined"}"
                    liveRegion = LiveRegionMode.Assertive
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Confidence: $confidenceText",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        //Displays the strongest anomaly factors when the session becomes uncertain or locked.
        if (showAnomalyPanel && testState.anomalyFactors.isNotEmpty()) {
            SectionCard(
                title = "Anomaly Explanation",
                contentDescription = "Top contributing factors to anomaly detection"
            ) {
                testState.anomalyFactors.take(3).forEachIndexed { index, factor ->
                    val factorLabel = when (index) {
                        0 -> "Primary factor"
                        1 -> "Secondary factor"
                        else -> "Additional factor"
                    }
                    val zScoreText = String.format(Locale.US, "%.1f", factor.zScore)
                    Text(
                        text = "$factorLabel: ${factor.featureName} (z = $zScoreText)",
                        modifier = Modifier.semantics {
                            contentDescription = "${factor.featureName}: deviation of $zScoreText standard deviations"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        //Lists recent scoring windows so changes over time can be reviewed.
        SectionCard(
            title = "Recent Windows",
            contentDescription = "Recent window scoring log"
        ) {
            if (testState.windowLogs.isEmpty()) {
                Text(
                    text = "No windows scored yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                testState.windowLogs.forEach { entry ->
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE1E5EB), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }
            }
        }

        AppButton(
            text = "End Session",
            onClick = onEndSession
        )
    }
}