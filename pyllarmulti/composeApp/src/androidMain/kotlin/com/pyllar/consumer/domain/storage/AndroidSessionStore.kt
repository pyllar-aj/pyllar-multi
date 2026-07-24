package com.pyllar.consumer.domain.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Android implementation of SessionStore.
 *
 * This implementation uses SharedPreferences. It is intentionally
 * free of Room so it can live entirely within the KMP module.
 */
class AndroidSessionStore(
    context: Context
) : SessionStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pyllar_session", Context.MODE_PRIVATE)

    override suspend fun saveUserSession(
        userId: String,
        email: String,
        phone: String,
        authToken: String,
        fullName: String
    ) {
        // Blank args are skipped rather than overwriting an existing value, matching
        // IosSessionStore.saveUserSession — callers (e.g. verifyOtp) may not have every
        // field yet and shouldn't wipe out ones saved by an earlier step.
        prefs.edit().apply {
            if (userId.isNotBlank()) putString(KEY_USER_ID, userId)
            if (email.isNotBlank()) putString(KEY_EMAIL, email)
            if (phone.isNotBlank()) putString(KEY_PHONE, phone)
            if (authToken.isNotBlank()) putString(KEY_AUTH_TOKEN, authToken)
            if (fullName.isNotBlank()) putString(KEY_FULL_NAME, fullName)
            putBoolean(KEY_LOGGED_IN, true)
        }.apply()
    }

    override suspend fun getCurrentToken(): String =
        prefs.getString(KEY_AUTH_TOKEN, "") ?: ""

    override suspend fun getCurrentUserId(): String =
        prefs.getString(KEY_USER_ID, "") ?: ""

    override suspend fun getCurrentEmail(): String =
        prefs.getString(KEY_EMAIL, "") ?: ""

    override suspend fun getCurrentPhone(): String =
        prefs.getString(KEY_PHONE, "") ?: ""

    override suspend fun getCurrentFullName(): String =
        prefs.getString(KEY_FULL_NAME, "") ?: ""

    override suspend fun isLoggedIn(): Boolean =
        prefs.getBoolean(KEY_LOGGED_IN, false)

    override suspend fun logout() {
        prefs.edit().clear().apply()
        com.pyllar.consumer.data.remote.crypto.createSecureSessionStore().clear()
    }

    override suspend fun saveToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    override suspend fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    override suspend fun savePhone(phone: String) {
        prefs.edit().putString(KEY_PHONE, phone).apply()
    }

    override suspend fun saveValue(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override suspend fun getValue(key: String): String? =
        prefs.getString(key, null)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_LOGGED_IN = "logged_in"
    }
}

