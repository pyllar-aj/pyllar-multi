package com.pyllar.consumer.presentation.auth.phone

import androidx.compose.runtime.Composable

@Composable
expect fun SmsRetrieverEffect(
    phoneNumber: String,
    onOtpReceived: (String) -> Unit
)
