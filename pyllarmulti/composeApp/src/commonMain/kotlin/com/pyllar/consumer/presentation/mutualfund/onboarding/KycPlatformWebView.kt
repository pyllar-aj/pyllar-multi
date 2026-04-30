package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Expect declaration for a specialized KYC WebView.
 * 
 * It takes a URL and a callback [onStatusDetected] that is triggered
 * when the redirect URL (pyllar.in?status=...) is encountered.
 */
@Composable
expect fun KycPlatformWebView(
    url: String,
    modifier: Modifier = Modifier,
    onStatusDetected: (String) -> Unit
)
