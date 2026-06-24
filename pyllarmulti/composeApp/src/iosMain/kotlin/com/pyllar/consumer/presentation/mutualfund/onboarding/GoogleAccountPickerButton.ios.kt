package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pyllar.composeapp.generated.resources.Res
import pyllar.composeapp.generated.resources.ic_google
import com.pyllar.consumer.presentation.auth.permission.SwiftGoogleSignInScope

@Composable
actual fun GoogleAccountPickerButton(
    onEmailPicked: (String) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clickable {
                SwiftGoogleSignInScope.bridge?.pickEmail { selectedEmail ->
                    if (!selectedEmail.isNullOrBlank()) {
                        onEmailPicked(selectedEmail)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_google),
            contentDescription = "Pick Google account",
            tint = Color(0xFF8B6B25), // V2GoldDeep
            modifier = Modifier.size(18.dp)
        )
    }
}
