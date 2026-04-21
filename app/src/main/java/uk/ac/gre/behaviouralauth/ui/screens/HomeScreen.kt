package uk.ac.gre.behaviouralauth.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.ui.components.AppButton
import uk.ac.gre.behaviouralauth.ui.components.ScreenFrame
import uk.ac.gre.behaviouralauth.ui.components.StatusBanner

@Composable
fun HomeScreen(
    uiState: AppUiState,
    onStartEnrollment: () -> Unit,
    onStartTestSession: () -> Unit,
    onOpenSettings: () -> Unit
) {
    ScreenFrame(
        title = "Behavioural Auth",
        contentDescription = "Home screen"
    ) {
        StatusBanner(authState = uiState.authState)

        Text(
            text = "Prototype status and session controls",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        AppButton(
            text = "Start Enrollment",
            onClick = onStartEnrollment
        )

        AppButton(
            text = "Start Test Session",
            onClick = onStartTestSession,
            enabled = uiState.hasProfile,
            disabledStateDescription = "Disabled, complete enrollment first"
        )

        AppButton(
            text = "Settings",
            onClick = onOpenSettings
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Sessions completed: ${uiState.sessionsCompleted} | Profile: ${if (uiState.hasProfile) "Available" else "None"}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
