package com.pyllar.consumer.presentation.mutualfund.onboarding

import android.accounts.AccountManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@Composable
actual fun GoogleAccountPickerButton(
    onEmailPicked: (String) -> Unit,
    modifier: Modifier
) {
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                onEmailPicked(accountName)
            }
        }
    }

    Box(
        modifier = modifier
            .clickable {
                val intent = AccountManager.newChooseAccountIntent(
                    null, null, arrayOf("com.google"), null, null, null, null
                )
                accountPickerLauncher.launch(intent)
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
