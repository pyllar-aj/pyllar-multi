package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun GoogleAccountPickerButton(
    onEmailPicked: (String) -> Unit,
    modifier: Modifier = Modifier
)
