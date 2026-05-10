package com.pyllar.consumer.presentation.dashboard

data class InvestmentDashboardV2State(
    val totalValue: Double = 0.0,
    val profitLoss: Double = 0.0,
    val profitLossPercentage: Double = 0.0,
    val primaryGoals: List<InvestmentGoal> = emptyList(),
    val recommendedGoals: List<InvestmentGoal> = emptyList(),
    val allGoals: List<InvestmentGoal> = emptyList(),
    val fundDetails: List<FundDetail> = emptyList(),
    val holdingsDetails: List<HoldingDetail> = emptyList(),
    val milestoneMessage: String = "",
    val monthsInvested: Int = 0,
    val hasFirstMilestone: Boolean = false,
    val hasFirstLakhMilestone: Boolean = false,
    val lakhMilestoneMessage: String = "",
    val firstMilestoneTimestamp: Long = 0,
    val firstLakhMilestoneTimestamp: Long = 0,
    val kycStatus: String = "PENDING",
    val userName: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class InvestmentGoal(
    val goalId: String,
    val name: String,
    val description: String,
    val iconType: String,
    val targetAmount: Double,
    val investedAmount: Double,
    val cummulativeValue: Double = 0.0,
    val currentValue: Double,
    val returnsPercentage: Double,
    val progressPercentage: Double,
    val timeRemainingMonths: Int,
    val recommendedMonthlyAmount: Double,
    val recommendedDailyAmount: Double,
    val category: String,
    val colorTheme: String,
    val actionButtonText: String,
    val targetDate: String,
    val schemeName: String? = null,
    val folioNo: String? = null,
    val planNumber: String? = null,
    val createdDate: String? = null,
    val transactions: List<com.pyllar.consumer.data.remote.model.dto.RecentTransactionDto> = emptyList(),
    val isin: String? = null,
    val investmentInProgressValue: Double = 0.0,
    val planSummary: PlanSummary? = null,
    val unitsInGm: Double? = null,
    val profit: Double = 0.0,
    val realizedProfit: Double = 0.0,
    val unrealizedProfit: Double = 0.0,
    val instantRedemptionValue: Double = 0.0,
    val redemptionInProgress: Double = 0.0,
    val redeemableAmount: Double = 0.0
)

data class PlanSummary(
    val amount: Double,
    val nextSipDate: String?,
    val status: String?,
    val frequency: String?
)

data class FundDetail(
    val purpose: String,
    val isin: String,
    val amc: String?,
    val schemeName: String,
    val folioNo: String?,
    val totalInvested: Double,
    val currentValue: Double,
    val plans: List<String>,
    val mandateStatus: String?
)

data class HoldingDetail(
    val folioNumber: String?,
    val isin: String?,
    val schemeName: String?,
    val marketValueAsOn: String?,
    val marketValueAmount: Double?
)

data class InitialDashboardGoalsState(
    val primaryGoals: List<InvestmentGoal> = emptyList(),
    val recommendedGoals: List<InvestmentGoal> = emptyList(),
    val isLoading: Boolean = true,
    val growthData: Map<String, BucketGrowthData> = emptyMap()
)

data class BucketGrowthData(
    val accumulatedUnits: Double,
    val totalInvestment: Double,
    val inputAmount: Double,
    val currentValuation: Double,
    val fundType: String,
    val startDate: String,
    val tradingDays: Int
)
