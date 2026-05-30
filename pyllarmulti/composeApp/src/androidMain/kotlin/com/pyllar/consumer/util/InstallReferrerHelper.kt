package com.pyllar.consumer.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.pyllar.consumer.data.local.AndroidLocalOnboardingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object InstallReferrerHelper {

    private const val TAG = "InstallReferrerHelper"
    private const val KEY_REFERRER_CAPTURED = "utm_referrer_captured"
    const val KEY_UTM_SOURCE = "utm_source"
    const val KEY_UTM_MEDIUM = "utm_medium"
    const val KEY_UTM_CAMPAIGN = "utm_campaign"
    const val KEY_UTM_TERM = "utm_term"
    const val KEY_UTM_CONTENT = "utm_content"
    const val KEY_UTM_CAMPAIGN_ID = "utm_campaign_id"
    const val KEY_GCLID = "gclid"
    const val KEY_GBRAID = "gbraid"
    const val KEY_WBRAID = "wbraid"
    const val KEY_REFERRAL_CODE = "referral_code"

    fun captureIfNeeded(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val store = AndroidLocalOnboardingStore.getInstance(context)
            val alreadyCaptured = store.getValue(KEY_REFERRER_CAPTURED)
            if (alreadyCaptured == "true") {
                Log.d(TAG, "Install referrer already captured, skipping")
                return@launch
            }

            try {
                connectAndCapture(context, store)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture install referrer: ${e.message}")
            }
        }
    }

    private fun connectAndCapture(context: Context, store: AndroidLocalOnboardingStore) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()

        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            val referrerDetails = referrerClient.installReferrer
                            val referrerUrl = referrerDetails.installReferrer
                            Log.d(TAG, "Install referrer URL: $referrerUrl")

                            if (!referrerUrl.isNullOrBlank()) {
                                val params = parseUtmParams(referrerUrl)
                                CoroutineScope(Dispatchers.IO).launch {
                                    saveUtmParams(store, params)
                                }
                            } else {
                                CoroutineScope(Dispatchers.IO).launch {
                                    store.saveValue(KEY_REFERRER_CAPTURED, "true")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading referrer details: ${e.message}")
                        } finally {
                            referrerClient.endConnection()
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Log.d(TAG, "Install referrer not supported")
                        referrerClient.endConnection()
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        Log.d(TAG, "Install referrer service unavailable")
                        referrerClient.endConnection()
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                Log.d(TAG, "Install referrer service disconnected")
            }
        })
    }

    private fun parseUtmParams(referrerUrl: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        try {
            val uri = Uri.parse("https://placeholder.com?$referrerUrl")
            uri.getQueryParameter("utm_source")?.let { params[KEY_UTM_SOURCE] = it }
            uri.getQueryParameter("utm_medium")?.let { params[KEY_UTM_MEDIUM] = it }
            uri.getQueryParameter("utm_campaign")?.let { params[KEY_UTM_CAMPAIGN] = it }
            uri.getQueryParameter("utm_term")?.let { params[KEY_UTM_TERM] = it }
            uri.getQueryParameter("utm_content")?.let { params[KEY_UTM_CONTENT] = it }
            uri.getQueryParameter("pcampaignid")?.let { params[KEY_UTM_CAMPAIGN_ID] = it }
            uri.getQueryParameter("gclid")?.let { params[KEY_GCLID] = it }
            uri.getQueryParameter("gbraid")?.let { params[KEY_GBRAID] = it }
            uri.getQueryParameter("wbraid")?.let { params[KEY_WBRAID] = it }
            uri.getQueryParameter("referral_code")?.let { params[KEY_REFERRAL_CODE] = it }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing UTM params: ${e.message}")
        }
        return params
    }

    private suspend fun saveUtmParams(store: AndroidLocalOnboardingStore, params: Map<String, String>) {
        params.forEach { (key, value) ->
            store.saveValue(key, value)
            Log.d(TAG, "Saved UTM param: $key = $value")
        }
        store.saveValue(KEY_REFERRER_CAPTURED, "true")
        Log.d(TAG, "UTM params saved successfully")
    }
}
