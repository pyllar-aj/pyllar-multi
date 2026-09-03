package com.pyllar.otp

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.pyllar.consumer.presentation.ui.theme.V2Obsidian
import com.pyllar.consumer.presentation.ui.theme.V2Ink

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
actual fun OtpField(
    length: Int,
    modifier: Modifier,
    enabled: Boolean,
    isError: Boolean,
    otpFieldValue: TextFieldValue,
    onOtpFieldValueChange: (TextFieldValue) -> Unit,
    onOtpComplete: () -> Unit,
    autoFocus: Boolean,
) {
    OutlinedTextField(
        value = otpFieldValue,
        onValueChange = { newValue ->
            val newText = newValue.text.take(length)
            // Adjust selection if text changed externally
            val selection = if (newText.length < newValue.text.length) {
                TextRange(newText.length)
            } else {
                newValue.selection
            }
            
            onOtpFieldValueChange(newValue.copy(text = newText, selection = selection))
            if (newText.length == length) onOtpComplete()
        },
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        textStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            letterSpacing = 8.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = VisualTransformation.None,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = V2Obsidian,
            unfocusedBorderColor = Color(0xFF9E9E9E), // Darker grey
            errorBorderColor = Color(0xFFB00020),
            cursorColor = V2Obsidian,
            unfocusedContainerColor = Color(0xFFF5F5F5), // Subtle background
            focusedContainerColor = Color.White,
            focusedTextColor = V2Obsidian,
            unfocusedTextColor = V2Obsidian
        ),
    )
}
