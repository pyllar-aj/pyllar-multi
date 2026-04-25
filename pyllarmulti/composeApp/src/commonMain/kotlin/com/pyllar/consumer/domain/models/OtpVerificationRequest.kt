package com.pyllar.consumer.domain.models

data class OtpVerificationRequest(
    val phoneNumber: String,
    val otp: String
)

