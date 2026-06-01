package com.pyllar.consumer.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateEmailRequest(
    @SerialName("email")
    val email: String,
    @SerialName("userId")
    val userId: String,
    @SerialName("utmSource")
    val utmSource: String? = null,
    @SerialName("utmMedium")
    val utmMedium: String? = null,
    @SerialName("utmCampaign")
    val utmCampaign: String? = null,
    @SerialName("utmTerm")
    val utmTerm: String? = null,
    @SerialName("utmContent")
    val utmContent: String? = null,
    @SerialName("utmCampaignId")
    val utmCampaignId: String? = null,
    @SerialName("gclid")
    val gclid: String? = null,
    @SerialName("gbraid")
    val gbraid: String? = null,
    @SerialName("wbraid")
    val wbraid: String? = null
)


