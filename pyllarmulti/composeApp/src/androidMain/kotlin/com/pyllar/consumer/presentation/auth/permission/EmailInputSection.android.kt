package com.pyllar.consumer.presentation.auth.permission

import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    // Pre-populate from first available Google account
    val initialEmail = remember {
        runCatching {
            AccountManager.get(context)
                .getAccountsByType("com.google")
                .firstOrNull()?.name ?: ""
        }.getOrDefault("")
    }
    LaunchedEffect(initialEmail) {
        if (email.isBlank() && initialEmail.isNotBlank()) onEmailChange(initialEmail)
    }

    // Account picker launcher
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) onEmailChange(accountName)
        }
    }

    val launchAccountPicker = {
        runCatching {
            accountPickerLauncher.launch(
                AccountManager.newChooseAccountIntent(
                    null, null, arrayOf("com.google"), false, null, null, null, null
                )
            )
        }
    }

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(Res.string.select_your_email)) },
        placeholder = { Text("Enter your email or select one") },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Pick account",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { launchAccountPicker() }
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        isError = showError,
        modifier = Modifier.fillMaxWidth()
    )
}

