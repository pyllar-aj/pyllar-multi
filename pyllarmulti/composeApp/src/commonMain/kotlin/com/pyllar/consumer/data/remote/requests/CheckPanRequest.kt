package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckPanRequest(
    @SerialName("panNumber")
    val pan: String,
    @SerialName("userId")
    val userId: String
)