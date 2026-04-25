package com.pyllar.consumer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.CurrentInvestmentDto
import com.pyllar.consumer.data.remote.model.dto.InvestorDashboardResponseV2Dto
import com.pyllar.consumer.data.remote.model.dto.RecentTransactionDto
import com.pyllar.consumer.data.remote.model.dto.RecommendationDto
import com.pyllar.consumer.domain.repository.DashboardRepository
import com.pyllar.consumer.util.Log
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

class InvestmentDashboardV2ViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    companion object {
        private const val TAG = "InvestmentDashboardV2ViewModel"
        private const val MILLIS_IN_DAY: Long = 24L * 60L * 60L * 1000L
    }

    private val _dashboardState = MutableStateFlow(InvestmentDashboardV2State())
    val dashboardState: StateFlow<InvestmentDashboardV2State> = _dashboardState

    fun loadDashboardData(userId: String) {
        viewModelScope.launch {
            Log.d(TAG, "loadDashboardData called - userId: $userId")
            _dashboardState.value = _dashboardState.value.copy(isLoading = true, errorMessage = null)

            dashboardRepository.getDashboardV2(userId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val response = result.data
                        if (response != null) {
                            val newState = mapV2ResponseToState(response, null)
                            _dashboardState.value = newState.copy(isLoading = false, errorMessage = null)
                        } else {
                            _dashboardState.value = _dashboardState.value.copy(
                                isLoading = false,
                                errorMessage = "We are facing issues connecting to our servers. Please try again later."
                            )
                        }
                    }
                    is Resource.Error -> {
                        val errorMsg = result.message ?: ""
                        Log.e(TAG, "API Error: $errorMsg")
                        val isNetworkError = result.isNetworkError ||
                            errorMsg.contains("Failed to connect", ignoreCase = true) ||
                            errorMsg.contains("Unable to connect", ignoreCase = true) ||
                            errorMsg.contains("Network", ignoreCase = true) ||
                            errorMsg.contains("timeout", ignoreCase = true) ||
                            errorMsg.contains("SocketTimeout", ignoreCase = true) ||
                            errorMsg.contains("UnknownHost", ignoreCase = true) ||
                            errorMsg.contains("IOException", ignoreCase = true) ||
                            errorMsg.contains("No address associated", ignoreCase = true)

                        _dashboardState.value = _dashboardState.value.copy(
                            isLoading = false,
                            errorMessage = if (isNetworkError) {
                                "Unable to connect to server. Please check your internet connection and try again."
                            } else {
                                "We are facing issues connecting to our servers. Please try again later."
                            }
                        )
                    }
                    is Resource.Loading -> {
                        _dashboardState.value = _dashboardState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }

    fun refreshDashboardData(userId: String) {
        loadDashboardData(userId)
    }

    suspend fun initGoalTxn(userId: String, goalId: String): Resource<com.pyllar.consumer.data.remote.model.dto.GoalSelectionResponseDto> {
        Log.d(TAG, "initGoalTxn called - userId: $userId, goalId: $goalId")
        val request = com.pyllar.consumer.data.remote.requests.GoalSelectionRequest(
            userId = userId,
            goal = goalId
        )
        
        var finalResult: Resource<com.pyllar.consumer.data.remote.model.dto.GoalSelectionResponseDto> = Resource.Loading()
        
        dashboardRepository.initGoalTxn(request).collect { result ->
            finalResult = result
        }
        
        return finalResult
    }

    fun clearErrorMessage() {
        _dashboardState.value = _dashboardState.value.copy(errorMessage = null)
    }

    private fun mapV2ResponseToState(
        response: InvestorDashboardResponseV2Dto,
        transactions: List<RecentTransactionDto>? = null
    ): InvestmentDashboardV2State {
        val portfolio = response.portfolioSummary
        val currentInvestments = response.currentInvestments.orEmpty()

        val successfulCredits = emptyList<RecentTransactionDto>()
        val successfulRedemptions = emptyList<RecentTransactionDto>()

        val investedFromPortfolio = portfolio?.totalInvestedAmount ?: 0.0
        val investedFromInvestments = currentInvestments.sumOf { it.investedAmount ?: 0.0 }

        val totalInvested = if (investedFromPortfolio > 0) investedFromPortfolio else investedFromInvestments

        val actualInvestmentDays = 0

        Log.d(TAG, "Portfolio Summary:")
        Log.d(TAG, "   Net Invested (portfolio): ₹${formatIndian(investedFromPortfolio)}")
        Log.d(TAG, "   Net Invested (investments): ₹${formatIndian(investedFromInvestments)}")
        Log.d(TAG, "   Selected Net Invested: ₹${formatIndian(totalInvested)}")

        val currentValueFromInvestments = currentInvestments.sumOf { it.currentValue ?: 0.0 }

        val cummulativeValueFromInvestments = currentInvestments.sumOf { investment ->
            investment.cummulativeValue
                ?: ((investment.currentValue ?: 0.0) + (investment.investmentInProgressValue ?: 0.0))
        }

        val totalValue = when {
            cummulativeValueFromInvestments > 0 -> cummulativeValueFromInvestments
            portfolio?.totalValue != null && portfolio.totalValue!! > 0 -> portfolio.totalValue!!
            currentValueFromInvestments > 0 -> currentValueFromInvestments
            else -> 0.0
        }

        val investedAmount = when {
            portfolio?.totalInvestedAmount != null && portfolio.totalInvestedAmount!! > 0 -> portfolio.totalInvestedAmount!!
            investedFromInvestments > 0 -> investedFromInvestments
            else -> 0.0
        }

        val profitLoss = if (totalValue > 0 && investedAmount > 0) {
            totalValue - investedAmount
        } else {
            portfolio?.profitAmount ?: 0.0
        }

        val profitLossPercentage = if (totalValue > 0 && investedAmount > 0) {
            calculatePercentage(totalValue, investedAmount)
        } else {
            portfolio?.profitPercentage ?: 0.0
        }

        val totalCurrentValue = totalValue

        Log.d(TAG, "   Portfolio totalValue (from API): ₹${formatIndian(portfolio?.totalValue ?: 0.0)}")
        Log.d(TAG, "   Portfolio investedAmount (from API): ₹${formatIndian(portfolio?.totalInvestedAmount ?: 0.0)}")
        Log.d(TAG, "   Current Value from ALL Investments (sum): ₹${formatIndian(currentValueFromInvestments)}")
        Log.d(TAG, "   Cumulative Value from ALL Investments (sum): ₹${formatIndian(cummulativeValueFromInvestments)}")
        Log.d(TAG, "   Invested Amount from ALL Investments (sum): ₹${formatIndian(investedFromInvestments)}")
        Log.d(TAG, "   Total Invested (selected from priority): ₹${formatIndian(totalInvested)}")
        Log.d(TAG, "   Using totalValue (cumulative) for TotalValueCard: ₹${formatIndian(totalValue)}")
        Log.d(TAG, "   Using investedAmount for TotalValueCard: ₹${formatIndian(investedAmount)}")
        Log.d(TAG, "   Calculated profitLoss: ₹${formatIndian(profitLoss)}")
        Log.d(TAG, "   Calculated profitLossPercentage: ${formatPercent(profitLossPercentage)}%")

        val kycStatus = response.kycDetails?.kycStatus ?: "PENDING"

        val primaryGoals = currentInvestments.map { investment ->
            mapInvestmentToGoal(investment, successfulCredits, successfulRedemptions)
        }.sortedByDescending { it.currentValue }

        val activeGoalIds = primaryGoals.map { it.goalId }.toSet()
        val recommendedGoalsFromApi = buildRecommendedGoals(response.recommendations.orEmpty(), activeGoalIds)
        val presetGoals = createAllPresetGoals().filter { it.goalId !in activeGoalIds }

        val allInOneGoal = presetGoals.find { it.goalId == "all_in_one" }
        val globalExposureGoal = presetGoals.find { it.goalId == "global_exposure" }
        val otherPresetGoals = presetGoals.filter {
            it.goalId != "all_in_one" && it.goalId != "global_exposure"
        }

        val allRecommendedGoals = if (recommendedGoalsFromApi.isEmpty()) {
            otherPresetGoals
        } else {
            val apiGoalIds = recommendedGoalsFromApi.map { it.goalId }.toSet()
            val additionalPresetGoals = otherPresetGoals.filter { it.goalId !in apiGoalIds }
            recommendedGoalsFromApi + additionalPresetGoals
        }

        val recommendedGoals = allRecommendedGoals.sortedBy { goal ->
            when (goal.category.uppercase()) {
                "GOLD" -> 1
                "SILVER" -> 2
                "SAVINGS" -> 3
                "FESTIVAL_SPENDS" -> 4
                else -> 7
            }
        }

        val showAll = response.showAll ?: false
        val allGoals = if (showAll) {
            listOfNotNull(allInOneGoal, globalExposureGoal)
        } else {
            emptyList()
        }

        val fundDetails = currentInvestments.flatMap { investment ->
            val purpose = investment.purpose ?: "Unknown"
            investment.folioDetails.orEmpty().map { folio ->
                FundDetail(
                    purpose = purpose,
                    isin = folio.isin ?: "",
                    amc = null,
                    schemeName = folio.fundName ?: "Unknown Scheme",
                    folioNo = folio.folioNumber,
                    totalInvested = folio.investmentAmount ?: 0.0,
                    currentValue = folio.currentValue ?: (folio.investmentAmount ?: 0.0),
                    plans = emptyList(),
                    mandateStatus = investment.mandateStatus
                )
            }
        }

        val holdingsDetails = currentInvestments.flatMap { investment ->
            investment.folioDetails.orEmpty().map { folio ->
                HoldingDetail(
                    folioNumber = folio.folioNumber,
                    isin = folio.isin,
                    schemeName = folio.fundName,
                    marketValueAsOn = null,
                    marketValueAmount = folio.currentValue
                )
            }
        }

        val (nowYear, nowMonth) = com.pyllar.consumer.util.currentYearMonth()
        val nowTotalMonths = nowYear * 12 + nowMonth
        val monthsInvested = currentInvestments.mapNotNull { parseDateMonthsAgo(it.createdAt) }
            .minOrNull()
            ?.let { (nowTotalMonths - it).coerceAtLeast(0) }
            ?: 0

        val milestoneAmount = maxOf(investedAmount, totalCurrentValue)
        val (milestoneMessage, hasFirstMilestone) = getMilestoneMessage(milestoneAmount)
        val hasFirstLakhMilestoneByAmount = investedAmount >= 100000.0 || totalValue >= 100000.0

        val firstMilestoneTimestamp = 0L
        val lakhMilestoneTimestamp = 0L

        val finalHasFirstMilestone = hasFirstMilestone
        val finalHasFirstLakhMilestone = hasFirstLakhMilestoneByAmount

        Log.d(TAG, "Milestone Calculations:")
        Log.d(TAG, "   Invested Amount (from portfolio): ₹${formatIndian(investedAmount)}")
        Log.d(TAG, "   Total Value (current): ₹${formatIndian(totalCurrentValue)}")
        Log.d(TAG, "   Milestone Amount: ₹${formatIndian(milestoneAmount)}")
        Log.d(TAG, "   Milestone message: $milestoneMessage")

        return InvestmentDashboardV2State(
            totalValue = totalCurrentValue,
            profitLoss = profitLoss,
            profitLossPercentage = profitLossPercentage,
            primaryGoals = primaryGoals,
            recommendedGoals = recommendedGoals,
            allGoals = allGoals,
            fundDetails = fundDetails,
            holdingsDetails = holdingsDetails,
            milestoneMessage = milestoneMessage,
            monthsInvested = monthsInvested,
            hasFirstMilestone = finalHasFirstMilestone,
            hasFirstLakhMilestone = finalHasFirstLakhMilestone,
            lakhMilestoneMessage = if (finalHasFirstLakhMilestone) "First ₹1L milestone reached! Superb progress." else "",
            firstMilestoneTimestamp = firstMilestoneTimestamp,
            firstLakhMilestoneTimestamp = lakhMilestoneTimestamp,
            kycStatus = response.kycDetails?.kycStatus ?: "PENDING",
            userName = response.userName ?: "",
            isLoading = false
        )
    }

    private fun mapInvestmentToGoal(
        investment: CurrentInvestmentDto,
        successfulCredits: List<RecentTransactionDto>,
        successfulRedemptions: List<RecentTransactionDto>
    ): InvestmentGoal {
        val purposeCode = investment.purpose?.lowercase() ?: "saving"
        val folioFundNames = investment.folioDetails.orEmpty().mapNotNull { it.fundName }.toSet()

        val purposeTransactions = emptyList<RecentTransactionDto>()

        val investedAmount = (investment.investedAmount ?: 0.0)
        val redemptionsForPurpose = 0.0

        val netInvestedAmount = (investedAmount - redemptionsForPurpose).coerceAtLeast(0.0)
        val currentValue = investment.currentValue ?: netInvestedAmount
        val returnsPercentage = if (investment.currentValuePercentage != null && investment.currentValuePercentage!! > 0) {
            investment.currentValuePercentage!!
        } else if (currentValue > 0 && netInvestedAmount > 0) {
            calculatePercentage(currentValue, netInvestedAmount)
        } else {
            0.0
        }

        Log.d(
            TAG,
            "   Investment: ${investment.purpose}, investedAmount: ₹${investedAmount}, currentValue: ₹${currentValue}, netInvestedAmount: ₹${netInvestedAmount}, returnsPercentage: ${formatPercent(returnsPercentage)}%"
        )

        val monthlySipAmountFromApi = investment.folioDetails.orEmpty()
            .sumOf { it.sipAmount ?: 0.0 }
        val dailySipAmount = monthlySipAmountFromApi / 30.0

        val monthlySipAmount = monthlySipAmountFromApi

        val firstFolio = investment.folioDetails.orEmpty().firstOrNull()

        val cummulativeValue = investment.cummulativeValue
            ?: (currentValue + (investment.investmentInProgressValue ?: 0.0))

        val goal = createPrimaryGoal(
            selectedGoalType = purposeCode,
            totalInvested = netInvestedAmount,
            totalValue = currentValue,
            cummulativeValue = cummulativeValue,
            returnsPercentage = returnsPercentage,
            dailySipAmount = dailySipAmount,
            monthlySipAmount = monthlySipAmount,
            schemeName = firstFolio?.fundName,
            folioNo = firstFolio?.folioNumber,
            planNumber = null,
            createdDate = investment.createdAt,
            transactions = purposeTransactions,
            isin = firstFolio?.isin
                ?: investment.folioDetails.orEmpty().firstNotNullOfOrNull { it.isin }
        )

        val progressOverride = investment.progressPercentage?.toDouble()
        val timeOverride = investment.timeRemainingMonths

        val planSummaryDto = investment.totalSipSummary
        val planSummary = planSummaryDto?.let { dto ->
            PlanSummary(
                amount = dto.amount ?: 0.0,
                nextSipDate = dto.nextSipDate,
                status = dto.status,
                frequency = dto.frequency
            )
        }

        return goal.copy(
            progressPercentage = progressOverride ?: goal.progressPercentage,
            timeRemainingMonths = timeOverride ?: goal.timeRemainingMonths,
            investmentInProgressValue = investment.investmentInProgressValue ?: goal.investmentInProgressValue,
            planSummary = planSummary,
            unitsInGm = investment.unitsInGm,
            profit = investment.profit ?: 0.0,
            realizedProfit = investment.realizedProfit ?: 0.0,
            unrealizedProfit = investment.unrealizedProfit ?: 0.0
        )
    }

    private fun buildRecommendedGoals(
        recommendations: List<RecommendationDto>,
        activeGoalIds: Set<String>
    ): List<InvestmentGoal> {
        if (recommendations.isEmpty()) return emptyList()

        return recommendations.mapNotNull { recommendation ->
            val rawPurpose = recommendation.purpose ?: return@mapNotNull null
            val goalId = rawPurpose.lowercase()
            if (goalId in activeGoalIds) return@mapNotNull null

            val monthly = recommendation.sipAmount ?: 0.0
            val target = recommendation.totalInvestmentAmount ?: (monthly * 12)

            InvestmentGoal(
                goalId = goalId,
                name = rawPurpose.replace("_", " ").split(" ").joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                },
                description = recommendation.message ?: "Suggested goal to improve your portfolio.",
                iconType = resolveIcon(goalId),
                targetAmount = target,
                investedAmount = 0.0,
                currentValue = 0.0,
                returnsPercentage = 0.0,
                progressPercentage = 0.0,
                timeRemainingMonths = 12,
                recommendedMonthlyAmount = monthly,
                recommendedDailyAmount = if (monthly > 0) monthly / 30 else 0.0,
                category = goalId.uppercase(),
                colorTheme = "blue",
                actionButtonText = "Start Planning",
                targetDate = calculateTargetDate(12),
                schemeName = null,
                folioNo = null,
                planNumber = null,
                createdDate = null
            )
        }
    }

    private fun calculatePercentage(currentValue: Double, invested: Double): Double {
        if (invested <= 0) return 0.0
        val gain = currentValue - invested
        return (gain / invested) * 100.0
    }

    private fun resolveIcon(purpose: String?): String {
        return when (purpose?.lowercase()) {
            "retirement" -> "🛡️"
            "vacation" -> "✈️"
            "childrens_education" -> "🎓"
            "festival_spends" -> "🎊"
            "emergency_fund" -> "🚑"
            "gold" -> "🪙"
            "silver" -> "⚪"
            "savings" -> "💰"
            "all_in_one" -> "🛡️"
            "global_exposure" -> "🌍"
            else -> "💡"
        }
    }

    private fun createPrimaryGoal(
        selectedGoalType: String,
        totalInvested: Double,
        totalValue: Double,
        cummulativeValue: Double,
        returnsPercentage: Double,
        dailySipAmount: Double,
        monthlySipAmount: Double,
        schemeName: String? = null,
        folioNo: String? = null,
        planNumber: String? = null,
        createdDate: String? = null,
        transactions: List<RecentTransactionDto> = emptyList(),
        isin: String? = null
    ): InvestmentGoal {
        val progressiveTarget = calculateProgressiveTarget(totalInvested)
        val progressPercentage = ((totalInvested / progressiveTarget) * 100).coerceAtMost(100.0)

        val remainingAmount = progressiveTarget - totalInvested
        val timeRemaining = run {
            val monthlyInvestmentRate = calculateMonthlyInvestmentRateFromRealData(dailySipAmount, monthlySipAmount)
            if (monthlyInvestmentRate > 0) {
                (remainingAmount / monthlyInvestmentRate).toInt().coerceAtLeast(1)
            } else {
                18
            }
        }

        Log.d(TAG, "Primary Goal Calculations:")
        Log.d(TAG, "   Total Invested: ₹${formatIndian(totalInvested)}")
        Log.d(TAG, "   Progressive Target: ₹${formatIndian(progressiveTarget)}")
        Log.d(TAG, "   Progress: $progressPercentage%")
        Log.d(TAG, "   Remaining Months: $timeRemaining")
        Log.d(TAG, "   Target Date: ${calculateTargetDate(timeRemaining)}")

        return when (selectedGoalType) {
            "festival_spends" -> InvestmentGoal(
                goalId = "festival_spends",
                name = "Festival Spends",
                description = "Save for your festive budget",
                iconType = "🎊",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "FESTIVAL_SPENDS",
                colorTheme = "orange",
                actionButtonText = "Start Planning",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate,
                isin = isin
            )
            "retirement" -> InvestmentGoal(
                goalId = "retirement",
                name = "Retirement",
                description = "Secure your golden years",
                iconType = "🛡️",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "RETIREMENT",
                colorTheme = "green",
                actionButtonText = "Secure your retirement",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate,
                isin = isin
            )
            "childrens_education" -> InvestmentGoal(
                goalId = "childrens_education",
                name = "Education",
                description = "Plan for your child's future",
                iconType = "🎓",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "CHILDRENS_EDUCATION",
                colorTheme = "blue",
                actionButtonText = "Secure Their Future",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate,
                isin = isin
            )
            "vacation" -> InvestmentGoal(
                goalId = "vacation",
                name = "Vacation",
                description = "Save for your dream getaway",
                iconType = "✈️",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "VACATION",
                colorTheme = "purple",
                actionButtonText = "Plan Trip",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate,
                isin = isin
            )
            "gold" -> InvestmentGoal(
                goalId = "gold",
                name = "Gold",
                description = "Invest in gold for wealth preservation",
                iconType = "🪙",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "GOLD",
                colorTheme = "gold",
                actionButtonText = "Start Investing",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate,
                isin = isin
            )
            "silver" -> InvestmentGoal(
                goalId = "silver",
                name = "Silver",
                description = "Invest in silver for wealth preservation",
                iconType = "⚪",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "SILVER",
                colorTheme = "silver",
                actionButtonText = "Start Investing",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate,
                isin = isin
            )
            "savings" -> InvestmentGoal(
                goalId = "savings",
                name = "Savings",
                description = "Build your emergency fund and savings",
                iconType = "💰",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "SAVINGS",
                colorTheme = "teal",
                actionButtonText = "Start Saving",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate
            )
            "all_in_one" -> InvestmentGoal(
                goalId = "all_in_one",
                name = "All-in-One",
                description = "Diversified investment across multiple asset classes",
                iconType = "🛡️",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "ALL_IN_ONE",
                colorTheme = "multi",
                actionButtonText = "Start Planning",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate,
                isin = isin
            )
            "global_exposure" -> InvestmentGoal(
                goalId = "global_exposure",
                name = "Global Exposure",
                description = "Invest in global markets for international diversification",
                iconType = "🌍",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "GLOBAL_EXPOSURE",
                colorTheme = "teal",
                actionButtonText = "Start Planning",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate,
                isin = isin
            )
            else -> InvestmentGoal(
                goalId = "saving",
                name = "Saving",
                description = "General savings and wealth building",
                iconType = "💰",
                targetAmount = progressiveTarget,
                investedAmount = totalInvested,
                cummulativeValue = cummulativeValue,
                currentValue = totalValue,
                returnsPercentage = returnsPercentage,
                progressPercentage = progressPercentage,
                timeRemainingMonths = timeRemaining,
                recommendedMonthlyAmount = monthlySipAmount,
                recommendedDailyAmount = monthlySipAmount / 30,
                category = "SAVING",
                colorTheme = "green",
                actionButtonText = "Start Planning",
                targetDate = calculateTargetDate(timeRemaining),
                schemeName = schemeName,
                folioNo = folioNo,
                planNumber = planNumber,
                createdDate = createdDate,
                isin = isin
            )
        }
    }

    private fun parseDateMonthsAgo(dateString: String?): Int? {
        if (dateString.isNullOrBlank()) return null
        return try {
            val trimmed = dateString.trim()
            val datePart = trimmed.substringBefore('T').takeIf { it.length >= 10 } ?: return null
            val parts = datePart.split('-')
            if (parts.size != 3) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            (year * 12 + month)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateProgressiveTarget(totalInvested: Double): Double {
        return when {
            totalInvested < 100000.0 -> 100000.0
            totalInvested < 200000.0 -> 200000.0
            totalInvested < 300000.0 -> 300000.0
            totalInvested < 500000.0 -> 500000.0
            totalInvested < 1000000.0 -> 1000000.0
            totalInvested < 2000000.0 -> 2000000.0
            totalInvested < 5000000.0 -> 5000000.0
            else -> 10000000.0
        }
    }

    private fun calculateMonthlyInvestmentRateFromRealData(dailySipAmount: Double, monthlySipAmount: Double): Double {
        val dailySipAsMonthly = dailySipAmount * 30.0
        val totalMonthlyInvestment = monthlySipAmount + dailySipAsMonthly

        Log.d(TAG, "Real SIP Calculation:")
        Log.d(TAG, "   Daily SIP: ₹${formatIndian(dailySipAmount)}")
        Log.d(TAG, "   Monthly SIP: ₹${formatIndian(monthlySipAmount)}")
        Log.d(TAG, "   Daily as Monthly: ₹${formatIndian(dailySipAsMonthly)}")
        Log.d(TAG, "   Total Monthly: ₹${formatIndian(totalMonthlyInvestment)}")

        return if (totalMonthlyInvestment > 0) {
            totalMonthlyInvestment
        } else {
            Log.w(TAG, "No SIP data available, using fallback estimation")
            5000.0
        }
    }

    private fun calculateTargetDate(remainingMonths: Int): String {
        val (curYear, curMonth) = com.pyllar.consumer.util.currentYearMonth()
        val totalMonths = (curYear * 12 + curMonth - 1) + remainingMonths
        val targetYear = totalMonths / 12
        val targetMonth = (totalMonths % 12) + 1

        val monthNames = arrayOf(
            "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        return "${monthNames[targetMonth]} $targetYear"
    }

    private fun createAllPresetGoals(): List<InvestmentGoal> {
        return listOf(
            InvestmentGoal(
                goalId = "gold",
                name = "Gold",
                description = "Invest in gold for wealth preservation",
                iconType = "🪙",
                targetAmount = 3600000.0,
                investedAmount = 0.0,
                currentValue = 0.0,
                returnsPercentage = 0.0,
                progressPercentage = 0.0,
                timeRemainingMonths = 360,
                recommendedMonthlyAmount = 2424.0,
                recommendedDailyAmount = 101.0,
                category = "GOLD",
                colorTheme = "gold",
                actionButtonText = "Start Investing",
                targetDate = calculateTargetDate(360)
            ),
            InvestmentGoal(
                goalId = "silver",
                name = "Silver",
                description = "Invest in silver for wealth preservation",
                iconType = "⚪",
                targetAmount = 3600000.0,
                investedAmount = 0.0,
                currentValue = 0.0,
                returnsPercentage = 0.0,
                progressPercentage = 0.0,
                timeRemainingMonths = 360,
                recommendedMonthlyAmount = 2424.0,
                recommendedDailyAmount = 101.0,
                category = "SILVER",
                colorTheme = "silver",
                actionButtonText = "Start Investing",
                targetDate = calculateTargetDate(360)
            ),
            InvestmentGoal(
                goalId = "savings",
                name = "Savings",
                description = "Build your emergency fund and savings",
                iconType = "💰",
                targetAmount = 100000.0,
                investedAmount = 0.0,
                currentValue = 0.0,
                returnsPercentage = 0.0,
                progressPercentage = 0.0,
                timeRemainingMonths = 37,
                recommendedMonthlyAmount = 2424.0,
                recommendedDailyAmount = 101.0,
                category = "SAVINGS",
                colorTheme = "teal",
                actionButtonText = "Start Saving",
                targetDate = calculateTargetDate(37)
            ),
            InvestmentGoal(
                goalId = "festival_spends",
                name = "Festival Spends",
                description = "Save for your festive budget",
                iconType = "🎊",
                targetAmount = 60000.0,
                investedAmount = 0.0,
                currentValue = 0.0,
                returnsPercentage = 0.0,
                progressPercentage = 0.0,
                timeRemainingMonths = 18,
                recommendedMonthlyAmount = 3200.0,
                recommendedDailyAmount = 115.0,
                category = "FESTIVAL_SPENDS",
                colorTheme = "orange",
                actionButtonText = "Start Planning",
                targetDate = calculateTargetDate(18)
            ),
            InvestmentGoal(
                goalId = "childrens_education",
                name = "Children's Education",
                description = "Plan for your child's future",
                iconType = "🎓",
                targetAmount = 1250000.0,
                investedAmount = 0.0,
                currentValue = 0.0,
                returnsPercentage = 0.0,
                progressPercentage = 0.0,
                timeRemainingMonths = 180,
                recommendedMonthlyAmount = 2640.0,
                recommendedDailyAmount = 110.0,
                category = "CHILDRENS_EDUCATION",
                colorTheme = "blue",
                actionButtonText = "Secure Their Future",
                targetDate = calculateTargetDate(180)
            ),
            InvestmentGoal(
                goalId = "vacation",
                name = "Vacation",
                description = "Save for your dream getaway",
                iconType = "✈️",
                targetAmount = 100000.0,
                investedAmount = 0.0,
                currentValue = 0.0,
                returnsPercentage = 0.0,
                progressPercentage = 0.0,
                timeRemainingMonths = 36,
                recommendedMonthlyAmount = 2424.0,
                recommendedDailyAmount = 101.0,
                category = "VACATION",
                colorTheme = "purple",
                actionButtonText = "Plan Trip",
                targetDate = calculateTargetDate(36)
            ),
            InvestmentGoal(
                goalId = "global_exposure",
                name = "Global Exposure",
                description = "Invest in global markets for international diversification",
                iconType = "🌍",
                targetAmount = 1000000.0,
                investedAmount = 0.0,
                currentValue = 0.0,
                returnsPercentage = 0.0,
                progressPercentage = 0.0,
                timeRemainingMonths = 180,
                recommendedMonthlyAmount = 5000.0,
                recommendedDailyAmount = 166.67,
                category = "GLOBAL_EXPOSURE",
                colorTheme = "teal",
                actionButtonText = "Start Planning",
                targetDate = calculateTargetDate(180)
            ),
            InvestmentGoal(
                goalId = "all_in_one",
                name = "All-in-One",
                description = "Diversified investment across multiple asset classes",
                iconType = "🛡️",
                targetAmount = 1000000.0,
                investedAmount = 0.0,
                currentValue = 0.0,
                returnsPercentage = 0.0,
                progressPercentage = 0.0,
                timeRemainingMonths = 180,
                recommendedMonthlyAmount = 5000.0,
                recommendedDailyAmount = 166.67,
                category = "ALL_IN_ONE",
                colorTheme = "multi",
                actionButtonText = "Start Planning",
                targetDate = calculateTargetDate(180)
            )
        )
    }

    private fun getMilestoneMessage(amount: Double): Pair<String, Boolean> {
        return when {
            amount >= 500000.0 -> "₹5L milestone reached! Outstanding achievement!" to true
            amount >= 300000.0 -> "₹3L milestone reached! Amazing progress!" to true
            amount >= 200000.0 -> "₹2L milestone reached! Great work!" to true
            amount >= 100000.0 -> "₹1L milestone reached! Superb progress!" to true
            amount >= 50000.0 -> "₹50K milestone reached! Keep up the momentum!" to true
            amount >= 30000.0 -> "₹30K milestone reached! You're doing great!" to true
            amount >= 20000.0 -> "₹20K milestone reached! Excellent progress!" to true
            amount >= 10000.0 -> "₹10K milestone reached! Keep going!" to true
            else -> "" to false
        }
    }
}

