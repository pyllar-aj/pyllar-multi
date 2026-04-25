package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class RedemptionResponse(
    val transactionId: String?,
    val transactionType: String?,
    val amount: Double?,
    val transactionDate: String?,
    val status: String?,
    val fundName: String?,
    val description: String?,
    val referenceNumber: String?,
    val message: String?
)
