package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RedemptionRequest(
    @SerialName("userId")
    val userId: String,
    @SerialName("isin")
    val isin: String,
    @SerialName("folioNumber")
    val folioNumber: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("redemptionMode")
    val mode: String? = null
)
