package com.pyllar.consumer.analytics

import android.content.Context
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.CreateOneLinkHttpTask
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.appsflyer.share.LinkGenerator
import com.appsflyer.share.ShareInviteHelper
import com.pyllar.consumer.util.Log

object AppsFlyerTracker {

    private const val TAG = "AppsFlyerTracker"
    private const val APPSFLYER_DEV_KEY = "gog7ERykY2ivzocSRnpKPi"

    fun init(context: Context, onAttributionData: (Map<String, Any?>) -> Unit = {}) {
        try {
            val conversionListener = object : AppsFlyerConversionListener {
                override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                    Log.d(TAG, "Attribution data received: $data")
                    onAttributionData(data ?: emptyMap())
                }

                override fun onConversionDataFail(errorMessage: String?) {
                    Log.w(TAG, "Attribution data failed: $errorMessage")
                }

                override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
                    Log.d(TAG, "App opened via attribution: $data")
                }

                override fun onAttributionFailure(errorMessage: String?) {
                    Log.w(TAG, "Attribution failure: $errorMessage")
                }
            }

            AppsFlyerLib.getInstance().apply {
                setDebugLog(true)
                setAppInviteOneLink("JV5P")
                init(APPSFLYER_DEV_KEY, conversionListener, context)
                start(context)
            }

            AppsFlyerLib.getInstance().subscribeForDeepLink(object : DeepLinkListener {
                override fun onDeepLinking(result: DeepLinkResult) {
                    when (result.status) {
                        DeepLinkResult.Status.FOUND ->
                            Log.d(TAG, "Deep link: ${result.deepLink?.deepLinkValue}")
                        DeepLinkResult.Status.NOT_FOUND ->
                            Log.d(TAG, "Deep link not found")
                        else ->
                            Log.w(TAG, "Deep link error: ${result.error}")
                    }
                }
            })

            Log.d(TAG, "AppsFlyer initialized")
        } catch (e: Exception) {
            Log.e(TAG, "AppsFlyer initialization failed", e)
        }
    }

    fun setUserId(userId: String) {
        try {
            AppsFlyerLib.getInstance().setCustomerUserId(userId)
        } catch (_: Throwable) {}
    }

    fun logEvent(context: Context, eventName: String, params: Map<String, Any?> = emptyMap()) {
        try {
            val afParams = HashMap<String, Any>()
            params.forEach { (k, v) -> if (v != null) afParams[k] = v }
            AppsFlyerLib.getInstance().logEvent(context, eventName, afParams)
        } catch (_: Throwable) {}
    }

    fun logRevenueEvent(
        context: Context,
        eventName: String,
        amountInr: Double,
        extraParams: Map<String, Any?> = emptyMap()
    ) {
        try {
            val params = HashMap<String, Any>()
            params[AFInAppEventParameterName.REVENUE] = amountInr
            params[AFInAppEventParameterName.CURRENCY] = "INR"
            extraParams.forEach { (k, v) -> if (v != null) params[k] = v }
            AppsFlyerLib.getInstance().logEvent(context, eventName, params)
            Log.d(TAG, "AF revenue event: $eventName amount=₹$amountInr")
        } catch (_: Throwable) {}
    }

    fun generateReferralLink(context: Context, referrerId: String, onComplete: (String?) -> Unit) {
        try {
            val generator = ShareInviteHelper.generateInviteUrl(context)
            generator.addParameter("deep_link_value", "referral")
            generator.addParameter("deep_link_sub1", referrerId)
            generator.addParameter("af_sub1", referrerId)
            
            generator.generateLink(context, object : CreateOneLinkHttpTask.ResponseListener {
                override fun onResponse(url: String?) {
                    onComplete(url)
                }
                override fun onError(error: String?) {
                    Log.e(TAG, "Failed to generate OneLink: $error")
                    onComplete(null)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "OneLink generator crashed", e)
            onComplete(null)
        }
    }
}
