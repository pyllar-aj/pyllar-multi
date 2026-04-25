package com.pyllar.consumer.analytics

/**
 * Shared analytics logger interface for commonMain.
 *
 * The [com.pyllar.consumer.platform.AnalyticsTracker] interface (in PlatformServices.kt)
 * already provides the Koin-injectable abstraction used by shared ViewModels.
 *
 * This object exists as a thin, static-access façade for call sites that
 * cannot inject via Koin (e.g. nav interceptors, utility code).
 *
 * Platform implementations delegate to their own SDKs:
 *  - Android: Firebase Analytics + Microsoft Clarity (androidMain/AnalyticsLogger.kt)
 *  - iOS: no-op until an iOS analytics SDK is wired
 */
expect object PlatformAnalyticsLogger {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
    fun logScreenView(screenName: String)
    fun setUserId(userId: String)
}
