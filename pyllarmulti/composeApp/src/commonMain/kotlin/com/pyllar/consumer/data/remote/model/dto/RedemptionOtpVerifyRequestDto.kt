package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class RedemptionOtpVerifyRequestDto(
    val id: String?,
    val userId: String,
    val phoneNumber: String,
    val otp: String
)
