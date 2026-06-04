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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pyllar.composeapp.generated.resources.*

@Composable
actual fun EmailInputSection(
    email: String,
    onEmailChange: (String) -> Unit,
    showError: Boolean
) {
    // Silently restore previously selected Google account so the user doesn't
    // have to go through the picker again on every visit.
    LaunchedEffect(Unit) {
        if (email.isBlank()) {
            SwiftGoogleSignInScope.bridge?.tryRestoreEmail { restoredEmail ->
                if (!restoredEmail.isNullOrBlank()) {
                    onEmailChange(restoredEmail)
                }
            }
        }
    }

    val triggerPicker = {
        SwiftGoogleSignInScope.bridge?.pickEmail { selectedEmail ->
            if (!selectedEmail.isNullOrBlank()) {
                onEmailChange(selectedEmail)
            }
        }
    }

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(Res.string.select_your_email)) },
        placeholder = { Text("Choose or enter your email") },
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
}


