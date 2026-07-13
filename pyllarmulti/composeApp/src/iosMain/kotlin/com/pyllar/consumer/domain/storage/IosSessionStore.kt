package com.pyllar.consumer.domain.storage

import platform.Foundation.NSUserDefaults
import com.pyllar.consumer.data.remote.crypto.SwiftCryptoScope
import com.pyllar.consumer.data.local.KeyValueConstants

/**
 * iOS implementation of SessionStore using Keychain for sensitive data
 * and NSUserDefaults for non-sensitive flags.
 */
class IosSessionStore : SessionStore {

    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
    private val bridge get() = SwiftCryptoScope.bridge

    override suspend fun saveUserSession(
        userId: String,
        email: String,
        phone: String,
        authToken: String,
        fullName: String
    ) {
        if (userId.isNotBlank()) bridge?.saveToKeychain(KEY_USER_ID, userId)
        if (email.isNotBlank()) bridge?.saveToKeychain(KEY_EMAIL, email)
        
        if (phone.isNotBlank()) {
            bridge?.saveToKeychain(KEY_PHONE, phone)
        }
        
        if (authToken.isNotBlank()) bridge?.saveToKeychain(KEY_AUTH_TOKEN, authToken)
        if (fullName.isNotBlank()) bridge?.saveToKeychain(KEY_FULL_NAME, fullName)
        
        defaults.setBool(true, forKey = KEY_LOGGED_IN)
        defaults.synchronize()
    }

    override suspend fun getCurrentToken(): String =
        bridge?.loadFromKeychain(KEY_AUTH_TOKEN) ?: ""

    override suspend fun getCurrentUserId(): String =
        bridge?.loadFromKeychain(KEY_USER_ID) ?: ""

    override suspend fun getCurrentEmail(): String =
        bridge?.loadFromKeychain(KEY_EMAIL) ?: ""

    override suspend fun getCurrentPhone(): String =
        bridge?.loadFromKeychain(KEY_PHONE) ?: ""

    override suspend fun getCurrentFullName(): String =
        bridge?.loadFromKeychain(KEY_FULL_NAME) ?: ""

    override suspend fun isLoggedIn(): Boolean =
        defaults.boolForKey(KEY_LOGGED_IN)

    override suspend fun logout() {
        bridge?.deleteFromKeychain(KEY_USER_ID)
        bridge?.deleteFromKeychain(KEY_EMAIL)
        bridge?.deleteFromKeychain(KEY_PHONE)
        bridge?.deleteFromKeychain(KEY_AUTH_TOKEN)
        bridge?.deleteFromKeychain(KEY_FULL_NAME)
        
        // Clear onboarding and personal keys to prevent prefilling on new logins
        val keysToClear = listOf(
            KeyValueConstants.KYC_ATTEMPT_ID,
            KeyValueConstants.INVESTOR_ID,
            KeyValueConstants.RE_URL,
            KeyValueConstants.ESIGN_URL,
            KeyValueConstants.USER_PURPOSE_ID,
            KeyValueConstants.ONBOARDING_STEP,
            KeyValueConstants.ONBOARDING_COMPLETED,
            KeyValueConstants.ACCOUNT_NUMBER,
            KeyValueConstants.IFSC_CODE,
            KeyValueConstants.BANK_NAME,
            KeyValueConstants.ACCOUNT_HOLDER_NAME,
            KeyValueConstants.ACCOUNT_TYPE,
            KeyValueConstants.REDEMPTION_TOKEN_TRACKER_ID,
            KeyValueConstants.CONSENT_TOKEN_TRACKER_ID,
            KeyValueConstants.PAN,
            KeyValueConstants.PAN_HOLDER_NAME,
            KeyValueConstants.DOB,
            KeyValueConstants.MARITAL_STATUS,
            KeyValueConstants.OCCUPATION_TYPE,
            KeyValueConstants.FATHER_NAME,
            KeyValueConstants.ANNUAL_INCOME,
            KeyValueConstants.IS_POLITICALLY_EXPOSED,
            KeyValueConstants.NATIONALITY_COUNTRY,
            KeyValueConstants.PLACE_OF_BIRTH,
            KeyValueConstants.GENDER,
            KeyValueConstants.SIP_AMOUNT,
            KeyValueConstants.SELECTED_GOAL_ID,
            KeyValueConstants.HELPER_CODE,
            KeyValueConstants.HELPER_CODE_SUBMITTED,
            KeyValueConstants.LONGITUDE,
            KeyValueConstants.LATITUDE,
            KeyValueConstants.LAST_SCREEN
        )
        keysToClear.forEach { key ->
            bridge?.deleteFromKeychain(key)
        }
        
        defaults.setBool(false, forKey = KEY_LOGGED_IN)
        defaults.synchronize()
    }

    override suspend fun saveToken(token: String) {
        bridge?.saveToKeychain(KEY_AUTH_TOKEN, token)
    }

    override suspend fun saveUserId(userId: String) {
        bridge?.saveToKeychain(KEY_USER_ID, userId)
    }

    override suspend fun savePhone(phone: String) {
        bridge?.saveToKeychain(KEY_PHONE, phone)
    }

    override suspend fun saveValue(key: String, value: String) {
        // For generic values, we use Keychain for safety by default
        bridge?.saveToKeychain(key, value)
    }

    override suspend fun getValue(key: String): String? =
        bridge?.loadFromKeychain(key)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_LOGGED_IN = "logged_in"
    }
}

