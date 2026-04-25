package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvestorOnboardingRequest(
    @SerialName("panNumber")
    val panNumber: String,

    @SerialName("email")
    val email: String,

    @SerialName("fullName")
    val fullName: String,

    @SerialName("dateOfBirth")
    val dateOfBirth: String,

    @SerialName("address")
    val address: String
)

@Serializable
data class SipCreationRequest(
    @SerialName("fundId")
    val fundId: String,

    @SerialName("amount")
    val amount: Double,

    @SerialName("frequency")
    val frequency: String,

    @SerialName("startDate")
    val startDate: String
)

@Serializable
data class LumpsumPurchaseRequest(
    @SerialName("fundId")
    val fundId: String,

    @SerialName("amount")
    val amount: Double,

    @SerialName("purchaseDate")
    val purchaseDate: String
)