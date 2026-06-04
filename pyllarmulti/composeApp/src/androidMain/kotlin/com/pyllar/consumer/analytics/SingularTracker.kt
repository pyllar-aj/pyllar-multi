package com.pyllar.consumer.analytics

import android.content.Context
import com.pyllar.consumer.BuildConfig
import com.pyllar.consumer.util.Log
import com.singular.sdk.Singular
import com.singular.sdk.SingularConfig
import org.json.JSONObject

object SingularTracker {

    private const val TAG = "SingularTracker"

    fun init(context: Context) {
        try {
            val config = SingularConfig(BuildConfig.SINGULAR_API_KEY, BuildConfig.SINGULAR_SECRET_KEY)
            Singular.init(context, config)
            Log.d(TAG, "Singular initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Singular initialization failed", e)
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
