package com.pyllar.consumer.analytics

/**
 * iOS no-op actual for [PlatformAnalyticsLogger].
 *
 * Replace with real implementations (Firebase iOS SDK,
 * Amplitude, etc.) when iOS analytics are required.
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
