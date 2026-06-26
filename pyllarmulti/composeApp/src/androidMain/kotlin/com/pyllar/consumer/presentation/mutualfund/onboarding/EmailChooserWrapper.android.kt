package com.pyllar.consumer.presentation.mutualfund.onboarding

import android.accounts.AccountManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun EmailChooserWrapper(
    onEmailPicked: (String) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
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
            }
    ) {
        content()
    }
}
