package com.pyllar.consumer

import android.app.Application
import com.appsflyer.AppsFlyerLib
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.di.androidPlatformModule
import com.pyllar.consumer.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PyllarApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PyllarApplication)
            modules(
                sharedModule,
                androidPlatformModule(this@PyllarApplication)
            )
        }

        initAppsFlyer()
        initClarity()
    }

    private fun initAppsFlyer() {
        try {
            AppsFlyerLib.getInstance().apply {
                setDebugLog(BuildConfig.DEBUG)
                init(BuildConfig.APPSFLYER_DEV_KEY, null, this@PyllarApplication)
                start(this@PyllarApplication)
            }
            PlatformAnalyticsLogger.init(this)
        } catch (_: Throwable) {}
    }

    private fun initClarity() {
        try {
            val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: BuildConfig.VERSION_NAME
            Clarity.initialize(applicationContext, ClarityConfig(projectId = "vkt8sc281d"))
            Clarity.setCustomTag("app_version", appVersion)
        } catch (_: Throwable) {}
    }
}
