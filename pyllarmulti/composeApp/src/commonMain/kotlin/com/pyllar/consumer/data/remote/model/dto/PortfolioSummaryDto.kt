package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class PortfolioSummaryDto(
    val totalInvested: Double?,
    val currentValue: Double?,
    val totalReturns: Double?,
    val returnsPercentage: Double?,
    val totalXirr: Double?,
    val todaysGainLoss: Double?,
    val todaysGainLossPercentage: Double?,
    val totalFunds: Int?,
    val activeSipCount: Int?,
    val totalDailySipAmount: Double?,
    val totalMonthlySipAmount: Double?,
    val assetAllocation: AssetAllocationDto?,
    val topPerformingFund: String?,
    val topFundReturns: Double?,
    val lastUpdatedAt: String?
)
