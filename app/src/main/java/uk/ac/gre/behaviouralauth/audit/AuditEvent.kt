package uk.ac.gre.behaviouralauth.audit


sealed class AuditEvent(val eventType: String) {
    ////Logged when the user gives permission for behavioural data collection
    data class ConsentGranted(val timestamp: Long) : AuditEvent("CONSENT_GRANTED")
    //the point where the user starts creating their behavioural profile
    data class EnrollmentStarted(
        val timestamp: Long,
        val isAccessibilityMode: Boolean
    ) : AuditEvent("ENROLLMENT_STARTED")
    //Stores the result of a completed enrolment process
    data class EnrollmentCompleted(
        val timestamp: Long,
        val sessionCount: Int,
        val isAccessibilityMode: Boolean
    ) : AuditEvent("ENROLLMENT_COMPLETED")
    //Used to track when a testing session begins and end
    data class TestSessionStarted(val timestamp: Long) : AuditEvent("TEST_SESSION_STARTED")
    data class TestSessionEnded(val timestamp: Long) : AuditEvent("TEST_SESSION_ENDED")
    //Records the trust score calculated for a behavioural window
    data class WindowScored(
        val timestamp: Long,
        val trustScore: Int,
        val confidence: Double,
        val isSuspicious: Boolean
    ) : AuditEvent("WINDOW_SCORED")
    //Keeps a clear record of authentication state changes and their reason
    data class StateChanged(
        val timestamp: Long,
        val fromState: String,
        val toState: String,
        val reason: String
    ) : AuditEvent("STATE_CHANGED")
    //Tracks PIN attempts so failed or successful fallback access can be reviewed
    data class PinAttempted(
        val timestamp: Long,
        val success: Boolean,
        val attemptsRemaining: Int
    ) : AuditEvent("PIN_ATTEMPTED")
    //Logged when the stored behavioural profile is cleared
    data class ProfileReset(val timestamp: Long) : AuditEvent("PROFILE_RESET")
    //Records when audit or behavioural records are exported
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
