package com.pyllar.consumer.presentation.auth.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pyllar.consumer.util.platformLog
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import java.util.concurrent.atomic.AtomicReference

class SmsRetrieverReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsRetrieverReceiver"

        private val _onConsentReceived = AtomicReference<((Intent) -> Unit)?>(null)
        private val _onOtpTimeout      = AtomicReference<(() -> Unit)?>(null)

        fun setCallbacks(onReceived: (Intent) -> Unit, onTimeout: () -> Unit) {
            _onConsentReceived.set(onReceived)
            _onOtpTimeout.set(onTimeout)
        }

        fun clearCallbacks() {
            _onConsentReceived.set(null)
            _onOtpTimeout.set(null)
        }

        internal fun invokeReceived(consentIntent: Intent) = _onConsentReceived.get()?.invoke(consentIntent)
        internal fun invokeTimeout() = _onOtpTimeout.get()?.invoke()
    }

    override fun onReceive(context: Context, intent: Intent) {
        platformLog("$TAG: 📨 onReceive: ${intent.action}")

        if (SmsRetriever.SMS_RETRIEVED_ACTION != intent.action) return

        val extras = intent.extras
        val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status
        platformLog("$TAG: 📊 SMS User Consent status: ${status?.statusCode}")

        when (status?.statusCode) {
            CommonStatusCodes.SUCCESS -> {
                platformLog("$TAG: ✅ SMS User Consent — consent intent received")
                @Suppress("DEPRECATION")
                val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                if (consentIntent != null) {
                    invokeReceived(consentIntent)
                } else {
                    platformLog("$TAG: ⚠️ EXTRA_CONSENT_INTENT is null")
                }
            }
            CommonStatusCodes.TIMEOUT -> {
                platformLog("$TAG: ⏱️ SMS User Consent TIMEOUT — 5-minute window expired")
                invokeTimeout()
            }
            else -> {
                platformLog("$TAG: SMS User Consent failed: ${status?.statusCode}")
            }
        }
    }
}
