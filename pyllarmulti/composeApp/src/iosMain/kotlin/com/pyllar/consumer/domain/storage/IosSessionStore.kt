package com.pyllar.consumer.domain.storage

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of SessionStore using NSUserDefaults.
 */
class IosSessionStore : SessionStore {

    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    override suspend fun saveUserSession(
        userId: String,
        email: String,
        phone: String,
        authToken: String,
        fullName: String
    ) {
        if (userId.isNotBlank()) defaults.setObject(userId, forKey = KEY_USER_ID)
        if (email.isNotBlank()) defaults.setObject(email, forKey = KEY_EMAIL)
        
        // Critically: don't overwrite a good phone with a blank one
        if (phone.isNotBlank()) {
            defaults.setObject(phone, forKey = KEY_PHONE)
        } else {
            val current = defaults.stringForKey(KEY_PHONE)
            if (current.isNullOrBlank()) {
                // Only if it's currently empty, do we allow setting it to blank (though it already is)
            }
        }
        
        if (authToken.isNotBlank()) defaults.setObject(authToken, forKey = KEY_AUTH_TOKEN)
        if (fullName.isNotBlank()) defaults.setObject(fullName, forKey = KEY_FULL_NAME)
        
        defaults.setBool(true, forKey = KEY_LOGGED_IN)
        defaults.synchronize()
    }

    override suspend fun getCurrentToken(): String =
        defaults.stringForKey(KEY_AUTH_TOKEN) ?: ""

    override suspend fun getCurrentUserId(): String =
        defaults.stringForKey(KEY_USER_ID) ?: ""

    override suspend fun getCurrentEmail(): String =
        defaults.stringForKey(KEY_EMAIL) ?: ""

    override suspend fun getCurrentPhone(): String =
        defaults.stringForKey(KEY_PHONE) ?: ""

    override suspend fun getCurrentFullName(): String =
        defaults.stringForKey(KEY_FULL_NAME) ?: ""

    override suspend fun isLoggedIn(): Boolean =
        defaults.boolForKey(KEY_LOGGED_IN)

    override suspend fun logout() {
        defaults.removeObjectForKey(KEY_USER_ID)
        defaults.removeObjectForKey(KEY_EMAIL)
        defaults.removeObjectForKey(KEY_PHONE)
        defaults.removeObjectForKey(KEY_AUTH_TOKEN)
        defaults.removeObjectForKey(KEY_FULL_NAME)
        defaults.setBool(false, forKey = KEY_LOGGED_IN)
        defaults.synchronize()
    }

    override suspend fun saveToken(token: String) {
        defaults.setObject(token, forKey = KEY_AUTH_TOKEN)
        defaults.synchronize()
    }

    override suspend fun saveUserId(userId: String) {
        defaults.setObject(userId, forKey = KEY_USER_ID)
        defaults.synchronize()
    }

    override suspend fun savePhone(phone: String) {
        defaults.setObject(phone, forKey = KEY_PHONE)
        defaults.synchronize()
    }

    override suspend fun saveValue(key: String, value: String) {
        defaults.setObject(value, forKey = key)
        defaults.synchronize()
    }

    override suspend fun getValue(key: String): String? =
        defaults.stringForKey(key)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_LOGGED_IN = "logged_in"
    }
}

