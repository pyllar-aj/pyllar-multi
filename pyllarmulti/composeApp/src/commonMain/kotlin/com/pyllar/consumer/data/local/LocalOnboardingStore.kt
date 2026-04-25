package com.pyllar.consumer.data.local

/**
 * Onboarding step states — shared enum used by both the
 * `commonMain` interface and the Room-backed `androidMain` implementation.
 */
enum class OnboardingStep {
    PHONE_OTP,
    PERMISSION,
    PRE_VERIFICATION,
    PAN_KYC,
    CHECK_PAN_POPULATED_DETAILS,
    NAME_DOB,
    KYC_INFORMATION,
    MIN_DETAILS,
    ADDITIONAL_KYC,
    BANK_DETAILS,
    NOMINEE_DETAILS,
    SIGNATURE,
    ESIGN_INFORMATION,
    SIGNATURE_ESIGN,
    SIP_AMOUNT,
    INITIAL_DASHBOARD,
    HOME_COMPLETED,
    PORTFOLIO_COMPLETED,
    INVESTMENT_DASHBOARD,
    COMPLETED;

    companion object {
        fun fromString(value: String): OnboardingStep =
            values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PHONE_OTP
    }
}

/**
 * Lightweight snapshot of onboarding state — usable in both platforms.
 */
data class OnboardingStateSnapshot(
    val userId: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val pan: String? = null,
    val name: String? = null,
    val dob: String? = null,
    val kycAttemptId: String? = null,
    val reUrl: String? = null,
    val maritalStatus: String? = null,
    val occupationType: String? = null,
    val fatherName: String? = null,
    val annualIncome: String? = null,
    val isPoliticallyExposed: Boolean? = null,
    val nationalityCountry: String? = null,
    val placeOfBirth: String? = null,
    val gender: String? = null,
    val accountNumber: String? = null,
    val ifscCode: String? = null,
    val bankName: String? = null,
    val accountHolderName: String? = null,
    val accountType: String? = null,
    val investorId: String? = null,
    val userPurposeId: String? = null,
    val sipAmount: Double? = null,
    val onboardingStep: OnboardingStep = OnboardingStep.PHONE_OTP,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val permanentAddress: String? = null,
    val correspondenceAddress: String? = null
) {
    /**
     * Determines the correct step to resume from when restoring local state.
     */
    fun resumeStep(): OnboardingStep = when {
        onboardingStep == OnboardingStep.CHECK_PAN_POPULATED_DETAILS ->
            OnboardingStep.CHECK_PAN_POPULATED_DETAILS
        kycAttemptId != null &&
            !kycAttemptId.lowercase().startsWith("pre_verified_") &&
            onboardingStep == OnboardingStep.ADDITIONAL_KYC ->
            OnboardingStep.NAME_DOB
        else -> onboardingStep
    }
}

/**
 * Shared interface for local onboarding persistence.
 *
 * `androidMain` implements this via Room + DAOs.
 * `iosMain` provides a lightweight in-memory / file-based placeholder.
 */
interface LocalOnboardingStore {
    // ─── Session ───
    suspend fun saveUserSession(
        userId: String,
        email: String = "",
        phone: String = "",
        authToken: String = "",
        fullName: String = ""
    )
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun getCurrentUserId(): String
    suspend fun getCurrentEmail(): String
    suspend fun getCurrentPhone(): String
    suspend fun getCurrentToken(): String
    suspend fun getCurrentFullName(): String

    // ─── Key-Value helpers ───
    suspend fun saveToken(token: String)
    suspend fun savePhone(phone: String)
    suspend fun saveUserId(userId: String)
    suspend fun getUserId(): String
    suspend fun saveKycAttemptId(kycAttemptId: String)
    suspend fun getKycAttemptId(): String
    suspend fun saveInvestorId(investorId: String)
    suspend fun getInvestorId(): String
    suspend fun saveReUrl(reUrl: String)
    suspend fun getReUrl(): String
    suspend fun deleteReUrl()
    suspend fun saveEsignUrl(esignUrl: String)
    suspend fun getEsignUrl(): String
    suspend fun deleteEsignUrl()
    suspend fun savePanHolderName(panHolderName: String)
    suspend fun getPanHolderName(): String
    suspend fun clearToken()
    suspend fun saveValue(key: String, value: String)
    suspend fun getValue(key: String): String?
    suspend fun getValueFromStore(key: String, currentValue: String): String

    // ─── Language ───
    suspend fun getLanguagePreference(): String?
    suspend fun setLanguagePreference(languageTag: String)

    // ─── Onboarding state ───
    suspend fun getOnboardingState(): OnboardingStateSnapshot?
}
