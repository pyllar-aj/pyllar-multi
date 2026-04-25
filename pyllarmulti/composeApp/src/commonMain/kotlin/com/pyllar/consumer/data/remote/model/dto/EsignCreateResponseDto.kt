package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EsignCreateResponseDto(
    @SerialName("object")
    val objectType: String? = null,
    val id: String? = null,
    val type: String? = null,
    @SerialName("kyc_request")
    val kycRequest: String? = null,
    @SerialName("redirect_url")
    val redirectUrl: String? = null,
    val status: String? = null,
    @SerialName("postback_url")
    val postbackUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
