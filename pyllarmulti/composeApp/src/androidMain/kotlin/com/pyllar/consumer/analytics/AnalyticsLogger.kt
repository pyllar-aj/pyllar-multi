package com.pyllar.consumer.analytics

import android.content.Context
import com.appsflyer.AppsFlyerLib

actual object PlatformAnalyticsLogger {

    private var appContext: Context? = null

    /** Called once from PyllarApplication after AppsFlyer is started. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun logEvent(name: String, params: Map<String, Any?>) {
        val ctx = appContext ?: return
        try {
            val afParams = HashMap<String, Any>()
            params.forEach { (k, v) -> if (v != null) afParams[k] = v }
            AppsFlyerLib.getInstance().logEvent(ctx, name, afParams)
        } catch (_: Throwable) {}
    }

    actual fun logScreenView(screenName: String) {
        val ctx = appContext ?: return
        try {
            AppsFlyerLib.getInstance().logEvent(ctx, screenName, emptyMap<String, Any>())
        } catch (_: Throwable) {}
    }

    actual fun setUserId(userId: String) {
        try {
            AppsFlyerLib.getInstance().setCustomerUserId(userId)
        } catch (_: Throwable) {}
    }
}
