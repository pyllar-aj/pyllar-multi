package com.pyllar.consumer.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.microsoft.clarity.Clarity
import com.pyllar.consumer.util.Log

actual object PlatformAnalyticsLogger {

    @Volatile
    var applicationContext: Context? = null

    actual fun logEvent(name: String, params: Map<String, Any?>) {
        val context = applicationContext ?: return
        try {
            // 1. Firebase Analytics
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle()
            params.forEach { (key, value) ->
                when (value) {
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Float -> bundle.putFloat(key, value)
                    is Boolean -> bundle.putString(key, value.toString())
                    else -> if (value != null) bundle.putString(key, value.toString())
                }
            }
            analytics.logEvent(name, bundle)

            // 2. AppsFlyer
            AppsFlyerTracker.logEvent(context, name, params)

            Log.d("PlatformAnalyticsLogger", "✅ Logged event: $name params=$params")
        } catch (e: Throwable) {
            Log.e("PlatformAnalyticsLogger", "❌ Failed to log event: $name", e)
        }
    }

    actual fun logScreenView(screenName: String) {
        val context = applicationContext ?: return
        try {
            // 1. Firebase Analytics Screen View
            val analytics = FirebaseAnalytics.getInstance(context)
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            }
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)

            // 2. AppsFlyer Screen View (mirroring iOS bridge behavior)
            AppsFlyerTracker.logEvent(context, "screen_view", mapOf("screen_name" to screenName))

            Log.d("PlatformAnalyticsLogger", "Logged screen_view: $screenName")
        } catch (_: Throwable) {}
    }

    actual fun setUserId(userId: String) {
        val context = applicationContext ?: return
        try {
            // 1. Firebase Analytics User ID
            FirebaseAnalytics.getInstance(context).setUserId(userId)

            // 2. Clarity User ID
            Clarity.setCustomUserId(userId)

            // 3. AppsFlyer Customer User ID
            AppsFlyerTracker.setUserId(userId)

            Log.d("PlatformAnalyticsLogger", "User ID set across platforms: $userId")
        } catch (_: Throwable) {}
    }
}
