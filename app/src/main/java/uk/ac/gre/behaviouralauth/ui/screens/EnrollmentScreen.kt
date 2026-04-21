package uk.ac.gre.behaviouralauth.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.ac.gre.behaviouralauth.capture.SwipeEvent
import uk.ac.gre.behaviouralauth.capture.captureSwipes
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.model.EnrollmentPassages
import uk.ac.gre.behaviouralauth.ui.AppViewModel
import uk.ac.gre.behaviouralauth.ui.components.AppButton
import uk.ac.gre.behaviouralauth.ui.components.ScreenFrame
import uk.ac.gre.behaviouralauth.ui.components.SwipeCaptureArea

@Composable
fun EnrollmentScreen(
    uiState: AppUiState,
    onNextPassage: () -> Unit,
    onTextChanged: (String) -> Unit,
    onGestureCaptured: (SwipeEvent) -> Unit,
    onFinishSession: () -> Unit
) {
    val activity = LocalContext.current.findComponentActivity()
    val sharedViewModel: AppViewModel? = if (activity != null) {
        viewModel(viewModelStoreOwner = activity)
    } else {
        null
    }

    LaunchedEffect(sharedViewModel) {
        sharedViewModel?.startEnrollmentSession()
    }

    val enrollmentState = uiState.enrollment
    val sessionProgress = uiState.sessionsCompleted.coerceAtMost(3) / 3f

    ScreenFrame(
        title = "Enrollment Session",
        contentDescription = "Enrollment session screen"
    ) {


        Text(
            text = "Type the passage below naturally to build your behavioural profile.",
            style = MaterialTheme.typography.bodyLarge
        )

        AccessibilityModeCard(
            enabled = enrollmentState.isAccessibilityMode,
            onToggle = {
                sharedViewModel?.toggleAccessibilityMode()
            }
        )

        Text(
            text = EnrollmentPassages[enrollmentState.currentPassageIndex],
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF0F0F0), RoundedCornerShape(18.dp))
                .semantics { contentDescription = "Enrollment typing passage" }
                .padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        AppButton(
            text = "Next Passage",
            onClick = onNextPassage
        )

        OutlinedTextField(
            value = enrollmentState.typedText,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .semantics {
                    contentDescription = "Type the displayed passage here to record your typing pattern"
                },
            label = { Text("Type here") },
            minLines = 6,
            textStyle = MaterialTheme.typography.bodyLarge
        )

        SwipeCaptureArea(
            onGestureCaptured = {},
            modifier = Modifier.captureSwipes { event ->
                onGestureCaptured(event)
            },
            contentDescription = "Swipe area. Perform swipe gestures here to record your gesture pattern"
        )

        Text(
            text = "Keystrokes: ${enrollmentState.keystrokeCount} | Gestures: ${enrollmentState.gestureCount}",
            modifier = Modifier.semantics {
                contentDescription = "Enrollment progress: ${enrollmentState.keystrokeCount} keystrokes and ${enrollmentState.gestureCount} gestures recorded"
            },
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Enrollment sessions: ${uiState.sessionsCompleted.coerceAtMost(3)}/3",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        LinearProgressIndicator(
            progress = sessionProgress,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 10.dp)
                .semantics {
                    contentDescription = "Enrollment progress: ${enrollmentState.keystrokeCount} keystrokes and ${enrollmentState.gestureCount} gestures recorded"
                }
        )

        AppButton(
            text = "Finish Session",
            onClick = onFinishSession
        )
    }
}

@Composable
private fun AccessibilityModeCard(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val activeBackground = Color(0xFFE8F1FF)
    val neutralBackground = MaterialTheme.colorScheme.surfaceVariant
    val iconTint = if (enabled) {
        Color(0xFF315DA8)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (enabled) {
                    "Accessibility enrollment mode. Currently enabled, wider tolerance active"
                } else {
                    "Accessibility enrollment mode. Currently disabled"
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) activeBackground else neutralBackground
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = iconTint
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Accessibility Mode",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Widens tolerance for users with motor impairments",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Toggle accessibility enrollment mode"
                        }
                )
            }

            if (enabled) {
                Text(
                    text = "Baseline will accommodate higher behavioural variance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = iconTint
                )
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}
