package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardResponseDto(
    val userSummary: UserSummaryDto?,
    val portfolioSummary: PortfolioSummaryDto?,
    val fundInvestments: List<FundInvestmentDto>?,
    val recentTransactions: List<RecentTransactionDto>?,
    val portfolioGrowth: List<PortfolioGrowthPointDto>?,
    val investmentCalendar: List<CalendarDayDto>?,
    val goalProgress: GoalProgressDto?
)
