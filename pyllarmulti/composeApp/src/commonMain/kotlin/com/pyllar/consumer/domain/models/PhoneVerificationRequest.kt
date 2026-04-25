package com.pyllar.consumer.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class PhoneVerificationRequest(
    val phoneNumber: String
)
