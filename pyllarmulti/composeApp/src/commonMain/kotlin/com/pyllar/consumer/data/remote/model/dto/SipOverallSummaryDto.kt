package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SipOverallSummaryDto(
    val totalActiveSips: Int?,
    val totalDailySipAmount: Double?,
    val totalMonthlySipAmount: Double?,
    val totalInvestedAmount: Double?,
    val nextSipDate: String?,
    val lastSipDate: String?,
    val upcomingSips: List<UpcomingSipDto>?,
    val sipPerformance: SipPerformanceDto?
)
