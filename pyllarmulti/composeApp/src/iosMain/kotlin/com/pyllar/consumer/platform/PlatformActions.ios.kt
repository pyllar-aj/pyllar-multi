package com.pyllar.consumer.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIWindow
import com.pyllar.consumer.util.platformLog

class IosPlatformActions : PlatformActions {

    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
        }
    }

    override fun openUpiUrl(url: String, packageName: String?) {
        // If it's a standard upi:// link and we have a specific app scheme (packageName),
        // we replace 'upi://' with the app's scheme (e.g., 'phonepe://') to force it to open.
        val targetUrl = if (url.startsWith("upi://") && !packageName.isNullOrBlank()) {
            if (packageName == "gpay://") {
                url.replace("upi://", "gpay://upi/")
            } else {
                url.replace("upi://", packageName)
            }
        } else {
            url
        }
        
        // URL Encoding is CRITICAL on iOS for mandate links with special characters
        val encodedUrl = targetUrl.replace(" ", "%20")
            .replace("|", "%7C")
            .replace("{", "%7B")
            .replace("}", "%7D")
            .replace("[", "%5B")
            .replace("]", "%5D")

        val nsUrl = NSURL.URLWithString(encodedUrl)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = { success ->
                platformLog("IosPlatformActions: Mandate launch result: $success")
            })
        } else {
            platformLog("IosPlatformActions: ❌ Invalid Mandate URL: $encodedUrl")
        }
    }

    override fun shareText(text: String, title: String) {
        val activityController = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )
        
        val rootViewController = getRootViewController()
        rootViewController?.presentViewController(activityController, animated = true, completion = null)
    }

    override fun openWhatsApp(phoneNumber: String, message: String) {
        if (phoneNumber.isBlank()) {
            val components = NSURLComponents.componentsWithString("whatsapp://send")
            components?.setQueryItems(listOf(NSURLQueryItem.queryItemWithName("text", message)))
            val url = components?.URL
            if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
                return
            }
            
            // Fallback to web URL
            val webComponents = NSURLComponents.componentsWithString("https://api.whatsapp.com/send")
            webComponents?.setQueryItems(listOf(NSURLQueryItem.queryItemWithName("text", message)))
            val webUrl = webComponents?.URL
            if (webUrl != null) {
                UIApplication.sharedApplication.openURL(webUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
            }
        } else {
            val baseUrl = "https://wa.me/$phoneNumber"
            val components = NSURLComponents.componentsWithString(baseUrl)
            components?.setQueryItems(listOf(NSURLQueryItem.queryItemWithName("text", message)))
            
            val url = components?.URL
            if (url != null) {
                UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
            }
        }
    }

    override fun getInstalledUpiApps(): List<UpiAppInfo> {
        val upiSchemes = mapOf(
            "phonepe://" to "PhonePe",
            "gpay://" to "Google Pay",
            "paytmmp://" to "Paytm",
            "bhim://" to "BHIM",
            "amazonpay://" to "Amazon Pay",
            "mobikwik://" to "MobiKwik",
            "payzapp://" to "HDFC PayZapp",
            "sbiyono://" to "SBI YONO",
            "imobile://" to "ICICI iMobile Pay",
            "axispay://" to "Axis Pay",
            "kotakmobile://" to "Kotak Mobile",
            "idbibank://" to "IDBI Bank GO",
            "bobworld://" to "BOB World",
            "indusmobile://" to "IndusMobile",
            "credpay://" to "Cred"
        )
        
        return upiSchemes.filter { (scheme, _) ->
            val nsUrl = NSURL.URLWithString(scheme)
            nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)
        }.map { (scheme, name) ->
            UpiAppInfo(
                packageName = scheme,
                displayName = name,
                icon = null
            )
        }
    }

    override fun openAppSettings() {
        val url = platform.UIKit.UIApplicationOpenSettingsURLString
        platformLog("IosPlatformActions: UIApplicationOpenSettingsURLString value is: '$url'")
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            platformLog("IosPlatformActions: Opening app settings...")
            UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = { success ->
                platformLog("IosPlatformActions: Open settings result: $success")
            })
        } else {
            platformLog("IosPlatformActions: ❌ Failed to create URL for app settings")
        }
    }

    override fun generateReferralLink(referrerId: String, onComplete: (String?) -> Unit) {
        com.pyllar.consumer.analytics.SwiftAnalyticsScope.bridge?.generateReferralLink(referrerId, onComplete)
    }

    override fun requestInAppReview(
        screenName: String,
        silentFallback: Boolean,
        trigger: String
    ) {
        platformLog("IosPlatformActions: 🚀 requestInAppReview called (silentFallback=$silentFallback)")
        com.pyllar.consumer.analytics.PlatformAnalyticsLogger.logEvent(
            "rate_us_in_app_review_started",
            mapOf("screen_name" to screenName, "silent" to silentFallback, "trigger" to trigger)
        )

        val window = UIApplication.sharedApplication.windows.first() as? UIWindow
        val scene = window?.windowScene
        if (scene != null) {
            platformLog("IosPlatformActions: ✅ Found windowScene, requesting review in scene")
            platform.StoreKit.SKStoreReviewController.requestReviewInScene(scene)
            com.pyllar.consumer.analytics.PlatformAnalyticsLogger.logEvent(
                "rate_us_in_app_review_shown",
                mapOf("screen_name" to screenName, "trigger" to trigger)
            )
        } else {
            platformLog("IosPlatformActions: ❌ requestInAppReview called but windowScene is null, falling back to App Store")
            com.pyllar.consumer.analytics.PlatformAnalyticsLogger.logEvent(
                "rate_us_in_app_review_fallback",
                mapOf("reason" to "no_scene", "screen_name" to screenName, "trigger" to trigger)
            )
            if (!silentFallback) {
                openAppStoreReview()
            }
        }
    }

    private fun openAppStoreReview() {
        val appStoreUrl = "itms-apps://itunes.apple.com/app/id6767513475?action=write-review"
        val nsUrl = NSURL.URLWithString(appStoreUrl)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = { success ->
                platformLog("IosPlatformActions: Opened App Store review result: $success")
            })
        } else {
            platformLog("IosPlatformActions: ❌ Failed to open App Store review URL")
        }
    }

    private fun getRootViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.windows.first() as? UIWindow
        return window?.rootViewController
    }
}
