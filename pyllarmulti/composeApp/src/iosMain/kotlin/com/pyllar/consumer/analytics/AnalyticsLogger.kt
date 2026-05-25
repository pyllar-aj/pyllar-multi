package com.pyllar.consumer.analytics

/**
 * iOS actual for [PlatformAnalyticsLogger] that delegates to SwiftAnalyticsScope.bridge.
 */
actual object PlatformAnalyticsLogger {
    actual fun logEvent(name: String, params: Map<String, Any?>) {
        SwiftAnalyticsScope.bridge?.logEvent(name, params)
    }

    actual fun logScreenView(screenName: String) {
        SwiftAnalyticsScope.bridge?.logScreenView(screenName)
    }

    actual fun setUserId(userId: String) {
        SwiftAnalyticsScope.bridge?.setUserId(userId)
    }
}
