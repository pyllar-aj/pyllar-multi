package com.pyllar.consumer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.GoalSelectionResponseDto
import com.pyllar.consumer.data.remote.requests.GoalSelectionRequest
import com.pyllar.consumer.domain.repository.CommonRepository
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Redundant data classes removed. Using central definitions in InvestmentDashboardModels.kt

class InitialDashboardViewModel(
    private val commonRepository: CommonRepository,
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _goalsState = MutableStateFlow(InitialDashboardGoalsState())
    val goalsState: StateFlow<InitialDashboardGoalsState> = _goalsState.asStateFlow()

    init {
        _goalsState.value = InitialDashboardGoalsState(
            primaryGoals = emptyList(),
            recommendedGoals = createAllPresetGoals(),
            isLoading = false
        )
        platformLog("✅ InitialDashboardViewModel initialized with preset goals")
        fetchDashboardGrowthData()
    }

    private fun fetchDashboardGrowthData() {
        viewModelScope.launch {
            try {
                platformLog("🚀 Fetching dashboard growth data")
                commonRepository.fetchScreenData("InitialDashboard").collectLatest { result ->
                    if (result is Resource.Success) {
                        val screenData = result.data?.data
                        if (screenData != null) {
                            val growthDataMap = parseGrowthData(screenData)
                            _goalsState.value = _goalsState.value.copy(growthData = growthDataMap)
                            platformLog("✅ Parsed growth data for ${growthDataMap.size} buckets")
                        }
                    } else if (result is Resource.Error) {
                        platformLog("❌ Error fetching growth data: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                platformLog("❌ Exception fetching growth data: ${e.message}")
            }
        }
    }

    private fun parseGrowthData(data: Map<String, Any?>): Map<String, BucketGrowthData> {
        val result = mutableMapOf<String, BucketGrowthData>()
        data.forEach { (key, value) ->
            if (value is Map<*, *>) {
                try {
                    // Primitive conversion for KMP Map parsing
                    val bucket = BucketGrowthData(
                        accumulatedUnits = (value["accumulatedUnits"] as? Number)?.toDouble() ?: 0.0,
                        totalInvestment = (value["totalInvestment"] as? Number)?.toDouble() ?: 0.0,
                        inputAmount = (value["inputAmount"] as? Number)?.toDouble() ?: 0.0,
                        currentValuation = (value["currentValuation"] as? Number)?.toDouble() ?: 0.0,
                        fundType = (value["fundType"] as? String) ?: "",
                        startDate = (value["startDate"] as? String) ?: "Jan 2025",
                        tradingDays = (value["tradingDays"] as? Number)?.toInt() ?: 0
                    )
                    result[key] = bucket
                } catch (e: Exception) {
                    platformLog("⚠️ Error parsing bucket $key: ${e.message}")
                }
            }
        }
        return result
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
                targetDate = "Jan 2055" // Hardcoded in KMP for simplicity without Calendar
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
                targetDate = "Jan 2055"
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
                targetDate = "Feb 2028"
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
                targetDate = "Jul 2026"
            ),
            InvestmentGoal(
                goalId = "childrens_education",
                name = "Education",
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
                targetDate = "Jan 2040"
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
                targetDate = "Jan 2028"
            )
        )
    }

    suspend fun selectGoal(
        userId: String,
        goalId: String
    ): Resource<GoalSelectionResponseDto> {
        val request = GoalSelectionRequest(
            userId = userId,
            goal = goalId
        )
        
        var finalResult: Resource<GoalSelectionResponseDto> = Resource.Loading()
        
        onboardingRepository.selectGoal(request).collectLatest { result ->
            finalResult = result
            when (result) {
                is Resource.Success -> {
                    // Logic for saving user_purpose_id can be done outside this ViewModel
                    // Since it requires a SessionStore or context. Let's just return the result.
                    platformLog("✅ Goal selected successfully")
                }
                is Resource.Error -> {
                    platformLog("❌ Error selecting goal: ${result.message}")
                }
                is Resource.Loading -> { }
            }
        }
        
        return finalResult
    }
}
