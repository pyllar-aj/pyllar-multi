package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class RedemptionRequest(
    val userId: String,
    val isin: String,
    val folioNumber: String,
    val amount: Double,
    val mode: String? = "NORMAL"
)
