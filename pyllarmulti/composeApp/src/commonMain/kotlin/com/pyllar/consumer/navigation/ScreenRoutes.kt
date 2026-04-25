package com.pyllar.consumer.navigation

/**
 * Centralized screen route constants for navigation.
 * Maps server screen identifiers to actual navigation routes used in the app.
 */
object ScreenRoutes {

    // Authentication
    const val LOGIN = "login"
    const val PHONE_VERIFICATION = "phone_verification"
    const val OTP_VERIFICATION = "otp_verification"
    const val SIGNUP = "signup"

    // Onboarding
    const val PERMISSION = "permission"
    const val KYC_PAN = "pan_kyc"
    const val NAME_DOB = "name_dob"
    const val ADDITIONAL_KYC = "additional_kyc"
    const val BANK_DETAILS = "bank_details"
    const val WEBVIEW = "webview"
    const val WEBVIEW_ESIGN = "webview_esign"

    // Investment
    const val SIP_AMOUNT = "sip_amount"
    const val DASHBOARD = "dashboard"
    const val PORTFOLIO = "portfolio"
    const val MUTUAL_FUND_MAIN = "mutual_fund_main"
    const val SIP_CREATION = "sip_creation"
    const val UPI_LINKING = "upi_linking"
    const val UPI_MANDATE = "upi_mandate"
    const val FUND_DETAILS = "fund_details"

    // Main app
    const val HOME = "home"

    // Utility
    const val SUCCESS = "success"
    const val ERROR = "error"
    const val RETRY = "retry"

    fun otpVerification(phoneNumber: String): String =
        "$OTP_VERIFICATION/$phoneNumber"

    fun nameDob(email: String): String =
        "$NAME_DOB/$email"
}

