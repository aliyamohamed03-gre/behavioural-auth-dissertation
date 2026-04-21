package uk.ac.gre.behaviouralauth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.ac.gre.behaviouralauth.ui.components.AppButton
import uk.ac.gre.behaviouralauth.ui.components.ScreenFrame
import uk.ac.gre.behaviouralauth.ui.components.SectionCard

@Composable
fun ConsentScreen(
    onContinue: () -> Unit
) {
    var consentChecked by rememberSaveable { mutableStateOf(false) }

    ScreenFrame(
        title = "Before We Begin",
        contentDescription = "Consent screen"
    ) {
        SectionCard(contentDescription = "Consent details") {
            Text(
                text = "This prototype collects behavioural data to study how people type and swipe during smartphone use.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Data collected: keystroke timing, typing rhythm, gesture patterns, and session metadata used for behavioural authentication research.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Why it is collected: to build and test a behavioural biometric authentication prototype for a university final year project.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "How it is stored: locally on the device using AES-256 encrypted storage. No cloud upload is performed in this prototype.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Your rights: you can delete your stored profile and session data at any time from Settings.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Research context: Behavioural Biometric Authentication Prototype, COMP1682 Final Year Project, University of Greenwich.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = consentChecked,
                onCheckedChange = { consentChecked = it },
                modifier = Modifier.semantics {
                    contentDescription = "I agree to data collection for behavioural authentication"
                }
            )
            Text(
                text = "I understand and consent to behavioural data collection",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        AppButton(
            text = "Continue",
            enabled = consentChecked,
            onClick = onContinue,
            disabledStateDescription = "Disabled, tick the consent checkbox first"
        )
    }
}
