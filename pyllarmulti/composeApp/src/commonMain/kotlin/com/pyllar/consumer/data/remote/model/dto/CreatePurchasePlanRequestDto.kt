package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class CreatePurchasePlanRequestDto(
    @SerialName("userId")
    val userId: String,
    @SerialName("kycAttemptId")
    val kycAttemptId: String,
    @SerialName("investorId")
    val investorId: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("userInvPurpose")
    val userInvPurpose: String,
    @SerialName("frequency")
    val frequency: String,
    @SerialName("installmentDay")
    val installmentDay: Int? = null,
    @SerialName("numberOfInstallments")
    val numberOfInstallments: Int? = null
)
