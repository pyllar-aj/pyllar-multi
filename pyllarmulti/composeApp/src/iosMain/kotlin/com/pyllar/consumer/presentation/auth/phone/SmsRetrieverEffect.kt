package com.pyllar.consumer.presentation.auth.phone

import androidx.compose.runtime.Composable

@Composable
actual fun SmsRetrieverEffect(
    phoneNumber: String,
    onOtpReceived: (String) -> Unit
) {
    // No-op on iOS. iOS keyboard automatically parses OTP SMS when keyboard type is Number/Phone
    // and textContentType is OneTimeCode.
}
