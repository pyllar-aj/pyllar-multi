package com.pyllar.consumer.data.local

import android.content.Context
import android.content.SharedPreferences
import com.pyllar.consumer.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [LocalOnboardingStore] backed by SharedPreferences.
 *
 * Note: Room is not yet a dependency of the KMP module. If Room is added later
 * (with KSP annotation processor), swap this class for a Room-backed implementation.
 * The Room entities, DAOs, and the full OnboardingRepository source are available at:
 *   Pyllar/android/app/src/main/…/data/local/OnboardingRepository.kt
 *
 * TODO: Add Room when ready:
 *   implementation("androidx.room:room-runtime:2.x.x")
 *   implementation("androidx.room:room-ktx:2.x.x")
 *   ksp("androidx.room:room-compiler:2.x.x")
 */
class AndroidLocalOnboardingStore private constructor(context: Context) : LocalOnboardingStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("pyllar_onboarding", Context.MODE_PRIVATE)

    // ─── Session ──────────────────────────────────────────────────────────────

    override suspend fun saveUserSession(
        userId: String,
        email: String,
        phone: String,
        authToken: String,
        fullName: String
    ) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            if (userId.isNotBlank()) putString(KeyValueConstants.USER_ID, userId)
            if (email.isNotBlank()) putString(KeyValueConstants.EMAIL, email)
            if (phone.isNotBlank()) putString(KeyValueConstants.PHONE, phone)
            if (authToken.isNotBlank()) putString(KeyValueConstants.AUTH_TOKEN, authToken)
            if (fullName.isNotBlank()) putString(KeyValueConstants.FULL_NAME, fullName)
            putBoolean("is_logged_in", true)
            putLong("last_login_time", System.currentTimeMillis())
        }.apply()
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    override suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean("is_logged_in", false)
    }

    override suspend fun getCurrentUserId(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.USER_ID, "") ?: ""
    }

    override suspend fun getCurrentEmail(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.EMAIL, "") ?: ""
    }

    override suspend fun getCurrentPhone(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.PHONE, "") ?: ""
    }

    override suspend fun getCurrentToken(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.AUTH_TOKEN, "") ?: ""
    }

    override suspend fun getCurrentFullName(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.FULL_NAME, "") ?: ""
    }

    // ─── KV helpers ──────────────────────────────────────────────────────────

    override suspend fun saveToken(token: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KeyValueConstants.AUTH_TOKEN, token).apply()
    }

    override suspend fun savePhone(phone: String) = withContext(Dispatchers.IO) {
        if (phone.isNotBlank()) prefs.edit().putString(KeyValueConstants.PHONE, phone).apply()
    }

    override suspend fun saveUserId(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isNotBlank()) prefs.edit().putString(KeyValueConstants.USER_ID, userId).apply()
    }

    override suspend fun getUserId(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.USER_ID, "") ?: ""
    }

    override suspend fun saveKycAttemptId(kycAttemptId: String) = withContext(Dispatchers.IO) {
        if (kycAttemptId.isNotBlank()) prefs.edit().putString(KeyValueConstants.KYC_ATTEMPT_ID, kycAttemptId).apply()
    }

    override suspend fun getKycAttemptId(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.KYC_ATTEMPT_ID, "") ?: ""
    }

    override suspend fun saveInvestorId(investorId: String) = withContext(Dispatchers.IO) {
        if (investorId.isNotBlank()) prefs.edit().putString(KeyValueConstants.INVESTOR_ID, investorId).apply()
    }

    override suspend fun getInvestorId(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.INVESTOR_ID, "") ?: ""
    }

    override suspend fun saveReUrl(reUrl: String) = withContext(Dispatchers.IO) {
        if (reUrl.isNotBlank()) prefs.edit().putString(KeyValueConstants.RE_URL, reUrl).apply()
    }

    override suspend fun getReUrl(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.RE_URL, "") ?: ""
    }

    override suspend fun deleteReUrl() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KeyValueConstants.RE_URL).apply()
    }

    override suspend fun saveEsignUrl(esignUrl: String) = withContext(Dispatchers.IO) {
        if (esignUrl.isNotBlank()) prefs.edit().putString(KeyValueConstants.ESIGN_URL, esignUrl).apply()
    }

    override suspend fun getEsignUrl(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.ESIGN_URL, "") ?: ""
    }

    override suspend fun deleteEsignUrl() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KeyValueConstants.ESIGN_URL).apply()
    }

    override suspend fun savePanHolderName(panHolderName: String) = withContext(Dispatchers.IO) {
        if (panHolderName.isNotBlank()) prefs.edit().putString(KeyValueConstants.PAN_HOLDER_NAME, panHolderName).apply()
    }

    override suspend fun getPanHolderName(): String = withContext(Dispatchers.IO) {
        prefs.getString(KeyValueConstants.PAN_HOLDER_NAME, "") ?: ""
    }

    override suspend fun clearToken() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KeyValueConstants.AUTH_TOKEN).apply()
    }

    override suspend fun saveValue(key: String, value: String) = withContext(Dispatchers.IO) {
        if (value.isNotBlank()) prefs.edit().putString(key, value).apply()
    }

    override suspend fun getValue(key: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(key, null)
    }

    override suspend fun getValueFromStore(key: String, currentValue: String): String = withContext(Dispatchers.IO) {
        val isPlaceholder = currentValue.isBlank() ||
            currentValue.startsWith("no_") ||
            currentValue == "no_user_id" ||
            currentValue == "no_kyc_id" ||
            currentValue == "no_investor_id"
        if (!isPlaceholder) return@withContext currentValue
        prefs.getString(key, null)?.takeIf { it.isNotBlank() } ?: currentValue
    }

    // ─── Language ─────────────────────────────────────────────────────────────

    override suspend fun getLanguagePreference(): String? = getValue(KeyValueConstants.LANGUAGE_PREFERENCE)

    override suspend fun setLanguagePreference(languageTag: String) = saveValue(KeyValueConstants.LANGUAGE_PREFERENCE, languageTag)

    // ─── Onboarding state ────────────────────────────────────────────────────

    override suspend fun getOnboardingState(): OnboardingStateSnapshot? = withContext(Dispatchers.IO) {
        val step = prefs.getString(KeyValueConstants.ONBOARDING_STEP, null) ?: return@withContext null
        OnboardingStateSnapshot(
            userId = prefs.getString(KeyValueConstants.USER_ID, null),
            phone = prefs.getString(KeyValueConstants.PHONE, null),
            email = prefs.getString(KeyValueConstants.EMAIL, null),
            pan = prefs.getString(KeyValueConstants.PAN, null),
            kycAttemptId = prefs.getString(KeyValueConstants.KYC_ATTEMPT_ID, null),
            reUrl = prefs.getString(KeyValueConstants.RE_URL, null),
            investorId = prefs.getString(KeyValueConstants.INVESTOR_ID, null),
            onboardingStep = OnboardingStep.fromString(step)
        )
    }

    companion object {
        @Volatile
        private var instance: AndroidLocalOnboardingStore? = null

        fun getInstance(context: Context): AndroidLocalOnboardingStore =
            instance ?: synchronized(this) {
                instance ?: AndroidLocalOnboardingStore(context).also { instance = it }
            }
    }
}
