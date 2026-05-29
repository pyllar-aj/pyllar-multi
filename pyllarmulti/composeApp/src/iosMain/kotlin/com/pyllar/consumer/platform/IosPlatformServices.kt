package com.pyllar.consumer.platform

/**
 * iOS implementations of shared platform service interfaces.
 *
 * These are intentionally minimal and can be expanded with
 * real implementations as the iOS host app evolves.
 */

import platform.UIKit.UIDevice
import platform.Foundation.NSBundle
import com.pyllar.consumer.analytics.PlatformAnalyticsLogger
import com.pyllar.consumer.analytics.SwiftAnalyticsScope

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
    override suspend fun getPushToken(): String? = null
}

class IosAnalyticsTracker : AnalyticsTracker {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        PlatformAnalyticsLogger.logEvent(name, params)
    }

    override fun logScreenView(screenName: String) {
        PlatformAnalyticsLogger.logScreenView(screenName)
    }
}

class IosUpdateManager : UpdateManager {
    override fun scheduleUpdateCheck() {
        // No-op stub; wire into iOS update mechanism when available
    }
}

class IosAttributionProvider : AttributionProvider {
    private fun getAttrValue(key: String): String? {
        val map = SwiftAnalyticsScope.bridge?.getAttributionData() ?: return null
        return map[key]
    }

    override fun getReferralCode(): String? = getAttrValue("deep_link_sub1") ?: getAttrValue("af_sub1")
    override fun getMediaSource(): String? = getAttrValue("media_source")
    override fun getCampaign(): String? = getAttrValue("campaign")
    override fun getCampaignId(): String? = getAttrValue("campaign_id")
    override fun getAdSet(): String? = getAttrValue("adset")
    override fun getAfStatus(): String? = getAttrValue("af_status")
    override fun getChannel(): String? = getAttrValue("channel")
    override fun getGclid(): String? = getAttrValue("gclid")
    override fun getGbraid(): String? = getAttrValue("gbraid")
    override fun getWbraid(): String? = getAttrValue("wbraid")
}

