package uk.ac.gre.behaviouralauth.scoring

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONObject

@Suppress("DEPRECATION")
//Stores and retrieves baseline profiles using encrypted shared preferences.
class BaselineRepository(context: Context) {

    //Lazily creates encrypted preferences for securely storing behavioural profile data.
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

    //Saves the baseline profile values and metadata securely.
    fun saveProfile(profile: BaselineProfile) {
        prefs.edit()
            .putString(KEY_FEATURE_MEANS, mapToJson(profile.featureMeans))
            .putString(KEY_FEATURE_STDS, mapToJson(profile.featureStds))
            .putInt(KEY_SESSION_COUNT, profile.sessionCount)
            .putBoolean(KEY_IS_ACCESSIBILITY_PROFILE, profile.isAccessibilityProfile)
            .putLong(KEY_CREATED_TIMESTAMP_MS, profile.createdTimestampMs)
            .apply()
    }

    //Loads the stored baseline profile or returns null if no profile exists.
    fun loadProfile(): BaselineProfile? {
        val meansJson = prefs.getString(KEY_FEATURE_MEANS, null) ?: return null
        val stdsJson = prefs.getString(KEY_FEATURE_STDS, null) ?: return null

        return BaselineProfile(
            featureMeans = jsonToMap(meansJson),
            featureStds = jsonToMap(stdsJson),
            sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0),
            isAccessibilityProfile = prefs.getBoolean(KEY_IS_ACCESSIBILITY_PROFILE, false),
            createdTimestampMs = prefs.getLong(KEY_CREATED_TIMESTAMP_MS, 0L)
        )
    }

    //Checks whether a valid baseline profile has been saved.
    fun hasProfile(): Boolean {
        return prefs.contains(KEY_FEATURE_MEANS) &&
                prefs.contains(KEY_FEATURE_STDS) &&
                prefs.getInt(KEY_SESSION_COUNT, 0) > 0
    }

    //Deletes all saved baseline profile data.
    fun clearProfile() {
        prefs.edit().clear().apply()
    }

    //Converts a feature map into a JSON string for storage.
    private fun mapToJson(values: Map<String, Double>): String {
        val json = JSONObject()
        values.toSortedMap().forEach { (key, value) ->
            json.put(key, value)
        }
        return json.toString()
    }

    //Converts stored JSON back into a feature map.
    private fun jsonToMap(rawJson: String): Map<String, Double> {
        val json = JSONObject(rawJson)
        return buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, json.optDouble(key, 0.0))
            }
        }
    }

    //Defines preference file and key names used for profile storage.
    private companion object {
        const val PREFERENCES_FILE = "baseline_profile_prefs"
        const val KEY_FEATURE_MEANS = "feature_means"
        const val KEY_FEATURE_STDS = "feature_stds"
        const val KEY_SESSION_COUNT = "session_count"
        const val KEY_IS_ACCESSIBILITY_PROFILE = "is_accessibility_profile"
        const val KEY_CREATED_TIMESTAMP_MS = "created_timestamp_ms"
    }
}