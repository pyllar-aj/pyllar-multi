package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateDailySipRequestDto(
    val userId: String,
    val kycAttemptId: String,
    val investorId: String,
    val amount: Double,
    val userInvPurpose: String
)
