package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class FundPerformanceDto(
    val oneDayReturn: Double?,
    val oneWeekReturn: Double?,
    val oneMonthReturn: Double?,
    val threeMonthReturn: Double?,
    val sixMonthReturn: Double?,
    val oneYearReturn: Double?,
    val threeYearReturn: Double?,
    val fiveYearReturn: Double?,
    val sinceInceptionReturn: Double?,
    val volatility: Double?,
    val sharpeRatio: Double?
)
