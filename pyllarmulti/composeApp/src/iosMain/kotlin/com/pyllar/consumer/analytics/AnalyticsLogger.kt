package com.pyllar.consumer.analytics

/**
 * iOS no-op actual for [PlatformAnalyticsLogger].
 *
 * Replace with real implementations (Firebase iOS SDK,
 * Amplitude, etc.) when iOS analytics are required.
 */
actual object PlatformAnalyticsLogger {
    actual fun logEvent(name: String, params: Map<String, Any?>) {
        // No-op on iOS — wire to Firebase iOS SDK when ready
    }

    actual fun logScreenView(screenName: String) {
        // No-op on iOS
    }

    actual fun setUserId(userId: String) {
        // No-op on iOS
    }
}
