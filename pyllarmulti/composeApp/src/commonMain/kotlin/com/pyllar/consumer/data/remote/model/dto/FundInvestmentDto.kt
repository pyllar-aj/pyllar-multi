package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundInvestmentDto(
    val fundId: String?,
    val fundName: String?,
    val schemeCode: String?,
    val totalInvested: Double?,
    val currentValue: Double?,
    val totalReturns: Double?,
    val returnsPercentage: Double?,
    val xirr: Double?,
    val units: Double?,
    val nav: Double?,
    val sipDetails: SipDetailsDto?,
    val performance: FundPerformanceDto?,
    val lastUpdatedAt: String?
)
