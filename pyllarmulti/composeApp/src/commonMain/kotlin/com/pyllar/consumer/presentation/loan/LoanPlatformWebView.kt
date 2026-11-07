package com.pyllar.consumer.presentation.loan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun LoanPlatformWebView(
    url: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
)

