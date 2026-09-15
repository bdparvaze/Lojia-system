package com.lojia.pos.data

import android.content.Context
import android.content.SharedPreferences
import com.lojia.pos.BuildConfig
import com.lojia.pos.auth.DevCredentials
import com.lojia.pos.util.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PreferencesRepository manages persistent user session state and Quick Login credentials
 * (PIN hash, Biometric status, session tokens, and security flags).
 *
 * It provides logic to determine if a valid security state exists to default
 * the app to the Quick Login screen upon startup or post-logout.
 */
class PreferencesRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isQuickLoginActive = MutableStateFlow(shouldDefaultToQuickLogin())
    val isQuickLoginActive: StateFlow<Boolean> = _isQuickLoginActive.asStateFlow()

    companion object {
        private const val PREFS_NAME = "lojia_secure_prefs"

        // Session keys
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_HAS_ACTIVE_SESSION = "key_has_active_session"
        private const val KEY_SAVED_USERNAME = "key_saved_username"
        private const val KEY_SAVED_FULL_NAME = "key_saved_full_name"
        private const val KEY_SAVED_EMAIL = "key_saved_email"
        private const val KEY_REMEMBER_ME = "key_remember_me"
        private const val KEY_LAST_LOGIN_TIME = "key_last_login_time"

        // Security / Quick Login keys
        private const val KEY_QUICK_LOGIN_ENABLED = "key_quick_login_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_STORED_PIN_HASH = "key_stored_pin_hash"
        private const val KEY_HAS_PIN_CONFIGURED = "key_has_pin_configured"

        @Volatile
        private var instance: PreferencesRepository? = null

        fun getInstance(context: Context): PreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: PreferencesRepository(context).also { instance = it }
            }
        }
    }

    init {
        // Debug seeding: in debug builds, if uninitialized, set default seed so user has a valid security state
        if (BuildConfig.DEBUG && !prefs.contains(KEY_QUICK_LOGIN_ENABLED)) {
            prefs.edit().apply {
                putBoolean(KEY_QUICK_LOGIN_ENABLED, true)
                putBoolean(KEY_BIOMETRIC_ENABLED, true)
                putString(KEY_STORED_PIN_HASH, SecurityUtils.hashSecret(DevCredentials.DEFAULT_PIN))
                putBoolean(KEY_HAS_PIN_CONFIGURED, true)
                putString(KEY_SAVED_USERNAME, DevCredentials.DEFAULT_USERNAME)
                putString(KEY_SAVED_FULL_NAME, "Demo Owner")
                putBoolean(KEY_HAS_ACTIVE_SESSION, true)
                putBoolean(KEY_REMEMBER_ME, true)
                apply()
            }
        }
    }

    /**
     * Checks if there is a valid security state configured:
     * 1. A stored user session or saved identity exists AND
     * 2. Quick Login is enabled with a configured PIN or Biometric verification.
     */
    fun hasValidSecurityState(): Boolean {
        val hasSession = hasActiveSession() || getSavedUsername().isNotBlank()
        val hasPin = hasPinConfigured() || !getStoredPinHash().isNullOrBlank()
        val isBio = isBiometricEnabled()
        val isQuickEnabled = isQuickLoginEnabled()

        return (hasPin || isBio || isQuickEnabled) && hasSession
    }

    /**
     * Determines whether the app should default directly to the Quick Login screen.
     */
    fun shouldDefaultToQuickLogin(): Boolean {
        return hasValidSecurityState()
    }

    /**
     * Saves user session data upon successful login or registration.
     */
    fun saveUserSession(
        username: String,
        fullName: String? = null,
        email: String? = null,
        rememberMe: Boolean = true
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_HAS_ACTIVE_SESSION, true)
            putString(KEY_SAVED_USERNAME, username)
            fullName?.let { putString(KEY_SAVED_FULL_NAME, it) }
            email?.let { putString(KEY_SAVED_EMAIL, it) }
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
        _isQuickLoginActive.value = shouldDefaultToQuickLogin()
    }

    /**
     * Persists security credentials (PIN hash and biometric toggle).
     */
    fun saveSecurityState(
        pinHash: String?,
        isBiometricEnabled: Boolean,
        isQuickLoginEnabled: Boolean = true
    ) {
        prefs.edit().apply {
            putBoolean(KEY_QUICK_LOGIN_ENABLED, isQuickLoginEnabled)
            putBoolean(KEY_BIOMETRIC_ENABLED, isBiometricEnabled)
            if (!pinHash.isNullOrBlank()) {
                val hash = SecurityUtils.hashSecret(pinHash)
                putString(KEY_STORED_PIN_HASH, hash)
                putBoolean(KEY_HAS_PIN_CONFIGURED, true)
            }
            apply()
        }
        _isQuickLoginActive.value = shouldDefaultToQuickLogin()
    }

    /**
     * Updates or sets the 6-digit Quick PIN.
     */
    fun setQuickPin(pin: String) {
        val hashed = SecurityUtils.hashSecret(pin)
        prefs.edit().apply {
            putString(KEY_STORED_PIN_HASH, hashed)
            putBoolean(KEY_HAS_PIN_CONFIGURED, true)
            putBoolean(KEY_QUICK_LOGIN_ENABLED, true)
            apply()
        }
        _isQuickLoginActive.value = shouldDefaultToQuickLogin()
    }

    /**
     * Toggles the biometric authentication status.
     */
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            apply()
        }
        _isQuickLoginActive.value = shouldDefaultToQuickLogin()
    }

    /**
     * Verifies an entered PIN against the stored hash in PreferencesRepository,
     * or a fallback profile PIN hash.
     */
    fun verifyPin(enteredPin: String, fallbackPinHash: String? = null): Boolean {
        val storedHash = getStoredPinHash()
        if (!storedHash.isNullOrBlank() && SecurityUtils.verifySecret(enteredPin, storedHash)) {
            return true
        }
        if (!fallbackPinHash.isNullOrBlank() && SecurityUtils.verifySecret(enteredPin, fallbackPinHash)) {
            return true
        }
        if (BuildConfig.DEBUG && enteredPin == DevCredentials.DEFAULT_PIN) {
            return true
        }
        return false
    }

    /**
     * Synchronizes PreferencesRepository with Room's UserProfile.
     */
    fun syncWithUserProfile(profile: UserProfile) {
        prefs.edit().apply {
            if (profile.username.isNotBlank()) {
                putString(KEY_SAVED_USERNAME, profile.username)
                putBoolean(KEY_HAS_ACTIVE_SESSION, true)
            }
            if (profile.fullName.isNotBlank()) {
                putString(KEY_SAVED_FULL_NAME, profile.fullName)
            }
            if (profile.email.isNotBlank()) {
                putString(KEY_SAVED_EMAIL, profile.email)
            }
            putBoolean(KEY_BIOMETRIC_ENABLED, profile.isBiometricEnabled)
            if (profile.pin.isNotBlank()) {
                putString(KEY_STORED_PIN_HASH, SecurityUtils.hashSecret(profile.pin))
                putBoolean(KEY_HAS_PIN_CONFIGURED, true)
            }
            putBoolean(KEY_QUICK_LOGIN_ENABLED, true)
            apply()
        }
        _isQuickLoginActive.value = shouldDefaultToQuickLogin()
    }

    /**
     * Records logout while preserving security credentials for subsequent Quick Logins.
     */
    fun recordLogout(keepQuickLoginState: Boolean = true) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            if (!keepQuickLoginState) {
                putBoolean(KEY_HAS_ACTIVE_SESSION, false)
            }
            apply()
        }
        _isQuickLoginActive.value = shouldDefaultToQuickLogin()
    }

    fun hasActiveSession(): Boolean = prefs.getBoolean(KEY_HAS_ACTIVE_SESSION, false)
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getSavedUsername(): String = prefs.getString(KEY_SAVED_USERNAME, "") ?: ""
    fun getSavedFullName(): String = prefs.getString(KEY_SAVED_FULL_NAME, "") ?: ""
    fun getSavedEmail(): String = prefs.getString(KEY_SAVED_EMAIL, "") ?: ""
    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, true)
    fun isQuickLoginEnabled(): Boolean = prefs.getBoolean(KEY_QUICK_LOGIN_ENABLED, true)
    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    fun getStoredPinHash(): String? = prefs.getString(KEY_STORED_PIN_HASH, null)
    fun hasPinConfigured(): Boolean = prefs.getBoolean(KEY_HAS_PIN_CONFIGURED, false)

    /**
     * Clears all preferences (useful for complete data wipe).
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        _isQuickLoginActive.value = false
    }
}
