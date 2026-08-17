package com.pyllar.consumer.presentation.dashboard

import androidx.compose.runtime.Composable

@Composable
expect fun InvestmentDashboardPlatformView(
    userId: String,
    onNavigateToProfile: () -> Void,
    onNavigateToHelp: () -> Void,
    onNavigateToGoal: (String) -> Void,
    onNavigateToSchemeDetails: (String) -> Void,
    onNavigateToWithdraw: () -> Void,
    onNavigateToReferral: () -> Void,
    onStartKyc: () -> Void,
    onRetryKyc: () -> Void
)

// Helper types matching KMP function parameter mappings
typealias Void = Unit
