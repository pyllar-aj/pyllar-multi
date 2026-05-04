package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Specialized WebView for eSign flow with Digio/Amazon AWS callbacks.
 */
@Composable
expect fun EsignPlatformWebView(
    url: String,
    modifier: Modifier = Modifier,
    onEsignComplete: () -> Unit,
    onEsignCancel: () -> Unit
)
