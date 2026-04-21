package uk.ac.gre.behaviouralauth.ui.navigation

sealed class AppDestination(val route: String) {
    data object Consent : AppDestination("consent")
    data object Home : AppDestination("home")
    data object Enrollment : AppDestination("enrollment")
    data object Test : AppDestination("test")
    data object StepUp : AppDestination("step_up")
    data object Settings : AppDestination("settings")
}
