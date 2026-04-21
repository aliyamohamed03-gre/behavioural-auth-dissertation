# Behavioural Auth Claude Export

## Purpose
This file is a single-document handoff for Claude. It contains the project brief, design decisions, Phase 1 requirements, current implementation notes, and a source snapshot of the current Android project.

## Project Summary
- Project: Android behavioural biometric authentication prototype for a BSc Computer Security and Forensics final year project.
- Platform: Android Emulator first, Kotlin, Android Studio, Jetpack Compose UI.
- Core idea: capture keystroke timing and swipe dynamics, build a behavioural profile during enrollment, then score live behaviour continuously during authentication.
- Continuous scoring model: sliding windows, per-feature z-scores against a baseline, trust score from 0 to 100, and state transitions Trusted -> Uncertain -> Locked.
- Security direction: encrypted local storage, integrity verification, tamper-evident audit trail, hashed step-up PIN, anti-replay nonce.
- Accessibility direction: TalkBack support, 16sp+ text, 48dp+ targets, font scaling, text-plus-colour state signalling, sensitivity controls, and accessibility-aware tolerance settings.

## Feature Architecture
### Tier 1 core features used for real-time scoring
- meanInterKeyInterval
- stdInterKeyInterval
- deleteRatio
- typingSpeed
- meanSwipeVelocity
- stdSwipeVelocity
- meanSwipeDuration
- meanSwipeDistance

### Tier 2 exploratory features for offline analysis only
- medianInterKeyInterval
- meanHoldTime
- stdHoldTime
- medianHoldTime
- stdSwipeDuration
- stdSwipeDistance
- meanSwipeCurvature

## Measurement Decisions
- Keystroke timing uses text change detection, not key down or key up events.
- Hold time is optional Tier 2 data and may be unavailable or zero on Android soft keyboards.
- Emulator touch pressure may be fixed or zero and must never be required for scoring.

## Enrollment and Profile Design
- Enrollment requires at least 3 sessions.
- Baseline stores both mean and standard deviation for each feature.
- An adaptive profile copy will later update using EMA with alpha 0.1.
- Original enrollment baseline remains preserved.

## Data Storage and Export Design
- Raw interaction events and derived feature vectors are stored separately.
- Planned CSV exports:
  - keystroke_events.csv
  - gesture_events.csv
  - feature_vectors.csv
  - auth_scores.csv
  - state_changes.csv
  - profile_baseline.csv
- Rigorous ML classification will happen offline in Python using Random Forest, KNN, and Gradient Boosting.

## Scoring Design
- z = |observed - baseline_mean| / max(baseline_std, 0.001)
- trust score = max(0, 100 - (mean_z_score * 20)), clamped to 0..100
- top 3 contributing features should be identified per scored window
- anomaly types: typing_rhythm, typing_speed, gesture, mixed

## Sliding Window Design
- Window closes on any of:
  - 20 keystrokes
  - 5 gestures
  - 30 seconds with at least 5 events
  - 10 seconds inactivity with at least 5 events
- Window confidence = min(1.0, eventCount / 20)
- Windows with confidence below 0.5 are shown but should not drive state transitions.

## State Machine Design
- NOT_ENROLLED -> TRUSTED after 3 enrollment sessions complete
- TRUSTED -> UNCERTAIN after 2 consecutive suspicious windows with confidence >= 0.5
- UNCERTAIN -> LOCKED after 3 more consecutive suspicious windows, 5 total
- UNCERTAIN -> TRUSTED after 2 consecutive normal windows
- LOCKED -> TRUSTED only via successful step-up PIN authentication

## Step-Up and Lockout Design
- LOCKED navigates to a PIN challenge screen.
- User gets 3 attempts.
- Correct PIN resets state to TRUSTED and returns user to the test session.
- 3 failed attempts end the session and navigate Home.
- Step-up back button must go Home, not back to Test.
- Planned escalating lockouts:
  - first: 30 seconds
  - second: 2 minutes
  - third: 10 minutes

## Accessibility Requirements
- Minimum 16sp text.
- Minimum 48dp interactive targets.
- Interactive elements need labels or content descriptions.
- State must use colour plus text or icon.
- Support system font scaling.
- Sensitivity slider changes tolerance and suspicious-window thresholds.
- Accessibility flag during enrollment will later widen variance and use more forgiving smoothing.

## Phase 1 Request
Create the project skeleton with these screens and navigation:
- HomeScreen
- EnrollmentScreen
- TestScreen
- StepUpScreen
- SettingsScreen
- ConsentScreen shown on first launch only

## Phase 1 Implementation Status
- Implemented as a Jetpack Compose app.
- Added navigation between Consent, Home, Enrollment, Test, StepUp, and Settings.
- Added encrypted shared preferences for consent, sensitivity, session count, auth state, and a temporary test PIN hash.
- Home banner reflects auth state using text plus symbol and colour.
- Enrollment includes passage cycling, multiline input, swipe area, counters, and progress bar.
- Test includes session labels, live mock trust display, confidence, anomaly panel, and recent window log.
- Step-up PIN screen verifies against a temporary Phase 1 PIN.
- Settings includes sensitivity slider, export placeholder button, reset confirmation dialog, data section, and about section.
- Consent screen appears only before consent is stored locally.

## Phase 1 Testing Notes
- Temporary test PIN for the current skeleton: 1234
- Start Test Session becomes enabled after 3 enrollment sessions.
- In Test, choosing Simulated Impersonation and generating about 10 total inputs will trigger the mock LOCKED flow and open StepUp.
- This Phase 1 app uses mock scoring logic so the navigation and screen states can be tested before the real behavioural engine is built.

## Known Constraints In This Snapshot
- I could not run a Gradle build in the current environment because Gradle and Android SDK tooling were not available here.
- The project is intended to be opened and synced in Android Studio for emulator verification.
- CSV export, real feature extraction, real scoring, audit chaining, HMAC integrity, and full lockout timing are not implemented yet.

## Source Snapshot
Below is the current project source.

## File: .gitignore

`$extension
.gradle/
.idea/
build/
app/build/
captures/
local.properties
*.iml
*.log
.DS_Store

```

## File: settings.gradle.kts

`$extension
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BehaviouralAuth"
include(":app")

```

## File: build.gradle.kts

`$extension
plugins {
    id("com.android.application") version "8.4.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

```

## File: gradle.properties

`$extension
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official

```

## File: gradle\wrapper\gradle-wrapper.properties

`$extension
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.6-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

```

## File: app\build.gradle.kts

`$extension
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uk.ac.gre.behaviouralauth"
    compileSdk = 34

    defaultConfig {
        applicationId = "uk.ac.gre.behaviouralauth"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.security:security-crypto:1.0.0")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

## File: app\proguard-rules.pro

`$extension
# Phase 1 keeps default ProGuard settings.

```

## File: app\src\main\AndroidManifest.xml

`$extension
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.BehaviouralAuth">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>

```

## File: app\src\main\res\values\strings.xml

`$extension
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Behavioural Auth</string>
</resources>

```

## File: app\src\main\res\values\themes.xml

`$extension
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.BehaviouralAuth" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/white</item>
        <item name="android:windowLightStatusBar" tools:targetApi="m">true</item>
    </style>
</resources>
```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\MainActivity.kt

`$extension
package uk.ac.gre.behaviouralauth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import uk.ac.gre.behaviouralauth.ui.theme.BehaviouralAuthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BehaviouralAuthTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BehaviouralAuthApp()
                }
            }
        }
    }
}

```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\BehaviouralAuthApp.kt

`$extension
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

@Composable
fun BehaviouralAuthApp(viewModel: AppViewModel = viewModel()) {
    val uiState = viewModel.uiState
    val navController = rememberNavController()
    val context = LocalContext.current

    if (!uiState.isReady) {
        LoadingScreen()
        return
    }

    val startDestination = if (uiState.hasConsented) {
        AppDestination.Home.route
    } else {
        AppDestination.Consent.route
    }

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
                onResetProfile = viewModel::resetProfile
            )
        }
    }
}
```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\data\SecurePreferencesRepository.kt

`$extension
package uk.ac.gre.behaviouralauth.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest
import java.util.Locale
import uk.ac.gre.behaviouralauth.model.AuthState
import uk.ac.gre.behaviouralauth.model.SensitivitySetting

@Suppress("DEPRECATION")
class SecurePreferencesRepository(context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFERENCES_FILE,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        ensureDefaults()
    }

    fun hasConsent(): Boolean = prefs.getBoolean(KEY_CONSENT, false)

    fun setConsent(value: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT, value).apply()
    }

    fun getSensitivity(): SensitivitySetting {
        val storedValue = prefs.getInt(KEY_SENSITIVITY, SensitivitySetting.MEDIUM.storageValue)
        return SensitivitySetting.fromStorage(storedValue)
    }

    fun setSensitivity(setting: SensitivitySetting) {
        prefs.edit().putInt(KEY_SENSITIVITY, setting.storageValue).apply()
    }

    fun getSessionsCompleted(): Int = prefs.getInt(KEY_SESSIONS_COMPLETED, 0)

    fun setSessionsCompleted(value: Int) {
        prefs.edit().putInt(KEY_SESSIONS_COMPLETED, value).apply()
    }

    fun getAuthState(): AuthState {
        val rawState = prefs.getString(KEY_AUTH_STATE, AuthState.NOT_ENROLLED.name)
        return runCatching { AuthState.valueOf(rawState.orEmpty()) }
            .getOrDefault(AuthState.NOT_ENROLLED)
    }

    fun setAuthState(authState: AuthState) {
        prefs.edit().putString(KEY_AUTH_STATE, authState.name).apply()
    }

    fun verifyPin(pin: String): Boolean {
        return sha256(pin) == prefs.getString(KEY_PIN_HASH, "")
    }

    fun resetProfile() {
        prefs.edit()
            .putInt(KEY_SESSIONS_COMPLETED, 0)
            .putString(KEY_AUTH_STATE, AuthState.NOT_ENROLLED.name)
            .apply()
    }

    fun approximateDataSizeText(): String {
        val byteCount = prefs.all.entries.sumOf { entry ->
            entry.key.length + (entry.value?.toString()?.length ?: 0)
        }.coerceAtLeast(1)

        return if (byteCount < 1024) {
            "$byteCount B"
        } else {
            String.format(Locale.US, "%.1f KB", byteCount / 1024.0)
        }
    }

    private fun ensureDefaults() {
        if (!prefs.contains(KEY_PIN_HASH)) {
            prefs.edit().putString(KEY_PIN_HASH, sha256(DEFAULT_PIN)).apply()
        }
    }

    private fun sha256(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val PREFERENCES_FILE = "behavioural_auth_secure_prefs"
        const val KEY_CONSENT = "consent_given"
        const val KEY_SENSITIVITY = "sensitivity_setting"
        const val KEY_SESSIONS_COMPLETED = "sessions_completed"
        const val KEY_AUTH_STATE = "auth_state"
        const val KEY_PIN_HASH = "pin_hash"
        const val DEFAULT_PIN = "1234"
    }
}

```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\model\AuthModels.kt

`$extension
package uk.ac.gre.behaviouralauth.model

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import uk.ac.gre.behaviouralauth.ui.theme.ScoreAmber
import uk.ac.gre.behaviouralauth.ui.theme.ScoreGreen
import uk.ac.gre.behaviouralauth.ui.theme.ScoreRed
import uk.ac.gre.behaviouralauth.ui.theme.StatusGreyBackground
import uk.ac.gre.behaviouralauth.ui.theme.StatusGreyText
import uk.ac.gre.behaviouralauth.ui.theme.StatusLockedBackground
import uk.ac.gre.behaviouralauth.ui.theme.StatusLockedText
import uk.ac.gre.behaviouralauth.ui.theme.StatusTrustedBackground
import uk.ac.gre.behaviouralauth.ui.theme.StatusTrustedText
import uk.ac.gre.behaviouralauth.ui.theme.StatusUncertainBackground
import uk.ac.gre.behaviouralauth.ui.theme.StatusUncertainText

enum class AuthState(
    val label: String,
    val symbol: String,
    val bannerBackground: Color,
    val bannerText: Color
) {
    NOT_ENROLLED("Not Enrolled", "i", StatusGreyBackground, StatusGreyText),
    TRUSTED("Trusted", "✓", StatusTrustedBackground, StatusTrustedText),
    UNCERTAIN("Uncertain", "⚠", StatusUncertainBackground, StatusUncertainText),
    LOCKED("Locked", "🔒", StatusLockedBackground, StatusLockedText)
}

enum class SensitivitySetting(
    val title: String,
    val suspiciousWindowsBeforeChange: Int,
    val storageValue: Int
) {
    LOW("Low", 4, 0),
    MEDIUM("Medium", 2, 1),
    HIGH("High", 1, 2);

    companion object {
        fun fromStorage(value: Int): SensitivitySetting {
            return entries.firstOrNull { it.storageValue == value } ?: MEDIUM
        }

        fun fromSlider(value: Float): SensitivitySetting {
            return when (value.roundToInt().coerceIn(0, 2)) {
                0 -> LOW
                2 -> HIGH
                else -> MEDIUM
            }
        }
    }
}

enum class SessionLabel(val displayName: String) {
    NORMAL("Normal"),
    ALTERED_SPEED("Altered Speed"),
    DIFFERENT_HAND("Different Hand"),
    SIMULATED_IMPERSONATION("Simulated Impersonation"),
    ACCESSIBILITY_SIMULATION("Accessibility Simulation")
}

data class AnomalyFactor(
    val featureName: String,
    val zScore: Double
)

data class EnrollmentUiState(
    val currentPassageIndex: Int = 0,
    val typedText: String = "",
    val keystrokeCount: Int = 0,
    val gestureCount: Int = 0
)

data class TestUiState(
    val selectedLabel: SessionLabel = SessionLabel.NORMAL,
    val typedText: String = "",
    val keystrokeCount: Int = 0,
    val gestureCount: Int = 0,
    val trustScore: Int? = null,
    val meanZScore: Double? = null,
    val confidence: Double? = null,
    val state: AuthState? = null,
    val anomalyFactors: List<AnomalyFactor> = emptyList(),
    val windowLogs: List<String> = emptyList(),
    val lastLoggedWindow: Int = 0
)

data class StepUpUiState(
    val pinInput: String = "",
    val attemptsRemaining: Int = 3
)

data class AppUiState(
    val isReady: Boolean = false,
    val hasConsented: Boolean = false,
    val authState: AuthState = AuthState.NOT_ENROLLED,
    val sessionsCompleted: Int = 0,
    val hasProfile: Boolean = false,
    val sensitivity: SensitivitySetting = SensitivitySetting.MEDIUM,
    val dataSizeText: String = "0 B",
    val enrollment: EnrollmentUiState = EnrollmentUiState(),
    val test: TestUiState = TestUiState(),
    val stepUp: StepUpUiState = StepUpUiState()
)

val EnrollmentPassages = listOf(
    "The quick brown fox jumps over the lazy dog. In the world of cybersecurity, authentication is the process of verifying identity. Smartphone usage has increased dramatically, creating new security challenges.",
    "Behavioural biometrics measures how someone interacts with a device over time. Typing rhythm and swipe patterns can support secure authentication without interrupting normal phone usage.",
    "Continuous authentication checks for changes in user behaviour after login. A reliable prototype should balance usability, accessibility, and strong protection against impersonation attempts."
)

fun trustScoreColor(score: Int?): Color {
    return when {
        score == null -> StatusGreyText
        score > 70 -> ScoreGreen
        score >= 40 -> ScoreAmber
        else -> ScoreRed
    }
}

```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\AppViewModel.kt

`$extension
package uk.ac.gre.behaviouralauth.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlin.math.roundToInt
import uk.ac.gre.behaviouralauth.data.SecurePreferencesRepository
import uk.ac.gre.behaviouralauth.model.AnomalyFactor
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.model.AuthState
import uk.ac.gre.behaviouralauth.model.EnrollmentUiState
import uk.ac.gre.behaviouralauth.model.SensitivitySetting
import uk.ac.gre.behaviouralauth.model.SessionLabel
import uk.ac.gre.behaviouralauth.model.StepUpUiState
import uk.ac.gre.behaviouralauth.model.TestUiState

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SecurePreferencesRepository(application.applicationContext)

    var uiState by mutableStateOf(AppUiState())
        private set

    init {
        loadInitialState()
    }

    fun grantConsent() {
        repository.setConsent(true)
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

    fun updateEnrollmentText(newText: String) {
        val oldText = uiState.enrollment.typedText
        val addedCharacters = (newText.length - oldText.length).coerceAtLeast(0)
        uiState = uiState.copy(
            enrollment = uiState.enrollment.copy(
                typedText = newText,
                keystrokeCount = uiState.enrollment.keystrokeCount + addedCharacters
            )
        )
    }

    fun registerEnrollmentGesture() {
        uiState = uiState.copy(
            enrollment = uiState.enrollment.copy(
                gestureCount = uiState.enrollment.gestureCount + 1
            )
        )
    }

    fun finishEnrollmentSession() {
        val updatedSessions = uiState.sessionsCompleted + 1
        val hasProfile = updatedSessions >= 3
        val nextAuthState = if (hasProfile) AuthState.TRUSTED else AuthState.NOT_ENROLLED

        repository.setSessionsCompleted(updatedSessions)
        repository.setAuthState(nextAuthState)

        uiState = uiState.copy(
            authState = nextAuthState,
            sessionsCompleted = updatedSessions,
            hasProfile = hasProfile,
            enrollment = EnrollmentUiState(
                currentPassageIndex = (uiState.enrollment.currentPassageIndex + 1) % 3
            ),
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    fun startTestSession() {
        if (!uiState.hasProfile) return

        repository.setAuthState(AuthState.TRUSTED)
        uiState = uiState.copy(
            authState = AuthState.TRUSTED,
            test = TestUiState(state = AuthState.TRUSTED),
            stepUp = StepUpUiState(),
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    fun selectSessionLabel(label: SessionLabel) {
        applyMockAssessment(uiState.test.copy(selectedLabel = label))
    }

    fun updateTestText(newText: String) {
        val oldText = uiState.test.typedText
        val addedCharacters = (newText.length - oldText.length).coerceAtLeast(0)
        applyMockAssessment(
            uiState.test.copy(
                typedText = newText,
                keystrokeCount = uiState.test.keystrokeCount + addedCharacters
            )
        )
    }

    fun registerTestGesture() {
        applyMockAssessment(
            uiState.test.copy(gestureCount = uiState.test.gestureCount + 1)
        )
    }

    fun endTestSession() {
        val nextState = if (uiState.hasProfile) AuthState.TRUSTED else AuthState.NOT_ENROLLED
        repository.setAuthState(nextState)
        uiState = uiState.copy(
            authState = nextState,
            test = TestUiState(state = nextState.takeIf { uiState.hasProfile }),
            stepUp = StepUpUiState(),
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    fun updatePinInput(value: String) {
        uiState = uiState.copy(
            stepUp = uiState.stepUp.copy(pinInput = value.filter(Char::isDigit).take(6))
        )
    }

    fun verifyStepUpPin(): PinVerificationResult {
        val pin = uiState.stepUp.pinInput
        if (pin.length !in 4..6) return PinVerificationResult.InvalidFormat

        return if (repository.verifyPin(pin)) {
            repository.setAuthState(AuthState.TRUSTED)
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
            uiState = uiState.copy(
                stepUp = uiState.stepUp.copy(
                    pinInput = "",
                    attemptsRemaining = remainingAttempts
                )
            )

            if (remainingAttempts == 0) {
                repository.setAuthState(AuthState.LOCKED)
                uiState = uiState.copy(
                    authState = AuthState.LOCKED,
                    test = TestUiState(),
                    dataSizeText = repository.approximateDataSizeText()
                )
                PinVerificationResult.FailedLockedOut
            } else {
                PinVerificationResult.FailedRetry
            }
        }
    }

    fun exitLockedSessionToHome() {
        repository.setAuthState(AuthState.LOCKED)
        uiState = uiState.copy(
            authState = AuthState.LOCKED,
            test = TestUiState(),
            stepUp = StepUpUiState(),
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    fun updateSensitivity(sliderValue: Float) {
        val sensitivity = SensitivitySetting.fromSlider(sliderValue)
        repository.setSensitivity(sensitivity)
        uiState = uiState.copy(
            sensitivity = sensitivity,
            dataSizeText = repository.approximateDataSizeText()
        )

        if (uiState.test.keystrokeCount + uiState.test.gestureCount > 0) {
            applyMockAssessment(uiState.test)
        }
    }

    fun resetProfile() {
        repository.resetProfile()
        uiState = uiState.copy(
            authState = AuthState.NOT_ENROLLED,
            sessionsCompleted = 0,
            hasProfile = false,
            enrollment = EnrollmentUiState(),
            test = TestUiState(),
            stepUp = StepUpUiState(),
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    private fun loadInitialState() {
        val sessionsCompleted = repository.getSessionsCompleted()
        val hasProfile = sessionsCompleted >= 3
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

    private fun applyMockAssessment(baseState: TestUiState) {
        val totalEvents = baseState.keystrokeCount + baseState.gestureCount
        if (totalEvents == 0) {
            uiState = uiState.copy(
                test = baseState.copy(
                    trustScore = null,
                    meanZScore = null,
                    confidence = null,
                    state = if (uiState.hasProfile) AuthState.TRUSTED else null,
                    anomalyFactors = emptyList(),
                    windowLogs = emptyList(),
                    lastLoggedWindow = 0
                )
            )
            return
        }

        val assessment = createMockAssessment(baseState.selectedLabel, totalEvents)
        val updatedWindowCount = (totalEvents / 10).coerceAtLeast(1)
        val updatedLogs = buildList {
            addAll(baseState.windowLogs)
            if (updatedWindowCount > baseState.lastLoggedWindow) {
                for (windowIndex in (baseState.lastLoggedWindow + 1)..updatedWindowCount) {
                    val confidenceLabel = if (assessment.confidence >= 0.5f) {
                        "high confidence"
                    } else {
                        "low confidence"
                    }
                    add(
                        "Window $windowIndex: Score ${assessment.score} -> " +
                            "${assessment.displayState.label.uppercase()} ($confidenceLabel)"
                    )
                }
            }
        }.takeLast(8)

        val persistedAuthState = if (assessment.confidence >= 0.5f && uiState.hasProfile) {
            assessment.displayState
        } else {
            uiState.authState
        }

        if (uiState.hasProfile && assessment.confidence >= 0.5f) {
            repository.setAuthState(persistedAuthState)
        }

        uiState = uiState.copy(
            authState = persistedAuthState,
            test = baseState.copy(
                trustScore = assessment.score,
                meanZScore = assessment.meanZScore,
                confidence = assessment.confidence.toDouble(),
                state = assessment.displayState,
                anomalyFactors = assessment.factors,
                windowLogs = updatedLogs,
                lastLoggedWindow = updatedWindowCount
            ),
            dataSizeText = repository.approximateDataSizeText()
        )
    }

    private fun createMockAssessment(
        sessionLabel: SessionLabel,
        totalEvents: Int
    ): MockAssessment {
        val sensitivityAdjustment = when (uiState.sensitivity) {
            SensitivitySetting.LOW -> 10
            SensitivitySetting.MEDIUM -> 0
            SensitivitySetting.HIGH -> -10
        }

        val confidence = (totalEvents / 20f).coerceAtMost(1f)

        val baseline = when (sessionLabel) {
            SessionLabel.NORMAL -> MockAssessment(
                score = 92,
                meanZScore = 0.6,
                displayState = AuthState.TRUSTED,
                factors = listOf(
                    AnomalyFactor("typingSpeed", 0.8),
                    AnomalyFactor("meanSwipeVelocity", 0.6),
                    AnomalyFactor("meanInterKeyInterval", 0.4)
                ),
                confidence = confidence
            )

            SessionLabel.ALTERED_SPEED -> MockAssessment(
                score = 58,
                meanZScore = 2.2,
                displayState = if (confidence >= 0.5f) AuthState.UNCERTAIN else AuthState.TRUSTED,
                factors = listOf(
                    AnomalyFactor("typingSpeed", 3.2),
                    AnomalyFactor("meanInterKeyInterval", 2.8),
                    AnomalyFactor("stdInterKeyInterval", 2.3)
                ),
                confidence = confidence
            )

            SessionLabel.DIFFERENT_HAND -> MockAssessment(
                score = 52,
                meanZScore = 2.6,
                displayState = if (confidence >= 0.5f) AuthState.UNCERTAIN else AuthState.TRUSTED,
                factors = listOf(
                    AnomalyFactor("meanSwipeDistance", 3.1),
                    AnomalyFactor("stdSwipeVelocity", 2.9),
                    AnomalyFactor("meanSwipeDuration", 2.4)
                ),
                confidence = confidence
            )

            SessionLabel.SIMULATED_IMPERSONATION -> MockAssessment(
                score = 28,
                meanZScore = 3.9,
                displayState = if (confidence >= 0.5f) AuthState.LOCKED else AuthState.UNCERTAIN,
                factors = listOf(
                    AnomalyFactor("meanInterKeyInterval", 4.5),
                    AnomalyFactor("meanSwipeVelocity", 4.2),
                    AnomalyFactor("typingSpeed", 3.8)
                ),
                confidence = confidence
            )

            SessionLabel.ACCESSIBILITY_SIMULATION -> MockAssessment(
                score = 68,
                meanZScore = 1.5,
                displayState = AuthState.TRUSTED,
                factors = listOf(
                    AnomalyFactor("stdInterKeyInterval", 1.8),
                    AnomalyFactor("meanSwipeDuration", 1.5),
                    AnomalyFactor("deleteRatio", 1.2)
                ),
                confidence = confidence
            )
        }

        val adjustedScore = (baseline.score + sensitivityAdjustment).coerceIn(0, 100)
        val adjustedZScore = (baseline.meanZScore - (sensitivityAdjustment / 20.0)).coerceAtLeast(0.1)
        val adjustedState = when {
            confidence < 0.5f && baseline.displayState == AuthState.LOCKED -> AuthState.UNCERTAIN
            adjustedScore < 40 && confidence >= 0.5f -> AuthState.LOCKED
            adjustedScore in 40..70 && confidence >= 0.5f -> AuthState.UNCERTAIN
            else -> AuthState.TRUSTED
        }

        return baseline.copy(
            score = adjustedScore,
            meanZScore = (adjustedZScore * 10).roundToInt() / 10.0,
            displayState = adjustedState
        )
    }

    private data class MockAssessment(
        val score: Int,
        val meanZScore: Double,
        val displayState: AuthState,
        val factors: List<AnomalyFactor>,
        val confidence: Float
    )
}

enum class PinVerificationResult {
    Success,
    FailedRetry,
    FailedLockedOut,
    InvalidFormat
}
```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\navigation\AppDestination.kt

`$extension
package uk.ac.gre.behaviouralauth.ui.navigation

sealed class AppDestination(val route: String) {
    data object Consent : AppDestination("consent")
    data object Home : AppDestination("home")
    data object Enrollment : AppDestination("enrollment")
    data object Test : AppDestination("test")
    data object StepUp : AppDestination("step_up")
    data object Settings : AppDestination("settings")
}
```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\components\CommonComponents.kt

`$extension
package uk.ac.gre.behaviouralauth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.ac.gre.behaviouralauth.model.AuthState
import uk.ac.gre.behaviouralauth.ui.theme.AppBackground
import uk.ac.gre.behaviouralauth.ui.theme.AppPrimary
import uk.ac.gre.behaviouralauth.ui.theme.AppSurface
import uk.ac.gre.behaviouralauth.ui.theme.SwipeAreaBackground
import uk.ac.gre.behaviouralauth.ui.theme.SwipeAreaBorder

@Composable
fun ScreenFrame(
    title: String,
    contentDescription: String = title,
    paddingValues: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(containerColor = AppBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .semantics { this.contentDescription = contentDescription },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = text },
        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun StatusBanner(authState: AuthState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .semantics {
                contentDescription = "Current authentication state: ${authState.label}"
            },
        colors = CardDefaults.cardColors(containerColor = authState.bannerBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = authState.symbol,
                fontSize = 32.sp,
                color = authState.bannerText
            )
            Text(
                text = authState.label,
                style = MaterialTheme.typography.headlineSmall,
                color = authState.bannerText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    contentDescription: String = title ?: "Section",
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
fun SwipeCaptureArea(
    onGestureCaptured: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SwipeAreaBackground)
            .drawBehind {
                drawRoundRect(
                    color = SwipeAreaBorder,
                    cornerRadius = CornerRadius(32f, 32f),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f), 0f)
                    )
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onGestureCaptured() },
                    onDrag = { change, _ -> change.consume() }
                )
            }
            .semantics {
                contentDescription = "Swipe capture area. Swipe here in different directions."
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Swipe here in different directions",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF1E3A5F),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\screens\ConsentScreen.kt

`$extension
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
                .heightIn(min = 56.dp)
                .semantics {
                    contentDescription = "Consent confirmation checkbox"
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = consentChecked,
                onCheckedChange = { consentChecked = it }
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
            onClick = onContinue
        )
    }
}

```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\screens\HomeScreen.kt

`$extension
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
            enabled = uiState.hasProfile
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
```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\screens\EnrollmentScreen.kt

`$extension
package uk.ac.gre.behaviouralauth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.model.EnrollmentPassages
import uk.ac.gre.behaviouralauth.ui.components.AppButton
import uk.ac.gre.behaviouralauth.ui.components.ScreenFrame
import uk.ac.gre.behaviouralauth.ui.components.SwipeCaptureArea

@Composable
fun EnrollmentScreen(
    uiState: AppUiState,
    onNextPassage: () -> Unit,
    onTextChanged: (String) -> Unit,
    onGestureCaptured: () -> Unit,
    onFinishSession: () -> Unit
) {
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
                .semantics { contentDescription = "Enrollment text input" },
            label = { Text("Type here") },
            minLines = 6,
            textStyle = MaterialTheme.typography.bodyLarge
        )

        SwipeCaptureArea(onGestureCaptured = onGestureCaptured)

        Text(
            text = "Keystrokes: ${enrollmentState.keystrokeCount} | Gestures: ${enrollmentState.gestureCount}",
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
        )

        AppButton(
            text = "Finish Session",
            onClick = onFinishSession
        )
    }
}
```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\screens\TestScreen.kt

`$extension
package uk.ac.gre.behaviouralauth.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.model.AuthState
import uk.ac.gre.behaviouralauth.model.SessionLabel
import uk.ac.gre.behaviouralauth.model.trustScoreColor
import uk.ac.gre.behaviouralauth.ui.components.AppButton
import uk.ac.gre.behaviouralauth.ui.components.ScreenFrame
import uk.ac.gre.behaviouralauth.ui.components.SectionCard
import uk.ac.gre.behaviouralauth.ui.components.SwipeCaptureArea

@Composable
fun TestScreen(
    uiState: AppUiState,
    onSelectLabel: (SessionLabel) -> Unit,
    onTextChanged: (String) -> Unit,
    onGestureCaptured: () -> Unit,
    onEndSession: () -> Unit
) {
    val testState = uiState.test
    val trustScoreText = testState.trustScore?.toString() ?: "--"
    val meanZText = testState.meanZScore?.let {
        String.format(Locale.US, "%.1f", it)
    } ?: "--"
    val confidenceText = testState.confidence?.let {
        String.format(Locale.US, "%.2f", it)
    } ?: "--"
    val stateText = testState.state?.let { "${it.symbol} ${it.label}" } ?: "--"
    val showAnomalyPanel = testState.state == AuthState.UNCERTAIN || testState.state == AuthState.LOCKED

    ScreenFrame(
        title = "Authentication Test",
        contentDescription = "Authentication test screen"
    ) {
        Text(
            text = "Session label",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SessionLabel.entries) { label ->
                FilterChip(
                    selected = label == testState.selectedLabel,
                    onClick = { onSelectLabel(label) },
                    label = {
                        Text(
                            text = label.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Session label ${label.displayName}"
                    }
                )
            }
        }

        OutlinedTextField(
            value = testState.typedText,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .semantics { contentDescription = "Authentication test text input" },
            label = { Text("Type here") },
            minLines = 6,
            textStyle = MaterialTheme.typography.bodyLarge
        )

        SwipeCaptureArea(onGestureCaptured = onGestureCaptured)

        SectionCard(title = "Live Scoring", contentDescription = "Live scoring panel") {
            Text(
                text = "Trust Score: $trustScoreText/100",
                style = MaterialTheme.typography.headlineSmall,
                color = trustScoreColor(testState.trustScore),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "(mean z = $meanZText)",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "State: $stateText",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Confidence: $confidenceText",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (showAnomalyPanel && testState.anomalyFactors.isNotEmpty()) {
            SectionCard(
                title = "Anomaly Explanation",
                contentDescription = "Anomaly explanation panel"
            ) {
                testState.anomalyFactors.getOrNull(0)?.let {
                    Text(
                        text = "Primary factor: ${it.featureName} (z = ${String.format(Locale.US, "%.1f", it.zScore)})",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                testState.anomalyFactors.getOrNull(1)?.let {
                    Text(
                        text = "Secondary factor: ${it.featureName} (z = ${String.format(Locale.US, "%.1f", it.zScore)})",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        SectionCard(
            title = "Recent Windows",
            contentDescription = "Recent window scoring log"
        ) {
            if (testState.windowLogs.isEmpty()) {
                Text(
                    text = "No windows scored yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                testState.windowLogs.forEach { entry ->
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE1E5EB), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }
            }
        }

        AppButton(
            text = "End Session",
            onClick = onEndSession
        )
    }
}

```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\screens\StepUpScreen.kt

`$extension
package uk.ac.gre.behaviouralauth.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
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
                .semantics { contentDescription = "PIN input field" },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge
        )

        AppButton(
            text = "Verify",
            onClick = onVerify
        )

        Text(
            text = "Attempts remaining: ${uiState.stepUp.attemptsRemaining}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\screens\SettingsScreen.kt

`$extension
package uk.ac.gre.behaviouralauth.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import uk.ac.gre.behaviouralauth.model.AppUiState
import uk.ac.gre.behaviouralauth.ui.components.AppButton
import uk.ac.gre.behaviouralauth.ui.components.ScreenFrame
import uk.ac.gre.behaviouralauth.ui.components.SectionCard

@Composable
fun SettingsScreen(
    uiState: AppUiState,
    onSensitivityChanged: (Float) -> Unit,
    onExportData: () -> Unit,
    onResetProfile: () -> Unit
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    ScreenFrame(
        title = "Settings",
        contentDescription = "Settings screen"
    ) {
        SectionCard(
            title = "Authentication Sensitivity",
            contentDescription = "Authentication sensitivity settings"
        ) {
            Slider(
                value = uiState.sensitivity.storageValue.toFloat(),
                onValueChange = onSensitivityChanged,
                valueRange = 0f..2f,
                steps = 1,
                modifier = Modifier.semantics {
                    contentDescription = "Authentication sensitivity slider"
                }
            )
            Text(
                text = "Current: ${uiState.sensitivity.title} — ${uiState.sensitivity.suspiciousWindowsBeforeChange} suspicious windows before state change",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Range: Low to High",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        AppButton(
            text = "Export Data",
            onClick = onExportData
        )

        AppButton(
            text = "Reset Profile",
            onClick = { showResetDialog = true }
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
}
```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\theme\Color.kt

`$extension
package uk.ac.gre.behaviouralauth.ui.theme

import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFFF7F9FC)
val AppSurface = Color(0xFFFFFFFF)
val AppPrimary = Color(0xFF1F5AA6)
val AppOnPrimary = Color(0xFFFFFFFF)

val StatusGreyBackground = Color(0xFFE0E0E0)
val StatusGreyText = Color(0xFF616161)
val StatusTrustedBackground = Color(0xFFC8E6C9)
val StatusTrustedText = Color(0xFF2E7D32)
val StatusUncertainBackground = Color(0xFFFFE0B2)
val StatusUncertainText = Color(0xFF8A5A00)
val StatusLockedBackground = Color(0xFFFFCDD2)
val StatusLockedText = Color(0xFFC62828)

val ScoreGreen = Color(0xFF2E7D32)
val ScoreAmber = Color(0xFFF57C00)
val ScoreRed = Color(0xFFC62828)

val SwipeAreaBackground = Color(0xFFE3F2FD)
val SwipeAreaBorder = Color(0xFF64B5F6)

```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\theme\Type.kt

`$extension
package uk.ac.gre.behaviouralauth.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
)

```

## File: app\src\main\java\uk\ac\gre\behaviouralauth\ui\theme\Theme.kt

`$extension
package uk.ac.gre.behaviouralauth.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BehaviouralAuthColorScheme = lightColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    background = AppBackground,
    surface = AppSurface
)

@Composable
fun BehaviouralAuthTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BehaviouralAuthColorScheme,
        typography = AppTypography,
        content = content
    )
}

```
