package com.pyllar.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun OtpField(
    length: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    otpText: String,
    allowNonSequentialFocus: Boolean = true,
    onOtpChange: (String) -> Unit,
    onOtpComplete: () -> Unit,
) {
    // Minimal implementation: one text field. Keeps call sites intact while unblocking builds.
    OutlinedTextField(
        value = otpText,
        onValueChange = { newValue ->
            val trimmed = newValue.take(length)
            onOtpChange(trimmed)
            if (trimmed.length == length) onOtpComplete()
        },
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = VisualTransformation.None,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF0A5C36),
            unfocusedBorderColor = Color(0xFF9E9E9E), // Darker grey
            errorBorderColor = Color(0xFFB00020),
            cursorColor = Color(0xFF0A5C36),
            unfocusedContainerColor = Color(0xFFF5F5F5), // Subtle background
            focusedContainerColor = Color.White,
        ),
    )
}

