package com.pyllar.consumer.presentation.auth.permission

import androidx.compose.runtime.Composable

/**
 * Platform-specific email collection UI.
 *
 * - Android: read-only field pre-populated from AccountManager (Google account picker).
 * - iOS: editable OutlinedTextField where the user types their email.
 */
@Composable
expect fun EmailInputSection(
    email: String,
    onEmailChange: (String) -> Unit,
    showError: Boolean
)
