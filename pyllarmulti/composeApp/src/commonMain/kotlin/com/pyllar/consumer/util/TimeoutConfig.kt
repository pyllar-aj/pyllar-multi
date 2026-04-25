package com.pyllar.consumer.util

/**
 * Shared configuration for timeout values across different screens.
 */
object TimeoutConfig {

    // Default timeout values in seconds
    const val DEFAULT_BUTTON_TIMEOUT = 2L
    const val DEFAULT_OTP_RESEND_TIMEOUT = 30L
    const val DEFAULT_API_TIMEOUT = 60L
    const val DEFAULT_LOADING_TIMEOUT = 30L

    object PhoneVerification {
        const val CONTINUE_BUTTON_TIMEOUT = 2L
        const val LOADING_TIMEOUT = 30L
    }

    object OtpVerification {
        const val RESEND_OTP_TIMEOUT = 30L
        const val VERIFY_BUTTON_TIMEOUT = 1L
        const val LOADING_TIMEOUT = 30L
    }

    object PermissionScreen {
        const val GRANT_PERMISSIONS_TIMEOUT = 2L
        const val PROCESSING_TIMEOUT = 2L
        const val LOADING_TIMEOUT = 30L
    }

    object PanKyc {
        const val VERIFY_BUTTON_TIMEOUT = 1L
        const val LOADING_TIMEOUT = 30L
    }

    object PreVerification {
        const val CHECK_READINESS_TIMEOUT = 1L
        const val LOADING_TIMEOUT = 30L
    }

    object NameDob {
        const val CONTINUE_BUTTON_TIMEOUT = 1L
        const val KYC_PROCESSING_TIMEOUT = 10L
        const val LOADING_TIMEOUT = 30L
    }

    object AdditionalKyc {
        const val SUBMIT_BUTTON_TIMEOUT = 2L
        const val LOCATION_TIMEOUT = 10L
        const val LOADING_TIMEOUT = 30L
    }

    object BankDetails {
        const val SUBMIT_BUTTON_TIMEOUT = 2L
        const val LOADING_TIMEOUT = 30L
    }

    object Signature {
        const val CONTINUE_BUTTON_TIMEOUT = 1L
        const val UPLOAD_TIMEOUT = 2L
        const val LOADING_TIMEOUT = 30L
    }

    object SipAmount {
        const val CONTINUE_BUTTON_TIMEOUT = 1L
        const val LOADING_TIMEOUT = 30L
    }

    fun getTimeout(screen: String, action: String): Long {
        return when ("$screen.$action") {
            "PhoneVerification.continue" -> PhoneVerification.CONTINUE_BUTTON_TIMEOUT
            "OtpVerification.resend" -> OtpVerification.RESEND_OTP_TIMEOUT
            "OtpVerification.verify" -> OtpVerification.VERIFY_BUTTON_TIMEOUT
            "PermissionScreen.grant" -> PermissionScreen.GRANT_PERMISSIONS_TIMEOUT
            "PermissionScreen.processing" -> PermissionScreen.PROCESSING_TIMEOUT
            "PanKyc.verify" -> PanKyc.VERIFY_BUTTON_TIMEOUT
            "PreVerification.checkReadiness" -> PreVerification.CHECK_READINESS_TIMEOUT
            "NameDob.continue" -> NameDob.CONTINUE_BUTTON_TIMEOUT
            "NameDob.processing" -> NameDob.KYC_PROCESSING_TIMEOUT
            "AdditionalKyc.submit" -> AdditionalKyc.SUBMIT_BUTTON_TIMEOUT
            "AdditionalKyc.location" -> AdditionalKyc.LOCATION_TIMEOUT
            "BankDetails.submit" -> BankDetails.SUBMIT_BUTTON_TIMEOUT
            "Signature.continue" -> Signature.CONTINUE_BUTTON_TIMEOUT
            "Signature.upload" -> Signature.UPLOAD_TIMEOUT
            "SipAmount.continue" -> SipAmount.CONTINUE_BUTTON_TIMEOUT
            else -> DEFAULT_BUTTON_TIMEOUT
        }
    }

    fun getLoadingTimeout(screen: String, action: String): Long {
        return when ("$screen.$action") {
            "PhoneVerification.continue" -> PhoneVerification.LOADING_TIMEOUT
            "OtpVerification.resend" -> OtpVerification.LOADING_TIMEOUT
            "OtpVerification.verify" -> OtpVerification.LOADING_TIMEOUT
            "PermissionScreen.grant" -> PermissionScreen.LOADING_TIMEOUT
            "PermissionScreen.processing" -> PermissionScreen.LOADING_TIMEOUT
            "PanKyc.verify" -> PanKyc.LOADING_TIMEOUT
            "PreVerification.checkReadiness" -> PreVerification.LOADING_TIMEOUT
            "NameDob.continue" -> NameDob.LOADING_TIMEOUT
            "NameDob.processing" -> NameDob.LOADING_TIMEOUT
            "AdditionalKyc.submit" -> AdditionalKyc.LOADING_TIMEOUT
            "AdditionalKyc.location" -> AdditionalKyc.LOADING_TIMEOUT
            "BankDetails.submit" -> BankDetails.LOADING_TIMEOUT
            "Signature.continue" -> Signature.LOADING_TIMEOUT
            "Signature.upload" -> Signature.LOADING_TIMEOUT
            "SipAmount.continue" -> SipAmount.LOADING_TIMEOUT
            else -> DEFAULT_LOADING_TIMEOUT
        }
    }
}

