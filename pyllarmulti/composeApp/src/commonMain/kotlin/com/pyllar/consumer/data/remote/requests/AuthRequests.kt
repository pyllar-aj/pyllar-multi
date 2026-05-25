package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtpRegistrationRequest(
    val phoneNumber: String,
    val name: String,
    val deviceId: String? = null,
    val deviceType: String? = null,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val pushToken: String? = null,
    val utmSource: String? = null,
    val utmMedium: String? = null,
    val utmCampaign: String? = null,
    val utmTerm: String? = null,
    val utmContent: String? = null,
    val utmCampaignId: String? = null,
    val gclid: String? = null,
    val gbraid: String? = null,
    val wbraid: String? = null,
    val afMediaSource: String? = null,
    val afCampaign: String? = null,
    val afCampaignId: String? = null,
    val afAdSet: String? = null,
    val afStatus: String? = null,
    val afChannel: String? = null
)

@Serializable
data class OtpVerificationRequest(
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("otp")
    val otp: String,
    @SerialName("id")
    val id: String? = null,
    val deviceId: String? = null,
    val deviceType: String? = null,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val pushToken: String? = null
)

