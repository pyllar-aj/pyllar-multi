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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*
import com.pyllar.consumer.config.IS_DEBUG

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

    if (IS_DEBUG) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            placeholder = { Text("Enter your email or select one") },
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
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { triggerPicker() }
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { /* read-only */ },
                label = { Text(stringResource(Res.string.select_your_email)) },
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
                readOnly = true,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = if (showError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true,
                isError = showError,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showError) {
        Text(
            text = stringResource(Res.string.please_select_email),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

