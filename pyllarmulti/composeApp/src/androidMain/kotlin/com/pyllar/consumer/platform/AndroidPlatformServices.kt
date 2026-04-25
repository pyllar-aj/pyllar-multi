package com.pyllar.consumer.platform

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.push.TokenStore

class AndroidDeviceInfoProvider(
    private val context: Context
) : DeviceInfoProvider {

    override fun getDeviceId(): String? =
        try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (_: Throwable) {
            null
        }

    override fun getOsName(): String = "Android"

    override fun getOsVersion(): String =
        Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()

    override fun getAppVersion(): String? =
        try {
            val pm = context.packageManager
            val packageInfo = pm.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (_: Throwable) {
            null
        }
}

class AndroidPushTokenProvider(
    private val context: Context
) : PushTokenProvider {

    override suspend fun getPushToken(): String? =
        try {
            TokenStore.getToken(context)
        } catch (_: Throwable) {
            null
        }
}

class AndroidAnalyticsTracker(
    private val context: Context
) : AnalyticsTracker {

    override fun logEvent(name: String, params: Map<String, Any?>) {
        PlatformAnalyticsLogger.logEvent(name, params)
    }

    override fun logScreenView(screenName: String) {
        PlatformAnalyticsLogger.logScreenView(screenName)
    }
}

// AndroidUpdateManager is defined in update/AndroidUpdateManager.kt

