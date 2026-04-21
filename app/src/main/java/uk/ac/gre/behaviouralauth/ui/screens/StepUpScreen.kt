package uk.ac.gre.behaviouralauth.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.ui.components.AppButton
import uk.ac.gre.behaviouralauth.ui.components.ScreenFrame

@Composable
fun StepUpScreen(
    uiState: AppUiState,
    onPinChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onBackToHome: () -> Unit
) {
    BackHandler(onBack = onBackToHome)

    ScreenFrame(
        title = "Identity Verification Required",
        contentDescription = "Step up PIN screen"
    ) {
        Text(
            text = "Your behaviour pattern has changed. Please enter your PIN to continue.",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = uiState.stepUp.pinInput,
            onValueChange = onPinChanged,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Enter your recovery PIN. ${uiState.stepUp.attemptsRemaining} attempts remaining"
                },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge
        )

        AppButton(
            text = "Verify",
            onClick = onVerify,
            contentDescription = "Submit PIN for verification"
        )

        Text(
            text = "Attempts remaining: ${uiState.stepUp.attemptsRemaining}",
            modifier = Modifier.semantics {
                contentDescription = "Attempts remaining: ${uiState.stepUp.attemptsRemaining}"
                liveRegion = LiveRegionMode.Polite
            },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
