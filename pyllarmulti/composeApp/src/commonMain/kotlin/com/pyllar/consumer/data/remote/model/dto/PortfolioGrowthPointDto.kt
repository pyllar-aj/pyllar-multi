package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class PortfolioGrowthPointDto(
    val date: String?,
    val value: Double?,
    val investedAmount: Double?,
    val currentValue: Double?
)
