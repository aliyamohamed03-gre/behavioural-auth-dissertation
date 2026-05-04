package uk.ac.gre.behaviouralauth.audit


sealed class AuditEvent(val eventType: String) {
    data class ConsentGranted(val timestamp: Long) : AuditEvent("CONSENT_GRANTED")
    //Logged when the user gives permission for behavioural data collection
    data class EnrollmentStarted(
        val timestamp: Long,
        val isAccessibilityMode: Boolean
    ) : AuditEvent("ENROLLMENT_STARTED")

    data class EnrollmentCompleted(
        val timestamp: Long,
        val sessionCount: Int,
        val isAccessibilityMode: Boolean
    ) : AuditEvent("ENROLLMENT_COMPLETED")

    data class TestSessionStarted(val timestamp: Long) : AuditEvent("TEST_SESSION_STARTED")
    data class TestSessionEnded(val timestamp: Long) : AuditEvent("TEST_SESSION_ENDED")
    data class WindowScored(
        val timestamp: Long,
        val trustScore: Int,
        val confidence: Double,
        val isSuspicious: Boolean
    ) : AuditEvent("WINDOW_SCORED")

    data class StateChanged(
        val timestamp: Long,
        val fromState: String,
        val toState: String,
        val reason: String
    ) : AuditEvent("STATE_CHANGED")

    data class PinAttempted(
        val timestamp: Long,
        val success: Boolean,
        val attemptsRemaining: Int
    ) : AuditEvent("PIN_ATTEMPTED")

    data class ProfileReset(val timestamp: Long) : AuditEvent("PROFILE_RESET")
    data class DataExported(
        val timestamp: Long,
        val recordCount: Int
    ) : AuditEvent("DATA_EXPORTED")
    //Captures changes to the system’s sensitivity level
    data class SensitivityChanged(
        val timestamp: Long,
        val newLevel: String
    ) : AuditEvent("SENSITIVITY_CHANGED")
}
