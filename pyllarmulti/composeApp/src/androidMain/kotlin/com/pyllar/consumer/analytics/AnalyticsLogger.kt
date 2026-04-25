package com.pyllar.consumer.analytics

/**
 * Android actual implementation of [PlatformAnalyticsLogger].
 *
 * TODO: Add Firebase Analytics and Microsoft Clarity dependencies to androidMain:
 *   implementation(platform("com.google.firebase:firebase-bom:33.x.x"))
 *   implementation("com.google.firebase:firebase-analytics")
 *   implementation("com.microsoft.clarity:clarity:2.x.x")
 *
 * Then replace this stub with the full implementation from:
 *   Pyllar/android/app/src/main/…/analytics/AnalyticsLogger.kt
 *
 * Until those dependencies are added, all methods are no-ops.
 */
actual object PlatformAnalyticsLogger {
    actual fun logEvent(name: String, params: Map<String, Any?>) {
        // TODO: wire to FirebaseAnalytics once dependency is added
    }

    actual fun logScreenView(screenName: String) {
        // TODO: wire to FirebaseAnalytics.Event.SCREEN_VIEW
    }

    actual fun setUserId(userId: String) {
        // TODO: wire to FirebaseAnalytics.setUserId + Clarity.setCustomUserId
    }
}
