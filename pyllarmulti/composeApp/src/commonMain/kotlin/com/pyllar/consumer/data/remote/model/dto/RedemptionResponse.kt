package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName

@Serializable
data class RedemptionResponse(
    @SerialName("redemptionId") val redemptionId: String? = null,
    @SerialName("redemptionGroupId") val redemptionGroupId: String? = null,
    @SerialName("transactionId") val transactionId: String? = null,
    @SerialName("transactionType") val transactionType: String? = null,
    @SerialName("amount") val amount: Double? = null,
    @SerialName("transactionDate") val transactionDate: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("fundName") val fundName: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("referenceNumber") val referenceNumber: String? = null,
    @SerialName("message") val message: String? = null
)
