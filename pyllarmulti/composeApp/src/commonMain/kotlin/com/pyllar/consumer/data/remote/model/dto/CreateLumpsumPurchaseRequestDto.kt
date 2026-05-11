package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateLumpsumPurchaseRequestDto(
    val userId: String,
    val amount: Double,
    val userInvPurpose: String
)
