package com.pyllar.consumer.presentation.auth.permission

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pyllar.consumer.config.IS_DEBUG
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*

@Composable
actual fun EmailInputSection(
    email: String,
    onEmailChange: (String) -> Unit,
    showError: Boolean
) {
    val triggerPicker = {
        SwiftGoogleSignInScope.bridge?.pickEmail { selectedEmail ->
            if (!selectedEmail.isNullOrBlank()) {
                onEmailChange(selectedEmail)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            readOnly = !IS_DEBUG,
            label = { Text(stringResource(Res.string.select_your_email)) },
            placeholder = { Text(if (IS_DEBUG) "Enter your email or select one" else "Select your email") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Pick account",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { triggerPicker() }
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = showError,
            modifier = Modifier.fillMaxWidth()
        )
        if (!IS_DEBUG) {
            // Overlay to detect click on the entire text field area and trigger the picker
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { triggerPicker() }
            )
        }
    }
}


