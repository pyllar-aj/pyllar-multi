package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyllar.consumer.presentation.auth.permission.SwiftGoogleSignInScope

@Composable
actual fun EmailChooserWrapper(
    onEmailPicked: (String) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clickable {
                SwiftGoogleSignInScope.bridge?.pickEmail { selectedEmail ->
                    if (!selectedEmail.isNullOrBlank()) {
                        onEmailPicked(selectedEmail)
                    }
                }
            }
    ) {
        content()
    }
}
