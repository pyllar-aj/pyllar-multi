package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A platform-specific wrapper that makes its content clickable and triggers
 * the Google account/email chooser.
 */
@Composable
expect fun EmailChooserWrapper(
    onEmailPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
