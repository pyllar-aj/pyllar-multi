package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class PastPerformanceResponseDto(
    @SerialName("fundName") val fundName: String,
    @SerialName("fundLabel") val fundLabel: String,
    @SerialName("baseUnitAmount") val baseUnitAmount: Double,
    @SerialName("startYearMonth") val startYearMonth: String,
    @SerialName("startLabel") val startLabel: String,
    @SerialName("asOfYearMonth") val asOfYearMonth: String,
    @SerialName("milestones") val milestones: List<PastPerformanceMilestoneDto>,
    @SerialName("metalName") val metalName: String? = null
)

@Serializable
data class PastPerformanceMilestoneDto(
    @SerialName("yearMonth") val yearMonth: String,
    @SerialName("dateLabel") val dateLabel: String,
    @SerialName("latest") val latest: Boolean,
    @SerialName("dailyBaselinePortfolioValue") val dailyBaselinePortfolioValue: Double,
    @SerialName("dailyBaselineInvestedValue") val dailyBaselineInvestedValue: Double,
    @SerialName("monthlyBaselinePortfolioValue") val monthlyBaselinePortfolioValue: Double,
    @SerialName("monthlyBaselineInvestedValue") val monthlyBaselineInvestedValue: Double,
    @SerialName("dailyBaselineGrams") val dailyBaselineGrams: Double? = null,
    @SerialName("monthlyBaselineGrams") val monthlyBaselineGrams: Double? = null,
    @SerialName("onetimeBaselinePortfolioValue") val onetimeBaselinePortfolioValue: Double? = null,
    @SerialName("onetimeBaselineInvestedValue") val onetimeBaselineInvestedValue: Double? = null,
    @SerialName("onetimeBaselineGrams") val onetimeBaselineGrams: Double? = null
)
