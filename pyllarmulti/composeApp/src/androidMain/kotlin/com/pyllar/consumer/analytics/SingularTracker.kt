package com.pyllar.consumer.analytics

import android.content.Context
import android.content.Intent
import com.pyllar.consumer.BuildConfig
import com.pyllar.consumer.util.Log
import com.singular.sdk.Singular
import com.singular.sdk.SingularConfig
import org.json.JSONObject

object SingularTracker {

    private const val TAG = "SingularTracker"

    /** Params from the most recently resolved Singular Link (deeplink/passthrough/is_deferred/url params). */
    var linkAttributionData: Map<String, String> = emptyMap()
        private set

    fun init(context: Context) {
        try {
            val config = SingularConfig(BuildConfig.SINGULAR_API_KEY, BuildConfig.SINGULAR_SECRET_KEY)
            Singular.init(context, config)
            Log.d(TAG, "Singular initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Singular initialization failed", e)
        }
    }

    /**
     * Re-inits Singular with the launching/new intent attached, so the SDK can resolve
     * a Singular Link (https://pyllar.sng.link/...) tap into deeplink/passthrough params.
     * Call from the launcher Activity's onCreate (cold start) and onNewIntent (warm start) —
     * per Singular's Android SDK docs, withSingularLink() must be chained onto the config
     * passed to Singular.init(), so cold/warm-start resolution both require an init call.
     */
    fun resolveLink(context: Context, intent: Intent) {
        try {
            val config = SingularConfig(BuildConfig.SINGULAR_API_KEY, BuildConfig.SINGULAR_SECRET_KEY)
                .withSingularLink(intent) { params ->
                    val dict = mutableMapOf<String, String>()
                    params.deeplink?.let { dict["deeplink"] = it }
                    params.passthrough?.let { dict["passthrough"] = it }
                    dict["is_deferred"] = params.isDeferred.toString()
                    params.urlParameters?.let { dict.putAll(it) }
                    linkAttributionData = dict
                    Log.d(TAG, "Singular link params received: $dict")
                }
            Singular.init(context, config)
        } catch (e: Exception) {
            Log.e(TAG, "Singular link resolution failed", e)
        }
    }

    fun setUserId(userId: String) {
        try {
            Singular.setCustomUserId(userId)
        } catch (_: Throwable) {}
    }

    fun logEvent(eventName: String, params: Map<String, Any?> = emptyMap()) {
        try {
            if (params.isEmpty()) {
                Singular.event(eventName)
            } else {
                val json = JSONObject()
                params.forEach { (k, v) -> if (v != null) json.put(k, v) }
                Singular.event(eventName, json)
            }
        } catch (_: Throwable) {}
    }

    fun logRevenueEvent(
        eventName: String,
        amountInr: Double,
        extraParams: Map<String, Any?> = emptyMap()
    ) {
        try {
            if (extraParams.isEmpty()) {
                Singular.customRevenue(eventName, "INR", amountInr)
            } else {
                val json = JSONObject()
                extraParams.forEach { (k, v) -> if (v != null) json.put(k, v) }
                Singular.customRevenue(eventName, "INR", amountInr, json)
            }
            Log.d(TAG, "Singular revenue event: $eventName amount=₹$amountInr")
        } catch (_: Throwable) {}
    }
}
