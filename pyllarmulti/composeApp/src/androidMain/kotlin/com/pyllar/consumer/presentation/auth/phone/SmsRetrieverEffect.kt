package com.pyllar.consumer.presentation.auth.phone

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.pyllar.consumer.util.platformLog

@Composable
actual fun SmsRetrieverEffect(
    phoneNumber: String,
    onOtpReceived: (String) -> Unit
) {
    val context = LocalContext.current
    val currentOnOtpReceived by rememberUpdatedState(onOtpReceived)

    val smsConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val smsMessage = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            if (smsMessage != null) {
                platformLog("SmsRetrieverEffect: SMS received: $smsMessage")
                val patterns = listOf(
                    Regex("""(\d{6})\s+is\s+your\s+login\s+OTP""", RegexOption.IGNORE_CASE),
                    Regex("""(\d{6})\s+is\s+your""", RegexOption.IGNORE_CASE),
                    Regex("""OTP[\s:]+(\d{6})""", RegexOption.IGNORE_CASE),
                    Regex("""code[\s:]+(\d{6})""", RegexOption.IGNORE_CASE),
                    Regex("""\b(\d{6})\b"""),
                    Regex("""\b(\d{4})\b""")
                )
                val otp = patterns.firstNotNullOfOrNull { it.find(smsMessage)?.groupValues?.getOrNull(1) }
                if (otp != null) {
                    platformLog("SmsRetrieverEffect: Extracted OTP: $otp")
                    currentOnOtpReceived(otp)
                } else {
                    platformLog("SmsRetrieverEffect: Could not extract OTP from message")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        var smsRetrieverRestarts = 0

        fun startSmsRetriever() {
            SmsRetrieverReceiver.setCallbacks(
                onReceived = { consentIntent ->
                    smsConsentLauncher.launch(consentIntent)
                },
                onTimeout = {
                    if (smsRetrieverRestarts < 2) {
                        smsRetrieverRestarts++
                        startSmsRetriever()
                    }
                }
            )
            SmsRetriever.getClient(context).startSmsUserConsent(null)
                .addOnSuccessListener { platformLog("SmsRetrieverEffect: ✅ SMS User Consent started") }
                .addOnFailureListener { platformLog("SmsRetrieverEffect: ⚠️ SMS User Consent failed to start: ${it.message}") }
        }

        startSmsRetriever()

        onDispose {
            SmsRetrieverReceiver.clearCallbacks()
        }
    }
}
