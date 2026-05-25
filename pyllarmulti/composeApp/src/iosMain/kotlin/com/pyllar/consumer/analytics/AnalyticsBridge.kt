package com.pyllar.consumer.analytics

interface IosAnalyticsBridge {
    fun logEvent(name: String, params: Map<String, Any?>)
    fun logScreenView(screenName: String)
    fun setUserId(userId: String)
    fun generateReferralLink(referrerId: String, onComplete: (String?) -> Unit)
    fun getAttributionData(): Map<String, String?>
}

object SwiftAnalyticsScope {
    var bridge: IosAnalyticsBridge? = null
}
