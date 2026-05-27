package com.pyllar.consumer.analytics

interface IosAnalyticsBridge {
    fun logEvent(name: String, params: Map<String, Any?>)
    fun logScreenView(screenName: String)
    fun setUserId(userId: String)
}

object SwiftAnalyticsScope {
    var bridge: IosAnalyticsBridge? = null
}
