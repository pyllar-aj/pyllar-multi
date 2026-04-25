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
    val firstMilestoneTimestamp: Long = 0L,
    val firstLakhMilestoneTimestamp: Long = 0L,
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
    val currentValue: Double,
    val cummulativeValue: Double = 0.0,
    val investmentInProgressValue: Double = 0.0,
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
    val isin: String? = null,
    val planSummary: PlanSummary? = null,
    val unitsInGm: Double? = null,
    val profit: Double = 0.0,
    val realizedProfit: Double = 0.0,
    val unrealizedProfit: Double = 0.0
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
    val plans: List<PlanDetail>,
    val mandateStatus: String? = null
)

data class PlanDetail(
    val amount: Double,
    val createdAt: String,
    val nextInstallmentDate: String?
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
    val growthData: Map<String, BucketGrowthData> = emptyMap(),
    val isLoading: Boolean = true
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

