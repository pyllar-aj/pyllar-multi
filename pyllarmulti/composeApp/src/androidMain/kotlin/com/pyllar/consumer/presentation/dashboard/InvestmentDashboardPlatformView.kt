package com.pyllar.consumer.presentation.dashboard

import androidx.compose.runtime.Composable

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
