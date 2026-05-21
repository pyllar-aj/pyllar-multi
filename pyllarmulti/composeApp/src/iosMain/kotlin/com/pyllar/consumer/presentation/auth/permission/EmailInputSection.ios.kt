package com.pyllar.consumer.presentation.auth.permission

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                Text(
                    text = stringResource(Res.string.pick_google_account),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
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
    if (showError) {
        Text(
            text = stringResource(Res.string.please_select_email),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

