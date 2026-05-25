package com.pyllar.consumer.analytics

/**
 * Holds AppsFlyer attribution data between Application.onCreate (AF callback fires async)
 * and the first OTP send (where attribution is forwarded to the backend).
 */
object AppsFlyerAttributionCache {

    @Volatile
    private var cache: Map<String, Any?> = emptyMap()

    fun store(data: Map<String, Any?>) {
        cache = data
    }

    fun getReferralCode(): String? = cache["deep_link_sub1"]?.toString() ?: cache["af_sub1"]?.toString()
    fun getMediaSource(): String? = cache["media_source"]?.toString()
    fun getCampaign(): String? = cache["campaign"]?.toString()
    fun getCampaignId(): String? = cache["campaign_id"]?.toString()
    fun getAdSet(): String? = cache["adset"]?.toString()
    fun getAfStatus(): String? = cache["af_status"]?.toString()
    fun getChannel(): String? = cache["channel"]?.toString()
    fun getGclid(): String?  = cache["gclid"]?.toString()
    fun getGbraid(): String? = cache["gbraid"]?.toString()
    fun getWbraid(): String? = cache["wbraid"]?.toString()

    fun isEmpty(): Boolean = cache.isEmpty()
}
