package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HelperCodeRequest(
    @SerialName("userId")
    val userId: String,
    @SerialName("helperCode")
    val helperCode: String
)
