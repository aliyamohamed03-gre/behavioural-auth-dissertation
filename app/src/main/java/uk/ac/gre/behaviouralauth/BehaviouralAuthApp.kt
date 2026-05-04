package uk.ac.gre.behaviouralauth

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uk.ac.gre.behaviouralauth.model.AuthState
import uk.ac.gre.behaviouralauth.ui.AppViewModel
import uk.ac.gre.behaviouralauth.ui.PinVerificationResult
import uk.ac.gre.behaviouralauth.ui.components.LoadingScreen
import uk.ac.gre.behaviouralauth.ui.navigation.AppDestination
import uk.ac.gre.behaviouralauth.ui.screens.ConsentScreen
import uk.ac.gre.behaviouralauth.ui.screens.EnrollmentScreen
import uk.ac.gre.behaviouralauth.ui.screens.HomeScreen
import uk.ac.gre.behaviouralauth.ui.screens.SettingsScreen
import uk.ac.gre.behaviouralauth.ui.screens.StepUpScreen
import uk.ac.gre.behaviouralauth.ui.screens.TestScreen

//Main app entry point that connects navigation, UI screens, and the shared ViewModel.
@Composable
fun BehaviouralAuthApp(viewModel: AppViewModel = viewModel()) {
    val uiState = viewModel.uiState
    val navController = rememberNavController()
    val context = LocalContext.current

    //Shows a loading screen until the ViewModel has finished preparing app state.
    if (!uiState.isReady) {
        LoadingScreen()
        return
    }

    //Sends new users to consent first and returning users directly to the home screen.
    val startDestination = if (uiState.hasConsented) {
        AppDestination.Home.route
    } else {
        AppDestination.Consent.route
    }

    //Defines the navigation graph for all app screens.
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppDestination.Consent.route) {
            ConsentScreen(
                onContinue = {
                    viewModel.grantConsent()
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Consent.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestination.Home.route) {
            HomeScreen(
                uiState = uiState,
                onStartEnrollment = { navController.navigate(AppDestination.Enrollment.route) },
                onStartTestSession = {
                    viewModel.startTestSession()
                    navController.navigate(AppDestination.Test.route)
                },
                onOpenSettings = { navController.navigate(AppDestination.Settings.route) }
            )
        }

        composable(AppDestination.Enrollment.route) {
            EnrollmentScreen(
                uiState = uiState,
                onNextPassage = viewModel::showNextEnrollmentPassage,
                onTextChanged = viewModel::updateEnrollmentText,
                onGestureCaptured = viewModel::registerEnrollmentGesture,
                onFinishSession = {
                    viewModel.finishEnrollmentSession()
                    Toast.makeText(context, "Enrollment session saved.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            )
        }

        composable(AppDestination.Test.route) {
            //Redirects to step-up verification when the behavioural state becomes locked.
            LaunchedEffect(uiState.authState) {
                if (uiState.authState == AuthState.LOCKED) {
                    navController.navigate(AppDestination.StepUp.route) {
                        launchSingleTop = true
                    }
                }
            }

            TestScreen(
                uiState = uiState,
                onSelectLabel = viewModel::selectSessionLabel,
                onTextChanged = viewModel::updateTestText,
                onGestureCaptured = viewModel::registerTestGesture,
                onEndSession = {
                    viewModel.endTestSession()
                    navController.popBackStack()
                }
            )
        }

        composable(AppDestination.StepUp.route) {
            StepUpScreen(
                uiState = uiState,
                onPinChanged = viewModel::updatePinInput,
                onVerify = {
                    //Handles each PIN verification outcome with feedback and navigation.
                    when (viewModel.verifyStepUpPin()) {
                        PinVerificationResult.Success -> {
                            Toast.makeText(context, "PIN accepted.", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }

                        PinVerificationResult.FailedRetry -> {
                            Toast.makeText(context, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show()
                        }

                        PinVerificationResult.FailedLockedOut -> {
                            Toast.makeText(
                                context,
                                "No attempts left. Returning Home.",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.navigate(AppDestination.Home.route) {
                                popUpTo(AppDestination.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }

                        PinVerificationResult.InvalidFormat -> {
                            Toast.makeText(
                                context,
                                "Enter a 4 to 6 digit PIN.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onBackToHome = {
                    viewModel.exitLockedSessionToHome()
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppDestination.Settings.route) {
            SettingsScreen(
                uiState = uiState,
                onSensitivityChanged = viewModel::updateSensitivity,
                onExportData = {
                    Toast.makeText(
                        context,
                        "CSV export will be added in a later phase.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onResetProfile = viewModel::resetProfile,
                auditLogEntryCount = viewModel.auditLogEntryCount(),
                onVerifyAuditChain = viewModel::verifyAuditChain
            )
        }
    }
}