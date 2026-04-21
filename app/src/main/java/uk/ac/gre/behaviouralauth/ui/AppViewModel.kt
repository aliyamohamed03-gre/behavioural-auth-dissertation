package uk.ac.gre.behaviouralauth.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.ac.gre.behaviouralauth.audit.AuditEvent
import uk.ac.gre.behaviouralauth.audit.AuditLogger
import uk.ac.gre.behaviouralauth.audit.ChainVerificationResult
import uk.ac.gre.behaviouralauth.capture.FeatureVector
import uk.ac.gre.behaviouralauth.capture.FeatureWindowController
import uk.ac.gre.behaviouralauth.capture.KeystrokeCaptureSource
import uk.ac.gre.behaviouralauth.capture.SwipeEvent
import uk.ac.gre.behaviouralauth.data.SecurePreferencesRepository
import uk.ac.gre.behaviouralauth.export.CsvExporter
import uk.ac.gre.behaviouralauth.export.FeatureRecord
import uk.ac.gre.behaviouralauth.export.FeatureRecordStore
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.model.AuthState
import uk.ac.gre.behaviouralauth.model.EnrollmentUiState
import uk.ac.gre.behaviouralauth.model.SensitivitySetting
import uk.ac.gre.behaviouralauth.model.SessionLabel
import uk.ac.gre.behaviouralauth.model.StepUpUiState
import uk.ac.gre.behaviouralauth.model.TestUiState
import uk.ac.gre.behaviouralauth.scoring.BaselineBuilder
import uk.ac.gre.behaviouralauth.scoring.BaselineProfile
import uk.ac.gre.behaviouralauth.scoring.BaselineRepository
import uk.ac.gre.behaviouralauth.scoring.ZScoreEngine

private const val FEATURE_WINDOW_LOG_TAG = "FeatureWindow"
private const val EXPORT_USER_ID = "user_001"
private const val SESSION_TYPE_ENROLLMENT = "ENROLLMENT"
private const val SESSION_TYPE_TEST = "TEST"

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SecurePreferencesRepository(application.applicationContext)
    private val baselineRepository = BaselineRepository(application.applicationContext)
    private val auditLogger = AuditLogger(application.applicationContext)
    private val baselineBuilder = BaselineBuilder()
    private val featureRecordStore = FeatureRecordStore()
    private val enrollmentVectorsForProfile = mutableListOf<FeatureVector>()
    private var currentProfile: BaselineProfile? = null
    private val keystrokeCaptureSource = KeystrokeCaptureSource()
    private var lastEnrollmentVector: FeatureVector? = null
    private var lastTestVector: FeatureVector? = null
    private var activeMode: ActiveMode = ActiveMode.IDLE
    private var enrollmentController = createEnrollmentController()
    private var testController = createTestController()

    var uiState by mutableStateOf(AppUiState())
        private set

    init {
        loadInitialState()
        startWindowTicker()
    }

    fun grantConsent() {
        repository.setConsent(true)
        auditLogger.log(AuditEvent.ConsentGranted(System.currentTimeMillis()))
        uiState = uiState.copy(
            hasConsented = true,
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    fun showNextEnrollmentPassage() {
        uiState = uiState.copy(
            enrollment = uiState.enrollment.copy(
                currentPassageIndex = (uiState.enrollment.currentPassageIndex + 1) % 3
            )
        )
    }

    fun startEnrollmentSession() {
        baselineBuilder.reset()
        baselineBuilder.setAccessibilityMode(false)
        enrollmentVectorsForProfile.forEach { vector ->
            baselineBuilder.addVector(vector)
        }
        uiState = uiState.copy(
            enrollment = uiState.enrollment.copy(isAccessibilityMode = false)
        )
        enrollmentController = createEnrollmentController()
        lastEnrollmentVector = null
        activeMode = ActiveMode.ENROLLING
        auditLogger.log(
            AuditEvent.EnrollmentStarted(
                timestamp = System.currentTimeMillis(),
                isAccessibilityMode = uiState.enrollment.isAccessibilityMode
            )
        )
    }

    fun toggleAccessibilityMode() {
        val newMode = !uiState.enrollment.isAccessibilityMode
        baselineBuilder.setAccessibilityMode(newMode)
        uiState = uiState.copy(
            enrollment = uiState.enrollment.copy(isAccessibilityMode = newMode)
        )
    }

    fun updateEnrollmentText(newText: String) {
        val oldText = uiState.enrollment.typedText
        keystrokeCaptureSource.handleComposeTextChange(oldText, newText) { event ->
            enrollmentController.submitKeystroke(event)
        }
        val addedCharacters = (newText.length - oldText.length).coerceAtLeast(0)
        uiState = uiState.copy(
            enrollment = uiState.enrollment.copy(
                typedText = newText,
                keystrokeCount = uiState.enrollment.keystrokeCount + addedCharacters
            )
        )
    }

    fun registerEnrollmentGesture(event: SwipeEvent) {
        enrollmentController.submitSwipe(event)
        uiState = uiState.copy(
            enrollment = uiState.enrollment.copy(
                gestureCount = uiState.enrollment.gestureCount + 1
            )
        )
    }

    fun finishEnrollmentSession() {
        val accessibilityMode = uiState.enrollment.isAccessibilityMode
        val previousState = uiState.authState
        val updatedSessions = uiState.sessionsCompleted + 1
        if (updatedSessions >= 3 && baselineBuilder.canBuildProfile()) {
            val profile = baselineBuilder.build()
            baselineRepository.saveProfile(profile)
            currentProfile = profile
        }

        val hasProfile = baselineRepository.hasProfile()
        val nextAuthState = if (hasProfile) AuthState.TRUSTED else AuthState.NOT_ENROLLED

        repository.setSessionsCompleted(updatedSessions)
        repository.setAuthState(nextAuthState)
        if (previousState != nextAuthState) {
            auditLogger.log(
                AuditEvent.StateChanged(
                    timestamp = System.currentTimeMillis(),
                    fromState = previousState.name,
                    toState = nextAuthState.name,
                    reason = "Enrollment session completed"
                )
            )
        }

        uiState = uiState.copy(
            authState = nextAuthState,
            sessionsCompleted = updatedSessions,
            hasProfile = hasProfile,
            enrollment = EnrollmentUiState(
                currentPassageIndex = (uiState.enrollment.currentPassageIndex + 1) % 3
            ),
            dataSizeText = repository.approximateDataSizeText()
        )
        activeMode = ActiveMode.IDLE
        auditLogger.log(
            AuditEvent.EnrollmentCompleted(
                timestamp = System.currentTimeMillis(),
                sessionCount = updatedSessions,
                isAccessibilityMode = accessibilityMode
            )
        )
    }

    fun startTestSession() {
        currentProfile = baselineRepository.loadProfile()
        if (currentProfile == null) return

        val previousState = uiState.authState
        repository.setAuthState(AuthState.TRUSTED)
        if (previousState != AuthState.TRUSTED) {
            auditLogger.log(
                AuditEvent.StateChanged(
                    timestamp = System.currentTimeMillis(),
                    fromState = previousState.name,
                    toState = AuthState.TRUSTED.name,
                    reason = "Test session started"
                )
            )
        }
        uiState = uiState.copy(
            authState = AuthState.TRUSTED,
            test = TestUiState(state = AuthState.TRUSTED),
            stepUp = StepUpUiState(),
            dataSizeText = repository.approximateDataSizeText()
        )
        testController = createTestController()
        lastTestVector = null
        testController.snapshot()
        activeMode = ActiveMode.TESTING
        auditLogger.log(AuditEvent.TestSessionStarted(System.currentTimeMillis()))
    }

    fun selectSessionLabel(label: SessionLabel) {
        uiState = uiState.copy(
            test = uiState.test.copy(selectedLabel = label)
        )
    }

    fun updateTestText(newText: String) {
        val oldText = uiState.test.typedText
        keystrokeCaptureSource.handleComposeTextChange(oldText, newText) { event ->
            testController.submitKeystroke(event)
        }
        val addedCharacters = (newText.length - oldText.length).coerceAtLeast(0)
        uiState = uiState.copy(
            test = uiState.test.copy(
                typedText = newText,
                keystrokeCount = uiState.test.keystrokeCount + addedCharacters
            )
        )
    }

    fun registerTestGesture(event: SwipeEvent) {
        testController.submitSwipe(event)
        uiState = uiState.copy(
            test = uiState.test.copy(gestureCount = uiState.test.gestureCount + 1)
        )
    }

    fun endTestSession() {
        val nextState = if (uiState.hasProfile) AuthState.TRUSTED else AuthState.NOT_ENROLLED
        val previousState = uiState.authState
        repository.setAuthState(nextState)
        if (previousState != nextState) {
            auditLogger.log(
                AuditEvent.StateChanged(
                    timestamp = System.currentTimeMillis(),
                    fromState = previousState.name,
                    toState = nextState.name,
                    reason = "Test session ended"
                )
            )
        }
        uiState = uiState.copy(
            authState = nextState,
            test = TestUiState(state = nextState.takeIf { uiState.hasProfile }),
            stepUp = StepUpUiState(),
            dataSizeText = repository.approximateDataSizeText()
        )
        activeMode = ActiveMode.IDLE
        auditLogger.log(AuditEvent.TestSessionEnded(System.currentTimeMillis()))
    }

    fun updatePinInput(value: String) {
        uiState = uiState.copy(
            stepUp = uiState.stepUp.copy(pinInput = value.filter(Char::isDigit).take(6))
        )
    }

    fun verifyStepUpPin(): PinVerificationResult {
        val pin = uiState.stepUp.pinInput
        if (pin.length !in 4..6) {
            auditLogger.log(
                AuditEvent.PinAttempted(
                    timestamp = System.currentTimeMillis(),
                    success = false,
                    attemptsRemaining = uiState.stepUp.attemptsRemaining
                )
            )
            return PinVerificationResult.InvalidFormat
        }

        return if (repository.verifyPin(pin)) {
            val previousState = uiState.authState
            repository.setAuthState(AuthState.TRUSTED)
            auditLogger.log(
                AuditEvent.PinAttempted(
                    timestamp = System.currentTimeMillis(),
                    success = true,
                    attemptsRemaining = uiState.stepUp.attemptsRemaining
                )
            )
            if (previousState != AuthState.TRUSTED) {
                auditLogger.log(
                    AuditEvent.StateChanged(
                        timestamp = System.currentTimeMillis(),
                        fromState = previousState.name,
                        toState = AuthState.TRUSTED.name,
                        reason = "Successful step-up PIN verification"
                    )
                )
            }
            uiState = uiState.copy(
                authState = AuthState.TRUSTED,
                test = uiState.test.copy(
                    state = AuthState.TRUSTED,
                    trustScore = (uiState.test.trustScore ?: 78).coerceAtLeast(78),
                    meanZScore = 0.9,
                    confidence = (uiState.test.confidence ?: 0.5).coerceAtLeast(0.5),
                    anomalyFactors = emptyList(),
                    windowLogs = (uiState.test.windowLogs + "Step-up successful -> TRUSTED").takeLast(8)
                ),
                stepUp = StepUpUiState(),
                dataSizeText = repository.approximateDataSizeText()
            )
            PinVerificationResult.Success
        } else {
            val remainingAttempts = (uiState.stepUp.attemptsRemaining - 1).coerceAtLeast(0)
            auditLogger.log(
                AuditEvent.PinAttempted(
                    timestamp = System.currentTimeMillis(),
                    success = false,
                    attemptsRemaining = remainingAttempts
                )
            )
            uiState = uiState.copy(
                stepUp = uiState.stepUp.copy(
                    pinInput = "",
                    attemptsRemaining = remainingAttempts
                )
            )

            if (remainingAttempts == 0) {
                val previousState = uiState.authState
                repository.setAuthState(AuthState.LOCKED)
                if (previousState != AuthState.LOCKED) {
                    auditLogger.log(
                        AuditEvent.StateChanged(
                            timestamp = System.currentTimeMillis(),
                            fromState = previousState.name,
                            toState = AuthState.LOCKED.name,
                            reason = "PIN attempts exhausted"
                        )
                    )
                }
                uiState = uiState.copy(
                    authState = AuthState.LOCKED,
                    test = TestUiState(),
                    dataSizeText = repository.approximateDataSizeText()
                )
                activeMode = ActiveMode.IDLE
                PinVerificationResult.FailedLockedOut
            } else {
                PinVerificationResult.FailedRetry
            }
        }
    }

    fun exitLockedSessionToHome() {
        val previousState = uiState.authState
        repository.setAuthState(AuthState.LOCKED)
        if (previousState != AuthState.LOCKED) {
            auditLogger.log(
                AuditEvent.StateChanged(
                    timestamp = System.currentTimeMillis(),
                    fromState = previousState.name,
                    toState = AuthState.LOCKED.name,
                    reason = "Locked session exited to home"
                )
            )
        }
        uiState = uiState.copy(
            authState = AuthState.LOCKED,
            test = TestUiState(),
            stepUp = StepUpUiState(),
            dataSizeText = repository.approximateDataSizeText()
        )
        activeMode = ActiveMode.IDLE
    }

    fun updateSensitivity(sliderValue: Float) {
        val sensitivity = SensitivitySetting.fromSlider(sliderValue)
        repository.setSensitivity(sensitivity)
        if (sensitivity != uiState.sensitivity) {
            auditLogger.log(
                AuditEvent.SensitivityChanged(
                    timestamp = System.currentTimeMillis(),
                    newLevel = sensitivity.title
                )
            )
        }
        uiState = uiState.copy(
            sensitivity = sensitivity,
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    fun resetProfile() {
        val previousState = uiState.authState
        repository.resetProfile()
        baselineRepository.clearProfile()
        baselineBuilder.reset()
        featureRecordStore.clear()
        enrollmentVectorsForProfile.clear()
        currentProfile = null
        auditLogger.log(AuditEvent.ProfileReset(System.currentTimeMillis()))
        if (previousState != AuthState.NOT_ENROLLED) {
            auditLogger.log(
                AuditEvent.StateChanged(
                    timestamp = System.currentTimeMillis(),
                    fromState = previousState.name,
                    toState = AuthState.NOT_ENROLLED.name,
                    reason = "Profile reset"
                )
            )
        }
        uiState = uiState.copy(
            authState = AuthState.NOT_ENROLLED,
            sessionsCompleted = 0,
            hasProfile = false,
            enrollment = EnrollmentUiState(),
            test = TestUiState(),
            stepUp = StepUpUiState(),
            dataSizeText = repository.approximateDataSizeText()
        )
        activeMode = ActiveMode.IDLE
        enrollmentController = createEnrollmentController()
        testController = createTestController()
        lastEnrollmentVector = null
        lastTestVector = null
    }

    fun verifyAuditChain(): ChainVerificationResult = auditLogger.verifyChain()

    fun auditLogEntryCount(): Int = auditLogger.entryCount()

    fun exportFeaturesCsv(): File? {
        val records = featureRecordStore.snapshot()
        if (records.isEmpty()) return null

        val file = CsvExporter(getApplication()).export(records)
        auditLogger.log(
            AuditEvent.DataExported(
                timestamp = System.currentTimeMillis(),
                recordCount = records.size
            )
        )
        return file
    }

    fun featureRecordCount(): Int = featureRecordStore.count()

    private fun loadInitialState() {
        val sessionsCompleted = repository.getSessionsCompleted()
        currentProfile = baselineRepository.loadProfile()
        val hasProfile = currentProfile != null
        val storedAuthState = repository.getAuthState()
        val normalizedAuthState = when {
            !hasProfile -> AuthState.NOT_ENROLLED
            storedAuthState == AuthState.NOT_ENROLLED -> AuthState.TRUSTED
            else -> storedAuthState
        }

        if (normalizedAuthState != storedAuthState) {
            repository.setAuthState(normalizedAuthState)
        }

        uiState = AppUiState(
            isReady = true,
            hasConsented = repository.hasConsent(),
            authState = normalizedAuthState,
            sessionsCompleted = sessionsCompleted,
            hasProfile = hasProfile,
            sensitivity = repository.getSensitivity(),
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    private fun startWindowTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1_500L)
                val now = System.currentTimeMillis()
                when (activeMode) {
                    ActiveMode.ENROLLING -> enrollmentController.checkForTimeBasedClose(now)
                    ActiveMode.TESTING -> testController.checkForTimeBasedClose(now)
                    ActiveMode.IDLE -> Unit
                }
            }
        }
    }

    private fun createEnrollmentController(): FeatureWindowController {
        return FeatureWindowController { featureVector ->
            lastEnrollmentVector = featureVector
            baselineBuilder.addVector(featureVector)
            enrollmentVectorsForProfile.add(featureVector)
            addFeatureRecord(featureVector, SESSION_TYPE_ENROLLMENT)
            Log.d(
                FEATURE_WINDOW_LOG_TAG,
                "Enrollment window closed: $featureVector"
            )
        }
    }

    private fun createTestController(): FeatureWindowController {
        return FeatureWindowController { featureVector ->
            lastTestVector = featureVector
            addFeatureRecord(featureVector, SESSION_TYPE_TEST)
            Log.d(
                FEATURE_WINDOW_LOG_TAG,
                "Test window closed: $featureVector"
            )
            scoreClosedTestWindow(featureVector)
        }
    }

    private fun addFeatureRecord(
        featureVector: FeatureVector,
        fallbackSessionType: String
    ) {
        val sessionType = when (activeMode) {
            ActiveMode.ENROLLING -> SESSION_TYPE_ENROLLMENT
            ActiveMode.TESTING -> SESSION_TYPE_TEST
            ActiveMode.IDLE -> fallbackSessionType
        }
        val sessionId = "session_${featureVector.windowStartMs}"
        featureRecordStore.add(
            FeatureRecord.fromFeatureVector(
                vector = featureVector,
                userId = EXPORT_USER_ID,
                sessionId = sessionId,
                sessionType = sessionType
            )
        )
    }

    private fun scoreClosedTestWindow(featureVector: FeatureVector) {
        val profile = currentProfile ?: return
        val sensitivity = uiState.sensitivity
        val result = ZScoreEngine(profile).score(
            vector = featureVector,
            zScoreMultiplier = sensitivity.zScoreMultiplier
        )
        val isSuspicious = result.trustScore < sensitivity.suspiciousThreshold
        val displayState = authStateFromTrustScore(result.trustScore, sensitivity)
        val previousState = uiState.authState
        val persistedAuthState = if (result.confidence >= 0.5 && uiState.hasProfile) {
            displayState
        } else {
            uiState.authState
        }

        auditLogger.log(
            AuditEvent.WindowScored(
                timestamp = System.currentTimeMillis(),
                trustScore = result.trustScore,
                confidence = result.confidence,
                isSuspicious = isSuspicious
            )
        )

        if (result.confidence >= 0.5 && uiState.hasProfile && previousState != persistedAuthState) {
            auditLogger.log(
                AuditEvent.StateChanged(
                    timestamp = System.currentTimeMillis(),
                    fromState = previousState.name,
                    toState = persistedAuthState.name,
                    reason = "Trust score ${result.trustScore} with suspicious threshold ${sensitivity.suspiciousThreshold}"
                )
            )
        }

        if (result.confidence >= 0.5 && uiState.hasProfile) {
            repository.setAuthState(persistedAuthState)
        }

        val windowNumber = uiState.test.lastLoggedWindow + 1
        val confidenceText = String.format(Locale.US, "%.2f", result.confidence)
        val statusText = if (isSuspicious) "SUSPICIOUS" else "OK"
        val logEntry = "Window #$windowNumber: score=${result.trustScore} " +
            "conf=$confidenceText $statusText"

        uiState = uiState.copy(
            authState = persistedAuthState,
            test = uiState.test.copy(
                trustScore = result.trustScore,
                meanZScore = result.meanZScore,
                confidence = result.confidence,
                state = displayState,
                anomalyFactors = result.topAnomalies,
                windowLogs = (uiState.test.windowLogs + logEntry).takeLast(8),
                lastLoggedWindow = windowNumber
            ),
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    private fun authStateFromTrustScore(
        trustScore: Int,
        sensitivity: SensitivitySetting
    ): AuthState {
        val lockedThreshold = (sensitivity.suspiciousThreshold - 10).coerceAtLeast(0)
        return when {
            trustScore < lockedThreshold -> AuthState.LOCKED
            trustScore < sensitivity.suspiciousThreshold -> AuthState.UNCERTAIN
            else -> AuthState.TRUSTED
        }
    }

    private enum class ActiveMode {
        IDLE,
        ENROLLING,
        TESTING
    }
}

enum class PinVerificationResult {
    Success,
    FailedRetry,
    FailedLockedOut,
    InvalidFormat
}
