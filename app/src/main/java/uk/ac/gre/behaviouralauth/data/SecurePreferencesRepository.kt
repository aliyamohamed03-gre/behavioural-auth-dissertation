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

