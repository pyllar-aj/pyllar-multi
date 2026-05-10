package com.pyllar.consumer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.DashboardResponseDto
import com.pyllar.consumer.domain.repository.DashboardRepository
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class SipStatus {
    COMPLETED, PENDING, SKIPPED
}

data class MonthlyProgress(
    val completed: Int,
    val total: Int
)

data class CalendarDay(
    val day: Int,
    val isInvested: Boolean,
    val isToday: Boolean
)

data class DailyTrend(
    val date: String,
    val amount: Double
)

data class PortfolioGrowthPoint(
    val date: String,
    val value: Double
)

data class DashboardState(
    val totalInvested: Double = 0.0,
    val currentValue: Double = 0.0,
    val totalReturns: Double = 0.0,
    val xirr: Double = 0.0,
    val sipStatus: SipStatus = SipStatus.PENDING,
    val amountInvestedToday: Double = 0.0,
    val nextSipDate: String = "",
    val monthlyProgress: MonthlyProgress = MonthlyProgress(0, 30),
    val monthlyCalendar: List<CalendarDay> = emptyList(),
    val dailyTrends: List<DailyTrend> = emptyList(),
    val portfolioGrowth: List<PortfolioGrowthPoint> = emptyList(),
    val isLoading: Boolean = true,
    val username: String = "User",
    val dailySipAmount: Double = 100.0
)

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    init {
        platformLog("🏗️ DashboardViewModel CREATED")
    }

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    fun loadDashboardData(userId: String) {
        viewModelScope.launch {
            platformLog("🎯 loadDashboardData called - userId: $userId")
            _dashboardState.value = _dashboardState.value.copy(isLoading = true)

            try {
                platformLog("📡 Making API call to dashboard endpoint")
                dashboardRepository.getDashboard(userId).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val dashboardData = result.data
                            val newState = if (dashboardData != null) {
                                convertApiResponseToDashboardState(dashboardData)
                            } else {
                                platformLog("⚠️ API returned null data, using mock data")
                                processDashboardData()
                            }
                            _dashboardState.value = newState.copy(isLoading = false)
                            platformLog("✅ Dashboard state updated successfully")
                        }
                        is Resource.Error -> {
                            platformLog("❌ API Error: ${result.message}")
                            val newState = processDashboardData()
                            _dashboardState.value = newState.copy(isLoading = false)
                        }
                        is Resource.Loading -> {
                            platformLog("⏳ API still loading...")
                        }
                    }
                }

            } catch (e: Exception) {
                platformLog("💥 Exception during API call: ${e.message}")
                val newState = processDashboardData()
                _dashboardState.value = newState.copy(isLoading = false)
            }
        }
    }

    fun refreshDashboardData(userId: String) {
        loadDashboardData(userId)
    }

    fun initGoalTxn(userId: String, purpose: String) = dashboardRepository.initGoalTxn(
        com.pyllar.consumer.data.remote.requests.GoalSelectionRequest(userId, purpose)
    )

    private fun convertApiResponseToDashboardState(
        dashboardData: DashboardResponseDto
    ): DashboardState {
        val portfolioSummary = dashboardData.portfolioSummary
        val userSummary = dashboardData.userSummary

        val portfolioGrowth = dashboardData.portfolioGrowth?.map { point ->
            PortfolioGrowthPoint(
                date = point.date ?: "",
                value = point.value ?: 0.0
            )
        } ?: emptyList()

        val dailyTrends = dashboardData.recentTransactions?.take(7)?.map { transaction ->
            DailyTrend(
                date = transaction.transactionDate ?: "",
                amount = transaction.amount ?: 0.0
            )
        } ?: emptyList()

        val monthlyCalendar = dashboardData.investmentCalendar?.map { day ->
            CalendarDay(
                day = day.day ?: 0,
                isInvested = day.isInvested ?: false,
                isToday = day.isToday ?: false
            )
        } ?: emptyList()

        val sipStatus = determineSipStatus(dailyTrends)

        return DashboardState(
            totalInvested = portfolioSummary?.totalInvested ?: 0.0,
            currentValue = portfolioSummary?.currentValue ?: 0.0,
            totalReturns = portfolioSummary?.totalReturns ?: 0.0,
            xirr = portfolioSummary?.totalXirr ?: 0.0,
            sipStatus = sipStatus,
            amountInvestedToday = portfolioSummary?.totalDailySipAmount ?: 0.0,
            nextSipDate = "Tomorrow", // Simplified since Calendar/SimpleDateFormat are not common code
            monthlyProgress = MonthlyProgress(
                completed = monthlyCalendar.count { it.isInvested },
                total = monthlyCalendar.size
            ),
            monthlyCalendar = monthlyCalendar,
            dailyTrends = dailyTrends,
            portfolioGrowth = portfolioGrowth,
            isLoading = false,
            username = userSummary?.name ?: "User",
            dailySipAmount = portfolioSummary?.totalDailySipAmount ?: 100.0
        )
    }

    private fun determineSipStatus(dailyTrends: List<DailyTrend>): SipStatus {
        val todayTrend = dailyTrends.firstOrNull() // Simplify for KMP without full Date formatting
        
        return when {
            todayTrend != null && todayTrend.amount > 0 -> SipStatus.COMPLETED
            todayTrend != null && todayTrend.amount == 0.0 -> SipStatus.SKIPPED
            else -> SipStatus.PENDING
        }
    }

    private fun processDashboardData(): DashboardState {
        val sipAmount = 100.0
        val username = "User"
        
        val dailySipAmount = sipAmount
        val daysInMonth = 30
        val monthlyInvestment = dailySipAmount * daysInMonth
        val totalInvested = monthlyInvestment * 12
        val mockCurrentValue = totalInvested * 1.10
        val mockReturns = mockCurrentValue - totalInvested
        val mockXirr = 12.5
        
        val monthlyCalendar = generateMonthlyCalendar()
        val dailyTrends = generateDailyTrends(dailySipAmount)
        val portfolioGrowth = generatePortfolioGrowth(totalInvested)
        
        return DashboardState(
            totalInvested = totalInvested,
            currentValue = mockCurrentValue,
            totalReturns = mockReturns,
            xirr = mockXirr,
            sipStatus = SipStatus.COMPLETED,
            amountInvestedToday = dailySipAmount,
            nextSipDate = "Tomorrow",
            monthlyProgress = MonthlyProgress(25, 30),
            monthlyCalendar = monthlyCalendar,
            dailyTrends = dailyTrends,
            portfolioGrowth = portfolioGrowth,
            isLoading = false,
            username = username,
            dailySipAmount = dailySipAmount
        )
    }

    private fun generateMonthlyCalendar(): List<CalendarDay> {
        val daysInMonth = 30
        val today = 15
        
        return (1..daysInMonth).map { day ->
            CalendarDay(
                day = day,
                isInvested = day % 2 == 0,
                isToday = day == today
            )
        }
    }

    private fun generateDailyTrends(dailySipAmount: Double): List<DailyTrend> {
        return (0..6).map { daysAgo ->
            val date = "Day -$daysAgo"
            val amount = dailySipAmount + (if (daysAgo % 2 == 0) 1 else -1) * (dailySipAmount * 0.1)
            DailyTrend(date = date, amount = amount)
        }
    }

    private fun generatePortfolioGrowth(baseInvestment: Double): List<PortfolioGrowthPoint> {
        var baseValue = baseInvestment * 0.8
        
        return (0..29).map { daysAgo ->
            val date = "Day -$daysAgo"
            baseValue += (if (daysAgo % 3 == 0) 1 else -1) * (baseInvestment * 0.01)
            PortfolioGrowthPoint(date = date, value = baseValue)
        }
    }
}
