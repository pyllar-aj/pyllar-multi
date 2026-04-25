package com.pyllar.consumer.domain.models

data class PhoneVerificationResponse(
    val userExists: Boolean,
    val message: String
)

