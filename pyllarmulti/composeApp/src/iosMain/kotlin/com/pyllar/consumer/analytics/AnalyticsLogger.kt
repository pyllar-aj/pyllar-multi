package com.pyllar.consumer.analytics

/**
 * iOS actual for [PlatformAnalyticsLogger] that delegates to SwiftAnalyticsScope.bridge.
 */
actual object PlatformAnalyticsLogger {
    actual fun logEvent(name: String, params: Map<String, Any?>) {
        try {
            SwiftAnalyticsScope.bridge?.logEvent(name, params)
        } catch (_: Throwable) {}
    }

    actual fun logScreenView(screenName: String) {
        try {
            SwiftAnalyticsScope.bridge?.logScreenView(screenName)
        } catch (_: Throwable) {}
    }

    actual fun setUserId(userId: String) {
        try {
            SwiftAnalyticsScope.bridge?.setUserId(userId)
        } catch (_: Throwable) {}
    }
}
