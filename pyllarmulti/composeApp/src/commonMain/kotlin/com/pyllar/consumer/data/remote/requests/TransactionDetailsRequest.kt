package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTO for fetching transaction details
 */
@Serializable
data class TransactionDetailsRequest(
    @SerialName("userId")
    val userId: String,

    @SerialName("userInvestmentPurposeId")
    val userInvestmentPurposeId: String
)

