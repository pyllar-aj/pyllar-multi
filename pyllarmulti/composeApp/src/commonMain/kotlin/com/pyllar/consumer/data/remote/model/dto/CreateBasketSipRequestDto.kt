package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body for POST api/goal-basket/sip. Same fields the app already sends to create-daily-sip (userInvPurpose is the goal id)
 * plus the frequency; the server reads the purpose from the goal and decides the schemes and the split.
 */
@Serializable
data class CreateBasketSipRequestDto(
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
    /** Day of the month for monthly SIPs; null for daily. */
    @SerialName("installmentDay")
    val installmentDay: Int? = null
)

