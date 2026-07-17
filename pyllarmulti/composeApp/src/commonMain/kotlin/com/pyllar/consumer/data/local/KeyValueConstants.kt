package com.pyllar.consumer.data.local

/**
 * Constants for key-value store keys.
 * These keys are used across the application to ensure consistent data access.
 * Migrated from Android-only to commonMain — no platform dependencies.
 */
object KeyValueConstants {
    // User Session
    const val USER_ID = "user_id"
    const val AUTH_TOKEN = "auth_token"
    const val EMAIL = "email"
    const val PHONE = "phone"
    const val FULL_NAME = "full_name"

    // KYC
    const val KYC_ATTEMPT_ID = "kyc_attempt_id"
    const val INVESTOR_ID = "investor_id"
    const val RE_URL = "re_url"
    const val ESIGN_URL = "esign_url"
    const val USER_PURPOSE_ID = "user_purpose_id"

    // Onboarding
    const val ONBOARDING_STEP = "onboarding_step"
    const val ONBOARDING_COMPLETED = "onboarding_completed"

    // Bank Details
    const val ACCOUNT_NUMBER = "account_number"
    const val IFSC_CODE = "ifsc_code"
    const val BANK_NAME = "bank_name"
    const val ACCOUNT_HOLDER_NAME = "account_holder_name"
    const val ACCOUNT_TYPE = "account_type"

    // Mutual Fund Redemption
    const val REDEMPTION_TOKEN_TRACKER_ID = "redemption_token_tracker_id"
    const val CONSENT_TOKEN_TRACKER_ID = "consent_token_tracker_id"

    // Personal Details
    const val PAN = "pan"
    const val PAN_HOLDER_NAME = "pan_holder_name"
    const val DOB = "dob"
    const val MARITAL_STATUS = "marital_status"
    const val OCCUPATION_TYPE = "occupation_type"
    const val FATHER_NAME = "father_name"
    const val ANNUAL_INCOME = "annual_income"
    const val IS_POLITICALLY_EXPOSED = "is_politically_exposed"
    const val NATIONALITY_COUNTRY = "nationality_country"
    const val PLACE_OF_BIRTH = "place_of_birth"
    const val GENDER = "gender"

    // Investment
    const val SIP_AMOUNT = "sip_amount"
    const val SELECTED_GOAL_ID = "selected_goal_id"

    // Helper Code
    const val HELPER_CODE = "helper_code"
    const val HELPER_CODE_SUBMITTED = "helper_code_submitted"

    // Location
    const val LONGITUDE = "longitude"
    const val LATITUDE = "latitude"

    // User preferences (metadata)
    const val LANGUAGE_PREFERENCE = "language_preference"
    // Persistence
    const val LAST_SCREEN = "last_screen"

    // One-time migration flags
    const val FORCE_LOGOUT_SESSION_FIX_DONE = "force_logout_session_fix_done"
}
