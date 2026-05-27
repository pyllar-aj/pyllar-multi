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
        // No-op for now on iOS
    }

    override fun logScreenView(screenName: String) {
        // No-op for now on iOS
    }
}

class IosUpdateManager : UpdateManager {
    override fun scheduleUpdateCheck() {
        // No-op stub; wire into iOS update mechanism when available
    }
}

