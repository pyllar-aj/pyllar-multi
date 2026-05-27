package com.pyllar.consumer.platform

import platform.UIKit.UIDevice
import platform.Foundation.NSBundle
import com.pyllar.consumer.push.PushTokenManager

/**
 * iOS implementations of shared platform service interfaces.
 *
 * These are intentionally minimal and can be expanded with
 * real implementations as the iOS host app evolves.
 */

class IosDeviceInfoProvider : DeviceInfoProvider {
    override fun getDeviceId(): String? {
        return UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown-ios-device"
    }

    override fun getOsName(): String = UIDevice.currentDevice.systemName

    override fun getOsVersion(): String = UIDevice.currentDevice.systemVersion

    override fun getAppVersion(): String? {
        return NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "1.0.0"
    }
}

class IosPushTokenProvider : PushTokenProvider {
    override suspend fun getPushToken(): String? = PushTokenManager.getPushToken()
}

class IosAnalyticsTracker : AnalyticsTracker {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        try {
            com.pyllar.consumer.analytics.SwiftAnalyticsScope.bridge?.logEvent(name, params)
        } catch (_: Throwable) {}
    }

    override fun logScreenView(screenName: String) {
        try {
            com.pyllar.consumer.analytics.SwiftAnalyticsScope.bridge?.logScreenView(screenName)
        } catch (_: Throwable) {}
    }
}

class IosUpdateManager : UpdateManager {
    override fun scheduleUpdateCheck() {
        // No-op stub; wire into iOS update mechanism when available
    }
}

