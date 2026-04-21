package uk.ac.gre.behaviouralauth.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.ac.gre.behaviouralauth.audit.ChainVerificationResult
import uk.ac.gre.behaviouralauth.export.ShareHelper
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.ui.AppViewModel
import uk.ac.gre.behaviouralauth.ui.components.AppButton
import uk.ac.gre.behaviouralauth.ui.components.ScreenFrame
import uk.ac.gre.behaviouralauth.ui.components.SectionCard

@Composable
fun SettingsScreen(
    uiState: AppUiState,
    onSensitivityChanged: (Float) -> Unit,
    onExportData: () -> Unit,
    onResetProfile: () -> Unit,
    auditLogEntryCount: Int,
    onVerifyAuditChain: () -> ChainVerificationResult
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var chainVerificationResult by remember {
        mutableStateOf<ChainVerificationResult?>(null)
    }
    val context = LocalContext.current
    val activity = context.findComponentActivity()
    val sharedViewModel: AppViewModel? = if (activity != null) {
        viewModel(viewModelStoreOwner = activity)
    } else {
        null
    }
    val featureRecordCount = sharedViewModel?.featureRecordCount() ?: 0

    ScreenFrame(
        title = "Settings",
        contentDescription = "Settings screen"
    ) {
        SectionCard(
            title = "Authentication Sensitivity",
            contentDescription = "Authentication sensitivity settings"
        ) {
            val sensitivity = uiState.sensitivity

            Slider(
                value = sensitivity.storageValue.toFloat(),
                onValueChange = onSensitivityChanged,
                valueRange = 0f..4f,
                steps = 3,
                modifier = Modifier.semantics {
                    contentDescription = "Security sensitivity level. Currently set to ${sensitivity.title}. ${sensitivity.description}"
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Low",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "High",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = "Current: ${sensitivity.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sensitivity.description,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${sensitivity.suspiciousWindowsBeforeChange} suspicious windows before state change",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Suspicious threshold: ${sensitivity.suspiciousThreshold}",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        AppButton(
            text = "Export Behavioural Data ($featureRecordCount records)",
            onClick = {
                val file = sharedViewModel?.exportFeaturesCsv()
                if (file != null) {
                    Toast.makeText(
                        context,
                        "Export ready - choose an app to share",
                        Toast.LENGTH_SHORT
                    ).show()
                    ShareHelper.shareCsv(context, file)
                } else {
                    Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = featureRecordCount > 0,
            contentDescription = "Export behavioural data as CSV. $featureRecordCount records available",
            disabledStateDescription = "Disabled, no behavioural data to export yet"
        )

        AppButton(
            text = "Reset Profile",
            onClick = { showResetDialog = true },
            contentDescription = "Reset profile. This will delete all enrollment data"
        )

        SectionCard(title = "Your Data", contentDescription = "User data summary") {
            Text(
                text = "Session count: ${uiState.sessionsCompleted}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Data size: ${uiState.dataSizeText}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Profile status: ${if (uiState.hasProfile) "Profile created" else "No profile yet"}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Audit log: $auditLogEntryCount entries",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = {
                    chainVerificationResult = onVerifyAuditChain()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .semantics {
                        contentDescription = "Verify audit chain integrity"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify Chain Integrity")
            }
        }

        SectionCard(title = "About", contentDescription = "About this prototype") {
            Text(
                text = "Behavioural Biometric Authentication Prototype | COMP1682 Final Year Project | University of Greenwich",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetProfile()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Reset Profile") },
            text = {
                Text("This will clear your stored behavioural profile and enrollment progress.")
            }
        )
    }

    chainVerificationResult?.let { result ->
        AlertDialog(
            onDismissRequest = { chainVerificationResult = null },
            confirmButton = {
                TextButton(onClick = { chainVerificationResult = null }) {
                    Text("OK")
                }
            },
            title = {
                Text(if (result.isValid) "Chain Verified" else "Integrity Failure")
            },
            text = {
                Text(
                    if (result.isValid) {
                        "Chain integrity verified. All ${result.entriesChecked} entries are intact."
                    } else {
                        "CHAIN INTEGRITY FAILURE at entry #${result.brokenAtSequence}. Possible tampering detected."
                    }
                )
            }
        )
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}
