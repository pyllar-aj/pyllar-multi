package com.pyllar.consumer.presentation.dashboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import org.koin.compose.koinInject
import platform.UIKit.UIView
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.launch

object IosDashboardRegistry {
    var factory: ((
        state: InvestmentDashboardV2State,
        onNavigateToProfile: () -> Unit,
        onNavigateToHelp: () -> Unit,
        onGoalClick: (InvestmentGoal) -> Unit,
        onRecommendedGoalClick: (InvestmentGoal) -> Unit,
        onRefresh: () -> Unit
    ) -> UIView)? = null

    var updater: ((UIView, InvestmentDashboardV2State) -> Unit)? = null
}

@Composable
actual fun InvestmentDashboardPlatformView(
    userId: String,
    onNavigateToProfile: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToGoal: (String) -> Unit,
    onNavigateToSchemeDetails: (String) -> Unit,
    onNavigateToWithdraw: () -> Unit,
    onNavigateToReferral: () -> Unit,
    onStartKyc: () -> Unit,
    onRetryKyc: () -> Unit
) {
    val viewModel: InvestmentDashboardV2ViewModel = koinInject()
    val state by viewModel.dashboardState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadDashboardData(userId)
        }
    }

    val currentFactory = IosDashboardRegistry.factory
    if (currentFactory != null) {
        UIKitView(
            factory = {
                currentFactory(
                    state,
                    onNavigateToProfile,
                    onNavigateToHelp,
                    { goal: InvestmentGoal ->
                        coroutineScope.launch {
                            val result = viewModel.initGoalTxn(userId, goal.goalId)
                            if (result is Resource.Success) {
                                val response = result.data
                                if (response != null) {
                                    val params = SchemeDetailsParams(
                                        isin = goal.isin,
                                        folioNumber = goal.folioNo,
                                        schemeName = goal.schemeName,
                                        currentValue = goal.currentValue,
                                        investmentInProgress = goal.investmentInProgressValue,
                                        investedAmount = goal.investedAmount,
                                        goalName = goal.name,
                                        unitsInGm = goal.unitsInGm,
                                        category = goal.category,
                                        colorTheme = goal.colorTheme,
                                        profit = goal.profit,
                                        realizedProfit = goal.realizedProfit,
                                        unrealizedProfit = goal.unrealizedProfit,
                                        redeemableAmount = goal.redeemableAmount,
                                        redemptionInProgress = goal.redemptionInProgress,
                                        instantRedemptionValue = goal.instantRedemptionValue,
                                        selectedTab = 0,
                                        userPurposeId = response.userPurposeId
                                    )
                                    SchemeDetailsParamsManager.set(params)
                                    onNavigateToSchemeDetails(goal.goalId)
                                }
                            }
                        }
                    },
                    { goal: InvestmentGoal ->
                        coroutineScope.launch {
                            val result = viewModel.initGoalTxn(userId, goal.goalId)
                            if (result is Resource.Success) {
                                onNavigateToGoal(goal.goalId)
                            }
                        }
                    },
                    {
                        viewModel.refreshDashboardData(userId)
                    }
                )
            },
            update = { uiView: UIView ->
                IosDashboardRegistry.updater?.invoke(uiView, state)
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        InvestmentDashboardV2Screen(
            userId = userId,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToHelp = onNavigateToHelp,
            onNavigateToGoal = onNavigateToGoal,
            onNavigateToSchemeDetails = onNavigateToSchemeDetails,
            onNavigateToWithdraw = onNavigateToWithdraw,
            onNavigateToReferral = onNavigateToReferral,
            onStartKyc = onStartKyc,
            onRetryKyc = onRetryKyc
        )
    }
}
