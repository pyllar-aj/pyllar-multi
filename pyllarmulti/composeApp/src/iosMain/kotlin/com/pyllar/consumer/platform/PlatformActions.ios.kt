package com.pyllar.consumer.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIWindow

class IosPlatformActions : PlatformActions {

    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl)
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
        val baseUrl = "https://wa.me/$phoneNumber"
        val components = NSURLComponents.componentsWithString(baseUrl)
        components?.setQueryItems(listOf(NSURLQueryItem.queryItemWithName("text", message)))
        
        val url = components?.URL
        if (url != null) {
            UIApplication.sharedApplication.openURL(url)
        }
    }

    override fun getInstalledUpiApps(): List<UpiAppInfo> {
        val upiSchemes = mapOf(
            "phonepe://" to "PhonePe",
            "tez://" to "Google Pay",
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

    private fun getRootViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.windows.first() as? UIWindow
        return window?.rootViewController
    }
}
