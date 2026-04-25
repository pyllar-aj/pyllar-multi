package com.pyllar.consumer.data.local

/**
 * iOS placeholder implementation of [LocalOnboardingStore].
 *
 * All state is held in-memory. Persist to NSUserDefaults or a SQLite
 * layer when the iOS host app requires durable onboarding state.
 */
class IosLocalOnboardingStore : LocalOnboardingStore {

    private val store = mutableMapOf<String, String>()
    private var loggedIn = false

    override suspend fun saveUserSession(userId: String, email: String, phone: String, authToken: String, fullName: String) {
        store[KeyValueConstants.USER_ID] = userId
        if (email.isNotBlank()) store[KeyValueConstants.EMAIL] = email
        if (phone.isNotBlank()) store[KeyValueConstants.PHONE] = phone
        if (authToken.isNotBlank()) store[KeyValueConstants.AUTH_TOKEN] = authToken
        if (fullName.isNotBlank()) store[KeyValueConstants.FULL_NAME] = fullName
        loggedIn = true
    }

    override suspend fun logout() {
        store.clear()
        loggedIn = false
    }

    override suspend fun isLoggedIn(): Boolean = loggedIn

    override suspend fun getCurrentUserId(): String = store[KeyValueConstants.USER_ID] ?: ""
    override suspend fun getCurrentEmail(): String = store[KeyValueConstants.EMAIL] ?: ""
    override suspend fun getCurrentPhone(): String = store[KeyValueConstants.PHONE] ?: ""
    override suspend fun getCurrentToken(): String = store[KeyValueConstants.AUTH_TOKEN] ?: ""
    override suspend fun getCurrentFullName(): String = store[KeyValueConstants.FULL_NAME] ?: ""

    override suspend fun saveToken(token: String) { store[KeyValueConstants.AUTH_TOKEN] = token }
    override suspend fun savePhone(phone: String) { if (phone.isNotBlank()) store[KeyValueConstants.PHONE] = phone }
    override suspend fun saveUserId(userId: String) { if (userId.isNotBlank()) store[KeyValueConstants.USER_ID] = userId }
    override suspend fun getUserId(): String = store[KeyValueConstants.USER_ID] ?: ""

    override suspend fun saveKycAttemptId(kycAttemptId: String) { if (kycAttemptId.isNotBlank()) store[KeyValueConstants.KYC_ATTEMPT_ID] = kycAttemptId }
    override suspend fun getKycAttemptId(): String = store[KeyValueConstants.KYC_ATTEMPT_ID] ?: ""

    override suspend fun saveInvestorId(investorId: String) { if (investorId.isNotBlank()) store[KeyValueConstants.INVESTOR_ID] = investorId }
    override suspend fun getInvestorId(): String = store[KeyValueConstants.INVESTOR_ID] ?: ""

    override suspend fun saveReUrl(reUrl: String) { if (reUrl.isNotBlank()) store[KeyValueConstants.RE_URL] = reUrl }
    override suspend fun getReUrl(): String = store[KeyValueConstants.RE_URL] ?: ""
    override suspend fun deleteReUrl() { store.remove(KeyValueConstants.RE_URL) }

    override suspend fun saveEsignUrl(esignUrl: String) { if (esignUrl.isNotBlank()) store[KeyValueConstants.ESIGN_URL] = esignUrl }
    override suspend fun getEsignUrl(): String = store[KeyValueConstants.ESIGN_URL] ?: ""
    override suspend fun deleteEsignUrl() { store.remove(KeyValueConstants.ESIGN_URL) }

    override suspend fun savePanHolderName(panHolderName: String) { if (panHolderName.isNotBlank()) store[KeyValueConstants.PAN_HOLDER_NAME] = panHolderName }
    override suspend fun getPanHolderName(): String = store[KeyValueConstants.PAN_HOLDER_NAME] ?: ""

    override suspend fun clearToken() { store.remove(KeyValueConstants.AUTH_TOKEN) }
    override suspend fun saveValue(key: String, value: String) { if (value.isNotBlank()) store[key] = value }
    override suspend fun getValue(key: String): String? = store[key]

    override suspend fun getValueFromStore(key: String, currentValue: String): String {
        val isPlaceholder = currentValue.isBlank() || currentValue.startsWith("no_")
        return if (!isPlaceholder) currentValue else store[key]?.takeIf { it.isNotBlank() } ?: currentValue
    }

    override suspend fun getLanguagePreference(): String? = store[KeyValueConstants.LANGUAGE_PREFERENCE]
    override suspend fun setLanguagePreference(languageTag: String) { store[KeyValueConstants.LANGUAGE_PREFERENCE] = languageTag }

    override suspend fun getOnboardingState(): OnboardingStateSnapshot? = null // in-memory only; no persistence yet
}
