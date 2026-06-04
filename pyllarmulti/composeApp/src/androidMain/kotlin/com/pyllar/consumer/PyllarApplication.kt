package com.pyllar.consumer

import android.app.Application
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.microsoft.clarity.models.LogLevel
import com.pyllar.consumer.analytics.AppsFlyerAttributionCache
import com.pyllar.consumer.analytics.AppsFlyerTracker
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.analytics.SingularTracker
import com.pyllar.consumer.di.androidPlatformModule
import com.pyllar.consumer.di.sharedModule
import com.pyllar.consumer.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PyllarApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Seed the application context for the static AnalyticsLogger
        PlatformAnalyticsLogger.applicationContext = this

        // Initialize Singular SDK
        try {
            SingularTracker.init(this)
            Log.d("PyllarApplication", "✅ Singular SDK initialized successfully")
        } catch (e: Exception) {
            Log.e("PyllarApplication", "⚠️ Singular SDK initialization failed", e)
        }

        // 2. Initialize Microsoft Clarity
        try {
            val config = ClarityConfig(
                projectId = "vkt8sc281d",
                logLevel = LogLevel.Verbose
            )
            Clarity.initialize(applicationContext, config)
            Log.d("PyllarApplication", "✅ Microsoft Clarity initialized successfully")
        } catch (e: Exception) {
            Log.e("PyllarApplication", "⚠️ Microsoft Clarity initialization failed", e)
        }

        // 3. Initialize AppsFlyer SDK
        try {
            AppsFlyerTracker.init(this) { attributionData ->
                AppsFlyerAttributionCache.store(attributionData)
            }
            Log.d("PyllarApplication", "✅ AppsFlyer initialized successfully")
        } catch (e: Exception) {
            Log.e("PyllarApplication", "⚠️ AppsFlyer initialization failed", e)
        }

        // 4. Initialize Koin Dependency Injection
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
        // AppsFlyer is initialised via AppsFlyerTracker.init() in onCreate().
    }

    private fun initClarity() {
        try {
            val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: BuildConfig.VERSION_NAME
            Clarity.initialize(applicationContext, ClarityConfig(projectId = "vkt8sc281d"))
            Clarity.setCustomTag("app_version", appVersion)
        } catch (_: Throwable) {}
    }
}
