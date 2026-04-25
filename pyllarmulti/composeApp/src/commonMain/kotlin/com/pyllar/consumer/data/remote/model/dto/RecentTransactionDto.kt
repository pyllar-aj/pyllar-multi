package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecentTransactionDto(
    val transactionId: String?,
    val fundId: String?,
    val fundName: String?,
    val transactionType: String?,
    val amount: Double?,
    val units: Double?,
    val nav: Double?,
    val transactionDate: String?,
    val status: String?,
    val remarks: String?
)
