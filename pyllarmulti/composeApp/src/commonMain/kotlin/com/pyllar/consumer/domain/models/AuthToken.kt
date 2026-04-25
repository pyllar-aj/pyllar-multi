package com.pyllar.consumer.domain.models

data class AuthToken(
    val auth_token: String? = null,
    val registration_token: String? = null,
    val token: String = auth_token ?: "",
    val userId: String? = null,
    /** OTP reference ID returned by send-otp (the `ref` field), required for verify-otp. */
    val otpRef: String? = null,
    /** Phone number sent to send-otp, forwarded so verify-otp can include it. */
    val phoneNumber: String? = null
)

