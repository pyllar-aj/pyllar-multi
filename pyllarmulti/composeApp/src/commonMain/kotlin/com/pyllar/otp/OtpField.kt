package com.pyllar.otp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue

@Composable
expect fun OtpField(
    length: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    otpFieldValue: TextFieldValue,
    onOtpFieldValueChange: (TextFieldValue) -> Unit,
    onOtpComplete: () -> Unit,
)
