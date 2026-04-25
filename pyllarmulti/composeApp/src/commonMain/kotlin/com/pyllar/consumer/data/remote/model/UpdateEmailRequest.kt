package com.pyllar.consumer.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateEmailRequest(
    val email: String,
    val userId: String,
    val utmSource: String? = null,
    val utmMedium: String? = null,
    val utmCampaign: String? = null,
    val utmTerm: String? = null,
    val utmContent: String? = null
)

